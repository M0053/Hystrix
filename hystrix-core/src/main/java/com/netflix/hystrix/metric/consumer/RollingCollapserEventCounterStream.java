/**
 * Copyright 2015 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.metric.consumer;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.datastore.HystrixDataStoreProvider;
import com.netflix.hystrix.datastore.HystrixKeyDataStore;
import com.netflix.hystrix.metric.HystrixCollapserEvent;
import com.netflix.hystrix.metric.HystrixCollapserEventStream;
import com.netflix.hystrix.util.Lazy;
import rx.functions.Func2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Maintains a stream of event counters for a given Command.
 * There is a rolling window abstraction on this stream.
 * The event counters object is calculated over a window of t1 milliseconds.  This window has b buckets.
 * Therefore, a new set of counters is produced every t2 (=t1/b) milliseconds
 * t1 = {@link com.netflix.hystrix.HystrixCollapserProperties#metricsRollingStatisticalWindowInMilliseconds()}
 * b = {@link com.netflix.hystrix.HystrixCollapserProperties#metricsRollingStatisticalWindowBuckets()}
 *
 * These values are stable - there's no peeking into a bucket until it is emitted
 *
 * These values get produced and cached in this class.  This value (the latest observed value) may be queried using {@link #getLatest(HystrixEventType.Collapser)}.
 */
public class RollingCollapserEventCounterStream extends BucketedRollingCounterStream<HystrixCollapserEvent, long[], long[]> {

    private static final Lazy<HystrixKeyDataStore<HystrixCollapserKey, RollingCollapserEventCounterStream>> streams = HystrixDataStoreProvider.lazyInitKeyDataStore();

    private static final int NUM_EVENT_TYPES = HystrixEventType.Collapser.values().length;

    public static RollingCollapserEventCounterStream getInstance(HystrixCollapserKey collapserKey, HystrixCollapserProperties properties) {
        final int counterMetricWindow = properties.metricsRollingStatisticalWindowInMilliseconds().get();
        final int numCounterBuckets = properties.metricsRollingStatisticalWindowBuckets().get();
        final int counterBucketSizeInMs = counterMetricWindow / numCounterBuckets;

        return getInstance(collapserKey, numCounterBuckets, counterBucketSizeInMs);
    }

    public static RollingCollapserEventCounterStream getInstance(HystrixCollapserKey collapserKey, int numBuckets, int bucketSizeInMs) {
        return streams.get().getOrLoad(collapserKey, () -> new RollingCollapserEventCounterStream(collapserKey, numBuckets, bucketSizeInMs, HystrixCollapserMetrics.appendEventToBucket, HystrixCollapserMetrics.bucketAggregator));
    }

    public static void reset() {
        streams.get().clear();
    }

    private RollingCollapserEventCounterStream(HystrixCollapserKey collapserKey, int numCounterBuckets, int counterBucketSizeInMs,
                                             Func2<long[], HystrixCollapserEvent, long[]> appendEventToBucket,
                                             Func2<long[], long[], long[]> reduceBucket) {
        super(HystrixCollapserEventStream.getInstance(collapserKey), numCounterBuckets, counterBucketSizeInMs, appendEventToBucket, reduceBucket);
    }

    @Override
    long[] getEmptyBucketSummary() {
        return new long[NUM_EVENT_TYPES];
    }

    @Override
    long[] getEmptyOutputValue() {
        return new long[NUM_EVENT_TYPES];
    }

    public long getLatest(HystrixEventType.Collapser eventType) {
        return getLatest()[eventType.ordinal()];
    }
}
