package com.cauldronjs;

import java.util.concurrent.atomic.AtomicReference;

import com.cauldronjs.isolate.Isolate;
import com.cauldronjs.async.Thenable;
import com.cauldronjs.utils.JsUtils;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class AsyncFactory {
  private final Isolate isolate;

  public AsyncFactory(Isolate isolate) {
    this.isolate = isolate;
  }

  public Thenable evaluateAsync(Thenable thenable) {
    assert thenable != null;
    Value promise = JsUtils.createPromise(this.isolate);
    AtomicReference<Value> resolveRef = new AtomicReference<>();
    AtomicReference<Value> rejectRef = new AtomicReference<>();
    Value promiseResult = promise.newInstance((ProxyExecutable) args -> {
      resolveRef.set(args[0]);
      rejectRef.set(args[1]);
      thenable.then(resolveRef.get(), rejectRef.get());
      return null;
    });

    return (resolve, reject) -> promiseResult.invokeMember("then", resolveRef.get(), rejectRef.get());
  }
}