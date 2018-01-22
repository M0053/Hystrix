package com.netflix.hystrix.datastore;

import com.netflix.hystrix.HystrixKey;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * An idiomatic implementation of a HystrixKey based HystrixDataStore.
 * Basically removes the repetition of key.name() when accessing the DataStore.
 * This makes the type much more expressive of what we're keying on.
 * @see HystrixDataStore for more information.
 */
public class HystrixKeyDataStore<K extends HystrixKey, V> implements HystrixDataStore<K, V> {
    private final HystrixDataStore<String, V> delegate;

    public HystrixKeyDataStore(HystrixDataStore<String, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V getOrLoad(K key, Supplier<? extends V> initializer) {
        return delegate.getOrLoad(key.name(), initializer);
    }

    @Override
    public V getIfPresent(K key) {
        return delegate.getIfPresent(key.name());
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void remove(K key) {
        delegate.remove(key.name());
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }
}
