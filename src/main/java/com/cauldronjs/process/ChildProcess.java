package com.cauldronjs.process;

import com.cauldronjs.EventEmitter;
import com.cauldronjs.utils.ProcessHelpers;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ChildProcess extends EventEmitter {
    public static class ChildProcessResult {
        public final long pid;
        public final String[] output;
        public final String stdout;
        public final String stderr;
        public final int status;
        public final String signal;
        public final Exception error;

        private ChildProcessResult(long pid, String stdout, String stderr, int status, String signal, Exception error) {
            this.pid = pid;
            this.output = new String[] {
                    "",
                    stdout,
                    stderr
            };
            this.stdout = stdout;
            this.stderr = stderr;
            this.status = status;
            this.signal = signal;
            this.error = error;
        }
    }

    private final Process process;
    private final int uid;
    private final CountDownLatch latch;
    private boolean isSync = false;
    private long pid;
    private Thread runner;
    private String killSignal;

    protected ChildProcess(Process process, int uid) {
        this.process = process;
        this.uid = uid;
        this.latch = new CountDownLatch(1);
    }

    public static ChildProcess spawn(String command, String[] args, SpawnOptions options) throws IOException, InterruptedException {
        String[] commandWithArgs = new String[1 + args.length];
        commandWithArgs[0] = command;
        System.arraycopy(args, 0, commandWithArgs, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(commandWithArgs)
                .directory(options.getCwd())
                .redirectOutput(options.getOutputRedirect())
                .redirectInput(options.getInputRedirect())
                .redirectError(options.getErrorRedirect());
        ChildProcess process = new ChildProcess(builder.start(), options.getUid());
        process.pipeStreams();
        process.runner.start();
        process.latch.await();
        return process;
    }

    public static ChildProcessResult spawnSync(String command, String[] args, SpawnOptions options) throws IOException {
        String[] commandWithArgs = new String[1 + args.length];
        commandWithArgs[0] = command;
        System.arraycopy(args, 0, commandWithArgs, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(commandWithArgs)
                .directory(options.getCwd())
                .redirectOutput(options.getOutputRedirect())
                .redirectInput(options.getInputRedirect())
                .redirectError(options.getErrorRedirect());
        ChildProcess process = new ChildProcess(builder.start(), options.getUid());
        process.isSync = true;
        process.pipeStreams();

        AtomicReference<String> stdin = new AtomicReference<>("");
        AtomicReference<String> stderr = new AtomicReference<>("");

        process.on("stdin", (eventArgs) -> stdin.set(stdin.get() + "\n" + eventArgs[0]));
        process.on("stderr", (eventArgs) -> stderr.set(stderr.get() + "\n" + eventArgs[0]));

        process.runner.start();
        long pid = process.pid;
        int status = process.process.exitValue();
        return new ChildProcessResult(pid, stdin.get(), stderr.get(), status, process.killSignal, null);
    }

    private static boolean isEof(String input) {
        return input != null && input.trim().equals("--EOF--");
    }

    private void pipeStreams() {
        this.pid = ProcessHelpers.getPid(this.process);

        OutputStream outStream = this.getOutputStream();
        InputStream inStream = this.getInputStream();
        InputStream errorStream = this.getErrorStream();
        this.runner = new Thread(() -> {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(inStream));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
            // not used at the moment
            BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(outStream));
            // do work
            try {
                String input, error = null;
                while (ChildProcess.this.process.isAlive() &&
                        ((input = inputReader.readLine()) != null ||
                        (error = errorReader.readLine()) != null) &&
                       ( !isEof(input) ||
                        !isEof(error))) {
                    if (input != null) {
                        ChildProcess.this.emit("stdin", input);
                    }
                    if (error != null) {
                        ChildProcess.this.emit("stderr", error);
                    }
                    if (!ChildProcess.this.isSync) {
                        Thread.sleep(10);
                    }
                }
            } catch (IOException | InterruptedException exception) {
                // log the exception somewhere
            }
            this.emit("finish");
            ChildProcess.this.latch.countDown();
        });
    }

    public void kill() throws InterruptedException {
        this.process.destroyForcibly();
        this.process.waitFor();
    }

    public void kill(String signal) throws InterruptedException {
        this.killSignal = signal;
        this.process.destroyForcibly();
        this.process.waitFor();
    }

    public int getUid() {
        return this.uid;
    }

    public OutputStream getOutputStream() {
        return this.process.getOutputStream();
    }

    public InputStream getInputStream() {
        return this.process.getInputStream();
    }

    public InputStream getErrorStream() {
        return this.process.getErrorStream();
    }
}
