package com.netflix.hystrix.datastore;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class HystrixCachedDataStore<K, V> implements HystrixDataStore<K, V> {
    private Cache<K, V> storage;

    private HystrixCachedDataStore(Cache<K, V> storage) {
        this.storage = storage;
    }

    @Override
    public V getOrLoad(K key, Supplier<? extends V> initializer) {
        try {
            return storage.get(key, initializer::get);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Error initializing entry for " + key, e.getCause());
        }
    }

    @Override
    public V getIfPresent(K key) {
        return storage.getIfPresent(key);
    }

    @Override
    public int size() {
        return (int) storage.size();
    }

    @Override
    public void clear() {
        storage.invalidateAll();
    }

    @Override
    public void remove(K key) {
        storage.invalidate(key);
    }

    @Override
    public Collection<V> values() {
        return storage.asMap().values();
    }

    public static class Factory implements HystrixDataStore.Factory {
        private Optional<Long> maxSize;
        private Optional<Long> expireAfterAccess;
        private Optional<Ticker> ticker;

        private Factory(Long maxSize, Long expireAfterAccess, Ticker ticker) {
            this.maxSize = Optional.ofNullable(maxSize);
            this.expireAfterAccess = Optional.ofNullable(expireAfterAccess);
            this.ticker = Optional.ofNullable(ticker);
        }

        @Override
        public <K, V> HystrixDataStore<K, V> create() {
            final CacheBuilder<K, V> cacheBuilder = (CacheBuilder<K, V>) CacheBuilder.newBuilder();
            maxSize.ifPresent(cacheBuilder::maximumSize);
            expireAfterAccess.ifPresent(v -> cacheBuilder.expireAfterAccess(v, TimeUnit.MILLISECONDS));
            ticker.ifPresent(cacheBuilder::ticker);
            return new HystrixCachedDataStore<>(cacheBuilder.build());
        }

        public static class Builder {
            private Long maxSize;
            private Long expireAfterAccessInMs;
            private Ticker ticker;

            public Builder maxSize(final long maxSize) {
                this.maxSize = maxSize;
                return this;
            }

            public Long maxSize() {
                return this.maxSize;
            }

            public Builder expireAfterAccess(final long expireAfterAccess) {
                this.expireAfterAccessInMs = expireAfterAccess;
                return this;
            }

            public Long expireAfterAccess() {
                return this.expireAfterAccessInMs;
            }

            public Builder ticker(final Ticker ticker) {
                this.ticker = ticker;
                return this;
            }

            public Ticker ticker() {
                return this.ticker;
            }

            public Factory build() {
                return new Factory(maxSize, expireAfterAccessInMs, ticker);
            }
        }
    }
}
