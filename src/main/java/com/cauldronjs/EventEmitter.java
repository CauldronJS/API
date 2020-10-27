package com.cauldronjs;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.HashMap;

public class EventEmitter {
    private static class HandlerFrame {
        private final RunnableWithArg runnable;
        private final boolean shouldDeleteAfterRun;

        public HandlerFrame(RunnableWithArg runnable, boolean shouldDeleteAfterRun) {
            this.runnable = runnable;
            this.shouldDeleteAfterRun = shouldDeleteAfterRun;
        }

        public RunnableWithArg getRunnable() {
            return runnable;
        }

        public boolean shouldDeleteAfterRun() {
            return shouldDeleteAfterRun;
        }
    }

    private final HashMap<String, ArrayList<HandlerFrame>> handlers;
    private int maxListeners = 256;

    public EventEmitter() {
        this.handlers = new HashMap<>();
    }

    public void on(String event, Value handler) {
        this.on(event, handler::executeVoid);
    }

    public void on(String event, RunnableWithArg handler) {
        HandlerFrame frame = new HandlerFrame(handler, false);
        if (!this.handlers.containsKey(event)) {
            this.handlers.put(event, new ArrayList<>());
        }
        this.handlers.get(event).add(frame);
    }

    public void prependListener(String event, Value handler) {
        this.prependListener(event, handler::executeVoid);
    }

    public void prependListener(String event, RunnableWithArg handler) {
        HandlerFrame frame = new HandlerFrame(handler, false);
        if (!this.handlers.containsKey(event)) {
            this.handlers.put(event, new ArrayList<>());
        }
        this.handlers.get(event).add(0, frame);
    }

    public void addListener(String event, RunnableWithArg handler) {
        this.on(event, handler);
    }

    public void addListener(String event, Value handler) {
        this.on(event, handler);
    }

    public void once(String event, Value handler) {
        this.once(event, handler::executeVoid);
    }

    public void once(String event, RunnableWithArg handler) {
        HandlerFrame frame = new HandlerFrame(handler, true);
        if (!this.handlers.containsKey(event)) {
            this.handlers.put(event, new ArrayList<>());
        }
        this.handlers.get(event).add(frame);
    }

    public void emit(String event, Object... args) {
        ArrayList<HandlerFrame> frames = this.handlers.getOrDefault(event, null);
        if (frames != null) {
            for (HandlerFrame frame: frames) {
                try {
                    frame.getRunnable().run(args);
                    if (frame.shouldDeleteAfterRun()) {
                        frames.remove(frame);
                    }
                } catch (Exception ex) {
                    // log somewhere?
                }
            }
        }
    }

    public void removeAllListeners() {
        this.handlers.clear();
    }

    public void removeAllListeners(String event) {
        this.handlers.remove(event);
    }

    public String[] eventNames() {
        return this.handlers.keySet().toArray(new String[0]);
    }

    public int getMaxListeners() {
        return this.maxListeners;
    }

    public void setMaxListeners(int maxListeners) {
        this.maxListeners = maxListeners;
    }
}