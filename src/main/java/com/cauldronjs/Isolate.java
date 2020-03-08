package com.cauldronjs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import de.mxro.process.Spawn;

import com.cauldronjs.exceptions.JsException;
import com.cauldronjs.utils.Console;
import com.cauldronjs.utils.FileReader;
import com.cauldronjs.utils.PathHelpers;

public class Isolate {
  private static final String CAULDRON_SYMBOL = "$$cauldron$$";

  private static final String ENGINE_ENTRY = "lib/internal/bootstrap/loader.js";

  private static final int POLLING_TIME = 2;
  private static final int POLLING_TIME_EMPTY = 10;
  private static final int POLLING_DURATION = 5;

  private static Isolate activeIsolate;

  private CauldronAPI cauldron;
  private Context context;
  private FileReader fileReader;
  private PathHelpers pathHelpers;
  private boolean initialized = false;

  private boolean isEngaged = false;
  private SynchronousQueue<Value> asyncQueue = new SynchronousQueue<>(true);
  private int asyncProcessId;
  private String cwd;

  /**
   * Represents an instance of the VM that runs scripts. Objects located in one
   * isolate are not to be used in another isolate.
   * 
   * @param cauldronInstance
   */
  public Isolate(CauldronAPI cauldron) {
    this.cauldron = cauldron;
    this.context = this.buildContext();
    this.fileReader = new FileReader(cauldron);
    this.pathHelpers = new PathHelpers(this);
    this.cwd = Optional.ofNullable(System.getenv("CAULDRON_CWD"))
        .orElse(this.cauldron.getDefaultCwd().getAbsolutePath());
  }

  private Runnable getAsyncRunnable() {
    return new Runnable() {

      @Override
      public void run() {
        int processed = 0;
        Isolate isolate = Isolate.activeIsolate;
        while ((processed++) < 10 && !isolate.asyncQueue.isEmpty() && isolate.isEngaged) {
          try {
            Value nextInQueue = isolate.asyncQueue.take();
            nextInQueue.executeVoid();
          } catch (InterruptedException ex) {
            Console.debug(Isolate.this.cauldron, "Failed to finish async queue due to interruption. Details below:",
                ex);
            break;
          }
        }
      }
    };
  }

  private Context buildContext() {
    return Context.newBuilder("js").option("js.ecmascript-version", "10").allowAllAccess(true)
        .allowHostAccess(HostAccess.ALL).allowHostClassLoading(true).allowHostClassLookup(s -> true).build();
  }

  private void createBindings() {
    // polyfill globalThis if the current version doesn't have it
    this.put("globalThis", this.context.getPolyglotBindings());
    this.put("process", false);
    this.put(CAULDRON_SYMBOL, this.cauldron);

    this.bind("FileReader", this.fileReader);
    this.bind("PathHelpers", this.pathHelpers);
  }

  private boolean activate() throws IOException {
    this.context.enter();
    if (!this.initialized) {
      this.pathHelpers.tryInitializeCwd(this.cauldron);
      this.createBindings();
      try {
        this.runScript(this.fileReader.read(ENGINE_ENTRY), ENGINE_ENTRY);
        this.initialized = true;
      } catch (FileNotFoundException ex) {
        Console.error(this.cauldron, "Failed to find Cauldron entry point", ex);
        return false;
      } catch (IOException ex) {
        Console.error(this.cauldron, "An error occured while reading entry point", ex);
        return false;
      } catch (JsException ex) {
        Console.error(this.cauldron, "An error occured while reading entry point", ex, ex.getStackTrace());
      }
    }
    activeIsolate = this;
    // refresh the registered isolate
    this.put("isolate", this);
    this.asyncProcessId = this.cauldron.scheduleRepeatingTask(this.getAsyncRunnable(),
        this.asyncQueue.isEmpty() ? POLLING_TIME_EMPTY : POLLING_TIME, POLLING_DURATION);
    return true;
  }

  private void pause() {
    this.cauldron.cancelTask(this.asyncProcessId);
    try {
      this.context.leave();
    } catch (Exception ex) {
      // ignore
    }
  }

  /**
   * Scopes this Isolate to the current isolate;
   */
  public boolean scope() throws IOException {
    if (activeIsolate != null) {
      // deactivate (not dispose) current isolate
      activeIsolate.pause();
    }
    return this.activate();
  }

  /**
   * Disposes of the isolate, destroying any resources and freeing them
   */
  public void dispose() {
    this.pause();
    try {
      this.context.close(true);
    } catch (Exception ex) {
      // ignore
    }
  }

  /**
   * Returns the current isolate
   * 
   * @return
   */
  public static Isolate activeIsolate() {
    return activeIsolate;
  }

  /**
   * Returns the Cauldron instance this Isolate belongs to
   * 
   * @return
   */
  public CauldronAPI cauldron() {
    return this.cauldron;
  }

  /**
   * Runs the script against this isolate
   * 
   * @param script
   * @param location
   * @return
   */
  public Value runScript(String script, String location) throws JsException {
    this.isEngaged = true;
    try {
      Source source = Source.newBuilder("js", script, location).build();
      this.isEngaged = false;
      return this.context.eval(source);
    } catch (IOException ex) {
      this.isEngaged = false;
      Console.error(this.cauldron, "IO exception during eval: " + ex.toString());
      return null;
    } catch (Exception ex) {
      Console.error(this.cauldron, "Generic exception during eval: " + ex.toString());
      throw new JsException(this.cauldron, ex);
    }
  }

  public Value runScript(String location) throws FileNotFoundException, IOException, JsException {
    String content = this.fileReader.read(location);
    return this.runScript(content, location);
  }

  private void put(String identifier, Object object) {
    this.context.getPolyglotBindings().putMember(identifier, object);
  }

  public void bind(String identifier, Object object) {
    this.context.getPolyglotBindings().putMember(CAULDRON_SYMBOL + '.' + identifier, object);
  }

  public String spawn(String command, String folder) {
    return Spawn.sh(this.pathHelpers.resolveLocalFile(folder), command);
  }

  /**
   * Creates a promise that executes asynchronously, returning a Thenable
   * 
   * @param promiseArgsFn
   * @return
   */
  public Value generateAsyncPromise(Value promiseBody) {
    // a JS function decorated with 'async' doesn't mean it'll run async. It just
    // means
    // that we can use the 'await' operator in it. Graal will allow us to use
    // 'await'
    // on a Thenable, so this function's job is to run the Promise's body async

    Value promise = this.context.getBindings("js").getMember("Promise");
    if (!promiseBody.canExecute()) {
      return promise.invokeMember("resolve", promiseBody);
    } else {
      return promise.newInstance((ProxyExecutable) args -> {
        Value resolve = args[0];
        Value reject = args[1];
        this.cauldron.scheduleTask(() -> {
          try {
            Value result = promiseBody.execute();
            resolve.execute(result);
          } catch (Exception ex) {
            reject.execute(ex);
          }
        }, 0);

        return null;
      });
    }
  }

  /**
   * Gets the context of this isolate
   * 
   * @return
   */
  public Context getContext() {
    return this.context;
  }

  public File cwd() {
    return new File(this.cwd);
  }
}