package com.netflix.hystrix.datastore;

import java.lang.ref.Reference;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Default DataStore implementation backed by a ConcurrentHashMap.
 */
public class HystrixDefaultDataStore<K, V> implements HystrixDataStore<K, V> {
    /**
     * We want the stronger guarantees of ConcurrentHashMap's computeIfAbsent instead of
     * the weaker ones from the ConcurrentMap interface.
     */
    private final ConcurrentHashMap<K, V> storage;

    private HystrixDefaultDataStore(ConcurrentHashMap<K, V> storage) {
        this.storage = storage;
    }

    /**
     * returns the existing value for the key or gets the value from the initializer to insert and return
     */
    @Override
    public V getOrLoad(K key, Supplier<? extends V> initializer) {
        return storage.computeIfAbsent(key, k -> initializer.get());
    }

    @Override
    public V getIfPresent(K key) {
        return storage.get(key);
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public void remove(K key) {
        storage.remove(key);
    }

    @Override
    public Collection<V> values() {
        return storage.values();
    }

    public static class Factory implements HystrixDataStore.Factory {

        @Override
        public <K, V> HystrixDataStore<K, V> create() {
            return new HystrixDefaultDataStore<>(new ConcurrentHashMap<>());
        }
    }
}
