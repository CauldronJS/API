package com.cauldronjs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.management.ExecutionListener;

import de.mxro.process.Spawn;

import com.cauldronjs.core.AsyncFactory;
import com.cauldronjs.core.JsRunnable;
import com.cauldronjs.core.net.NetServer;
import com.cauldronjs.exceptions.JsException;
import com.cauldronjs.utils.Console;
import com.cauldronjs.utils.FileReader;
import com.cauldronjs.utils.PathHelpers;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;

public class Isolate {
  private static final String CAULDRON_SYMBOL = "$$cauldron$$";

  private static final String ENGINE_ENTRY = "lib/internal/bootstrap/loader.js";

  private static Isolate activeIsolate;

  private CauldronAPI cauldron;
  private Context context;
  private FileReader fileReader;
  private PathHelpers pathHelpers;
  private AsyncFactory asyncFactory;
  private boolean initialized = false;
  private String cwd;

  private ArrayList<Value> onCloseHandlers;

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
    this.asyncFactory = new AsyncFactory(this);
    this.cwd = Optional.ofNullable(System.getenv("CAULDRON_CWD"))
        .orElse(this.cauldron.getDefaultCwd().getAbsolutePath());
    this.onCloseHandlers = new ArrayList<>();
  }

  private Thread createContextThread() {
    Thread thread = new Thread(null, null, "CauldronThread");
    return thread;
  }

  private Context buildContext() {
    return Context.newBuilder("js").option("js.ecmascript-version", "11").allowAllAccess(true)
        .allowHostAccess(HostAccess.ALL).allowHostClassLoading(true).allowHostClassLookup(s -> true).build();
  }

  private void createBindings() {
    // polyfill globalThis if the current version doesn't have it
    this.put("globalThis", this.context.getPolyglotBindings());
    this.put("process", false);
    this.put(CAULDRON_SYMBOL, this.cauldron);

    this.bind("FileReader", this.fileReader);
    this.bind("PathHelpers", this.pathHelpers);

    // bind Cauldron classes/modules
    this.bind("NetServer", new NetServer(this));
    this.bind("Runnable", new JsRunnable());
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
    return true;
  }

  private void pause() {
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
      this.onCloseHandlers.forEach((value) -> {
        if (value.canExecute()) {
          value.execute();
        }
      });
      this.context.close(true);
    } catch (Exception ex) {
      // ignore
    }
  }

  public void runEntry() {
    assert this.initialized;
    assert this.context.getBindings("js").hasMember("NativeModule");
    this.context.getBindings("js").getMember("NativeModule").invokeMember("$$bootstrap");
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
    try {
      Source source = Source.newBuilder("js", script, location).build();
      return this.context.eval(source);
    } catch (IOException ex) {
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

  public Value eval(String content) throws JsException {
    try {
      return this.context.eval("js", content);
    } catch (Exception ex) {
      throw new JsException(this.cauldron, ex);
    }
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

  public void onClose(Value handler) {
    this.onCloseHandlers.add(handler);
  }

  /**
   * Gets the context of this isolate
   * 
   * @return
   */
  public Context getContext() {
    return this.context;
  }

  public AsyncFactory getAsyncFactory() {
    return this.asyncFactory;
  }

  public File cwd() {
    return new File(this.cwd);
  }
}