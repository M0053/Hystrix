package com.netflix.hystrix.datastore;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HystrixCachedDataStore<K, V> extends HystrixDataStore<K, V> {
    private Cache<K, V> storage;

    private HystrixCachedDataStore(Cache<K, V> storage) {
        this.storage = storage;
    }

    @Override
    public V getOrLoad(K key, Callable<V> initializer) {
        try {
            return storage.get(key, initializer);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error initializing entry for " + key, e.getCause());
        }
    }

    @Override
    public int size() {
        return (int) storage.size();
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
        public <K, V> HystrixDataStore<K, V> create(Class<K> keyClass, Class<V> valueClass) {
            final CacheBuilder<K, V> cacheBuilder = (CacheBuilder<K, V>) CacheBuilder.newBuilder();
            maxSize.ifPresent(cacheBuilder::maximumSize);
            expireAfterAccess.ifPresent(v -> cacheBuilder.expireAfterAccess(v, TimeUnit.MILLISECONDS));
            ticker.ifPresent(cacheBuilder::ticker);
            return new HystrixCachedDataStore<K, V>(cacheBuilder.build());
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            Long maxSize;
            Long expireAfterAccess;
            Ticker ticker;

            public void maxSize(final long maxSize) {
                this.maxSize = maxSize;
            }

            public Long maxSize() {
                return this.maxSize;
            }

            public void expireAfterAccess(final long expireAfterAccess) {
                this.expireAfterAccess = expireAfterAccess;
            }

            public Long expireAfterAccess() {
                return this.expireAfterAccess;
            }

            public void ticker(final Ticker ticker) {
                this.ticker = ticker;
            }

            public Ticker ticker() {
                return this.ticker;
            }

            public Factory build() {
                return new Factory(maxSize, expireAfterAccess, ticker);
            }
        }
    }
}
