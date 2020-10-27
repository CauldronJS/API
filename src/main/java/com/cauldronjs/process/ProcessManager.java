package com.cauldronjs.process;

import com.cauldronjs.isolate.Isolate;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.HashMap;

public class ProcessManager {
    private final Isolate isolate;
    private final transient HashMap<Integer, ChildProcess> processes;
    private transient int lastId = 1;

    public ProcessManager(Isolate isolate) {
        this.isolate = isolate;
        this.processes = new HashMap<>();
    }

    public ChildProcess spawn(String command, String[] args, Value options) throws IOException, InterruptedException {
        return this.spawn(command, args, new SpawnOptions().fromValue(options));
    }

    public ChildProcess spawn(String command, String[] args, SpawnOptions options) throws IOException, InterruptedException {
        ChildProcess process = ChildProcess.spawn(command, args, options);
        if (options.getUid() == 0) {
            // it starts at 0 and 0 will be seen as a non-set ID
            options.setUid(this.getNewId());
        }
        process.on("finish", (eventArgs) -> this.processes.remove(options.getUid()));
        this.processes.put(process.getUid(), process);
        return process;
    }

    public ChildProcess.ChildProcessResult spawnSync(String command, String[] args, Value options) throws IOException, InterruptedException {
        return this.spawnSync(command, args, new SpawnOptions().fromValue(options));
    }

    public ChildProcess.ChildProcessResult spawnSync(String command, String[] args, SpawnOptions options) throws IOException, InterruptedException {
        // don't worry about adding it to the processes list, it's blocking
        return ChildProcess.spawnSync(command, args, options);
    }

    public int getProccesCount() {
        return this.processes.size();
    }

    private int getNewId() {
        if (this.lastId == Integer.MAX_VALUE) {
            this.lastId = 0;
            return this.lastId;
        } else {
            return this.lastId++;
        }
    }
}
