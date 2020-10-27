package com.cauldronjs;

@FunctionalInterface
public interface RunnableWithArg {
    void run(Object... args);
}
