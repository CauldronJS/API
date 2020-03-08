package com.cauldronjs.core;

import com.cauldronjs.Isolate;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class AsyncFactory {
  private Isolate isolate;

  public AsyncFactory(Isolate isolate) {
    this.isolate = isolate;
  }

  public Value generateAsyncPromise(Value fn) {
    Value promise = this.isolate.getContext().getBindings("js").getMember("Promise");
    if (!fn.canExecute()) {
      return promise.invokeMember("resolve", fn);
    } else {
      return promise.newInstance((ProxyExecutable) args -> {
        Value resolve = args[0];
        Value reject = args[1];
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
    Value promise = this.isolate.getContext().getBindings("js").getMember("Promise");
    return promise.newInstance((ProxyExecutable) args -> {
      Value resolve = args[0];
      this.isolate.cauldron().scheduleTask(() -> {
        resolve.execute(true);
      }, ms);
      return null;
    });
  }
}