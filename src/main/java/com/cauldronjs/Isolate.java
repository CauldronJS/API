package com.cauldronjs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import de.mxro.process.Spawn;
import com.cauldronjs.exceptions.JsException;
import com.cauldronjs.utils.Console;
import com.cauldronjs.utils.FileReader;
import com.cauldronjs.utils.PathHelpers;

public class Isolate {
  private static final String CAULDRON_SYMBOL = "$$cauldron$$";
  private static final String ISOLATE_SYMBOL = CAULDRON_SYMBOL + ".isolate";

  private static final String ENGINE_ENTRY = "lib/internal/bootstrap/loader.js";

  private static final int POLLING_TIME = 2;
  private static final int POLLING_TIME_EMPTY = 10;
  private static final int POLLING_DURATION = 5;

  private static Isolate activeIsolate;

  private CauldronAPI cauldron;
  private Context context;
  private boolean initialized = false;

  private boolean isEngaged = false;
  private SynchronousQueue<Value> asyncQueue = new SynchronousQueue<>(true);
  private int asyncProcessId;

  /**
   * Represents an instance of the VM that runs scripts. Objects located in one
   * isolate are not to be used in another isolate.
   * 
   * @param cauldronInstance
   */
  public Isolate(CauldronAPI cauldron) {
    this.cauldron = cauldron;
    this.context = Context.newBuilder("js").option("js.ecmascript-version", "10").allowAllAccess(true)
        .allowHostAccess(HostAccess.ALL).allowHostClassLoading(true).allowHostClassLookup(s -> true).build();
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
            Console.debug("Failed to finish async queue due to interruption. Details below:", ex);
            break;
          }
        }
      }
    };
  }

  private void createBindings() {
    // polyfill globalThis if the current version doesn't have it
    this.put("globalThis", this.context.getPolyglotBindings());
    this.put("process", false);
    this.put(CAULDRON_SYMBOL, this.cauldron);
  }

  private boolean activate() {
    this.context.enter();
    if (!this.initialized) {
      this.createBindings();
      try {
        this.runScript(FileReader.read(this.cauldron, ENGINE_ENTRY), ENGINE_ENTRY);
        this.initialized = true;
      } catch (FileNotFoundException ex) {
        Console.error("Failed to find Cauldron entry point", ex);
        return false;
      } catch (IOException ex) {
        Console.error("An error occured while reading entry point", ex);
        return false;
      } catch (JsException ex) {
        Console.error("An error occured while reading entry point", ex, ex.getStackTrace());
      }
    }
    activeIsolate = this;
    // refresh the registered isolate
    this.put(ISOLATE_SYMBOL, this);
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
  public boolean scope() {
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
      Source source = Source.newBuilder("js", script, location).mimeType("application/javascript").build();
      this.isEngaged = false;
      return this.context.eval(source);
    } catch (IOException ex) {
      this.isEngaged = false;
      Console.error(ex);
      return null;
    } catch (Exception ex) {
      cauldron.log(Level.INFO, ex.toString());
      cauldron.log(Level.INFO, Arrays.toString(ex.getStackTrace()));
      throw new JsException(ex);
    }
  }

  public Value runScript(String location) throws FileNotFoundException, IOException, JsException {
    String content = FileReader.read(this.cauldron, location);
    return this.runScript(content, location);
  }

  public void put(String identifier, Object object) {
    this.context.getPolyglotBindings().putMember(identifier, object);
  }

  public void queueFn(Value fn) {
    if (fn.canExecute()) {
      this.asyncQueue.add(fn);
    }
  }

  public String spawn(String command, String folder) {
    return Spawn.sh(PathHelpers.resolveLocalFile(folder), command);
  }

  /**
   * Gets the context of this isolate
   * 
   * @return
   */
  public Context getContext() {
    return this.context;
  }
}