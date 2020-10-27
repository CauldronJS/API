package com.cauldronjs.bindings;

import com.cauldronjs.isolate.Isolate;

import java.util.HashMap;

public class BindingProvider {
    private static final HashMap<Integer, BindingProvider> registeredProviders = new HashMap<>();

    private final Isolate isolate;
    private final HashMap<String, Object> bindings = new HashMap<>();

    private BindingProvider(Isolate isolate) {
        this.isolate = isolate;
    }

    public static BindingProvider getInstanceFor(Isolate isolate) {
        if (registeredProviders.containsKey(isolate.hashCode())) {
            return registeredProviders.get(isolate.hashCode());
        }
        BindingProvider instance = new BindingProvider(isolate);
        registeredProviders.put(instance.hashCode(), instance);
        return instance;
    }

    public BindingProvider register(Object binding) {
        String name = binding.getClass().getSimpleName();
        return this.register(name, binding);
    }

    public BindingProvider register(String name, Object binding) {
        this.bindings.put(name, binding);
        this.isolate.getContext().getPolyglotBindings().putMember(name, binding);
        return this;
    }

    public BindingProvider registerGlobal(String name, Object binding) {
        this.isolate.getContext().getBindings("js").putMember(name, binding);
        return this;
    }

    public Object getBinding(String name) {
        return this.bindings.getOrDefault(name, null);
    }
}
