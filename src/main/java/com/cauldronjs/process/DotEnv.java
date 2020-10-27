package com.cauldronjs.process;

import com.cauldronjs.isolate.Isolate;
import com.cauldronjs.utils.FileReader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DotEnv {
    private final Isolate isolate;
    private final FileReader fileReader;
    private Map<String, String> envVariables;

    public DotEnv(Isolate isolate, FileReader fileReader) {
        this.isolate = isolate;
        this.fileReader = fileReader;
    }

    public void configure() {
        this.envVariables = System.getenv();
        File dotenv = new File(this.isolate.cwd(), ".env");
        if (!dotenv.exists()) return;
        try {
            String[] content = this.fileReader.read(dotenv.getAbsolutePath()).split("\n");
            for (String line: content) {
                String[] kvp = line.split("=", 1);
                if (kvp.length != 2) {
                    this.isolate.cauldron().log(Level.WARNING, "Invalid key found in .env, ignoring");
                    continue;
                }
                this.envVariables.put(kvp[0], kvp[1]);
            }
        } catch (IOException ex) {
            // ignore and log
            this.isolate.cauldron().log(Level.WARNING, "Failed to read .env file: " + ex.getMessage());
        }
    }

    public Map<String, String> getEnvVariables() {
        return this.envVariables;
    }
}
