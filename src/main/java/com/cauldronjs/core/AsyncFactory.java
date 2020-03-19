package com.cauldronjs.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.cauldronjs.Isolate;
import com.cauldronjs.api.Thenable;
import com.cauldronjs.utils.JsUtils;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class AsyncFactory {
  private Isolate isolate;
  private ExecutorService threadExecutor;

  // for now, each Isolate only gets 8 separate threads
  private static final int ASYNC_THREAD_COUNT = 8;

  public AsyncFactory(Isolate isolate) {
    this.isolate = isolate;
    this.threadExecutor = Executors.newFixedThreadPool(ASYNC_THREAD_COUNT);
  }

  public synchronized Thenable evaluateAsync(Thenable thenable) {
    assert thenable != null;
    Value promise = JsUtils.createPromise(this.isolate);
    AtomicReference<Value> resolveRef = new AtomicReference<>();
    AtomicReference<Value> rejectRef = new AtomicReference<>();
    Value promiseResult = promise.newInstance((ProxyExecutable) args -> {
      resolveRef.set(args[0]);
      rejectRef.set(args[1]);

      this.threadExecutor.execute(() -> {
        thenable.then(resolveRef.get(), rejectRef.get());
      });
      return null;
    });

    return (resolve, reject) -> {
      promiseResult.invokeMember("then", resolveRef.get(), rejectRef.get());
    };
  }

  public Value generateAsyncPromise(Value fn) {
    Value promise = JsUtils.createPromise(this.isolate);
    if (!fn.canExecute()) {
      return promise.invokeMember("resolve", fn);
    } else {
      return promise.newInstance((ProxyExecutable) args -> {
        Value resolve = args[0];
        Value reject = args[1];

        assert resolve != null;
        assert reject != null;

        this.isolate.cauldron().scheduleTask(() -> {
          try {
            Value result = fn.execute();
            resolve.execute(result);
          } catch (Exception ex) {
            reject.execute(ex);
          }
        }, 0);

        return null;
      });
    }
  }

  public Value wait(int ms) {
    Value promise = JsUtils.createPromise(this.isolate);
    return promise.newInstance((ProxyExecutable) args -> {
      Value resolve = args[0];
      this.isolate.cauldron().scheduleTask(() -> {
        resolve.execute(true);
      }, ms);
      return null;
    });
  }
}