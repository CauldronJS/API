package com.cauldronjs.isolate;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cauldronjs.*;
import com.cauldronjs.bindings.BindingProvider;
import com.cauldronjs.bindings.Crypto;
import com.cauldronjs.process.DotEnv;
import com.cauldronjs.process.ProcessManager;
import com.cauldronjs.sourceMap.SourceMapParser;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import com.cauldronjs.exceptions.JSException;
import com.cauldronjs.utils.Console;
import com.cauldronjs.utils.FileReader;
import com.cauldronjs.utils.PathHelpers;

public class Isolate extends EventEmitter {
  private final CauldronAPI cauldron;
  private final Context context;
  private final FileReader fileReader;
  private final PathHelpers pathHelpers;
  private final ProcessManager processManager;
  private final UUID uuid;
  private final DotEnv dotEnv;
  private final SourceMapParser sourceMapParser;
  private final ScriptRunner scriptRunner;
  private final BindingProvider bindingProvider;
  private boolean initialized = false;
  private final String cwd;

  /**
   * Represents an instance of the VM that runs scripts. Objects located in one
   * isolate are not to be used in another isolate.
   */
  public Isolate(CauldronAPI cauldron) {
    this.uuid = UUID.randomUUID();
    this.cauldron = cauldron;
    this.context = this.buildContext();
    this.fileReader = new FileReader(this);
    this.pathHelpers = new PathHelpers(this);
    this.processManager = new ProcessManager(this);
    this.cwd = Optional.ofNullable(System.getenv("CAULDRON_CWD"))
        .orElse(this.cauldron.getDefaultCwd().getAbsolutePath());
    this.dotEnv = new DotEnv(this, this.fileReader);
    this.sourceMapParser = new SourceMapParser(this.context);
    this.scriptRunner = new ScriptRunner(this, this.fileReader);
    this.bindingProvider = BindingProvider.getInstanceFor(this);
  }

  private Context buildContext() {
    ClassLoader mainClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(CauldronAPI.class.getClassLoader());
    Context context = Context.newBuilder("js").option("js.ecmascript-version", "11").allowAllAccess(true)
        .allowHostAccess(HostAccess.ALL).allowHostClassLoading(true).allowHostClassLookup(s -> true).build();
    Thread.currentThread().setContextClassLoader(mainClassLoader);
    return context;
  }

  private void createBindings() {
    this.bindingProvider
            .register(this.fileReader)
            .register(this.pathHelpers)
            .register(this.processManager)
            .register(this.sourceMapParser)
            .register(this.scriptRunner)
            .register(new Crypto())
            .register("Runnable", new JSRunnable())
            .registerGlobal("process", false)
            .registerGlobal("$$cauldron$$", this.cauldron)
            .registerGlobal("$$isolate$$", this);
  }

  public boolean activate() throws IOException {
    this.context.enter();
    if (!this.initialized) {
      this.dotEnv.configure();
      this.pathHelpers.tryInitializeCwd(this.cauldron);
      this.createBindings();
      try {
        this.scriptRunner.runEntry();
        this.initialized = true;
      } catch (JSException ex) {
        Console.error(this.cauldron, "An error occurred while reading entry point", ex, ex.getStackTrace());
      }
    }
    this.cauldron.scheduleRepeatingTask(() -> this.emit("tick"), 50, 50);
    return true;
  }

  public void start() {
    try {
      this.scriptRunner.runMain();
    } catch (JSException ex) {
      Console.error(this.cauldron, "An error occurred while starting", ex, ex.getStackTrace());
    }
  }

  public void pause() {
    try {
      this.context.leave();
    } catch (Exception ex) {
      // ignore
    }
  }

  /**
   * Disposes of the isolate, destroying any resources and freeing them
   */
  public void dispose() {
    this.pause();
    this.emit("shutdown");
    this.context.close(true);
  }
  /**
   * Returns the Cauldron instance this Isolate belongs to
   */
  public CauldronAPI cauldron() {
    return this.cauldron;
  }

  /**
   * Gets the unique identifier of the instance
   */
  public UUID uuid() {
    return this.uuid;
  }

  public Map<String, String> getEnvVars() {
    return this.dotEnv.getEnvVariables();
  }

  /**
   * Gets the context of this isolate
   */
  public Context getContext() {
    return this.context;
  }

  public BindingProvider getBindingProvider() {
    return this.bindingProvider;
  }

  public File cwd() {
    return new File(this.cwd);
  }

  @Override
  public int hashCode() {
    return this.uuid.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other.hashCode() == this.hashCode();
  }
}