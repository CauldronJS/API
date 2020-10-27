package com.cauldronjs.isolate;

import com.cauldronjs.exceptions.JSException;
import com.cauldronjs.utils.FileReader;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;

public class ScriptRunner {
    private static final String ENGINE_ENTRY = "lib/internal/bootstrap/loader.js";

    private final Isolate isolate;
    private final Context context;
    private final FileReader fileReader;
    private final ScriptEngine validationEngine;

    public ScriptRunner(Isolate isolate, FileReader fileReader) {
        this.isolate = isolate;
        this.context = isolate.getContext();
        this.fileReader = fileReader;
        this.validationEngine = new ScriptEngineManager().getEngineByName("graal-js");
    }

    public void runEntry() throws JSException {
        try {
            this.runScript(ENGINE_ENTRY);
        } catch (Exception ex) {
            throw new JSException(this.isolate.cauldron(), ex);
        }
    }

    public void runMain() throws JSException {
        try {
            this.context.getBindings("js").getMember("NativeModule").invokeMember("$$bootstrap");
        } catch (Exception ex) {
            throw new JSException(this.isolate.cauldron(), ex);
        }
    }

    public Value runScript(CharSequence content, String file) throws JSException {
        try {
            // TODO: add safety checks here for access, we can't expect every script to be trusted
            Source source = Source.newBuilder("js", content, file).build();
            return this.context.eval(source);
        } catch (Exception ex) {
            throw prunedJSException(ex);
        }
    }

    public Value runScript(String file) throws JSException {
        try {
            return this.runScript(this.fileReader.read(file), file);
        } catch (IOException ex) {
            throw prunedJSException(ex);
        }
    }

    public Value eval(CharSequence content) throws JSException {
        return this.runScript(content, "repl");
    }

    public boolean isValid(CharSequence content) throws JSException {
        try {
            CompiledScript result = ((Compilable) this.validationEngine).compile(content.toString());
            return result != null;
        } catch (Exception ex) {
            throw prunedJSException(ex);
        }
    }

    private JSException prunedJSException(Exception original) {
        JSException pruned = new JSException(this.isolate.cauldron(), original);
        // TODO: clean up the stack trace and source map
        return pruned;
    }
}
