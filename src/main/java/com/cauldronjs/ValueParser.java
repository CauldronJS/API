package com.cauldronjs;

import org.graalvm.polyglot.Value;

public interface ValueParser<T> {
    Value toValue();
    T fromValue(Value value);
}
