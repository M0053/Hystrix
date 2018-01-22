/**
 * Copyright 2012 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix;

import com.netflix.hystrix.datastore.HystrixDataStore;
import com.netflix.hystrix.datastore.HystrixDataStoreProvider;
import com.netflix.hystrix.datastore.HystrixKeyDataStore;
import com.netflix.hystrix.metric.HystrixCollapserEvent;
import com.netflix.hystrix.metric.HystrixThreadEventStream;
import com.netflix.hystrix.metric.consumer.CumulativeCollapserEventCounterStream;
import com.netflix.hystrix.metric.consumer.RollingCollapserBatchSizeDistributionStream;
import com.netflix.hystrix.metric.consumer.RollingCollapserEventCounterStream;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import com.netflix.hystrix.util.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func2;

import java.util.Collection;
import java.util.Collections;

/**
 * Used by {@link HystrixCollapser} to record metrics.
 * {@link HystrixEventNotifier} not hooked up yet.  It may be in the future.
 */
public class HystrixCollapserMetrics extends HystrixMetrics {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(HystrixCollapserMetrics.class);

    // String is HystrixCollapserKey.name() (we can't use HystrixCollapserKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static final Lazy<HystrixKeyDataStore<HystrixCollapserKey, HystrixCollapserMetrics>> metrics = HystrixDataStoreProvider.lazyInitKeyDataStore();

    /**
     * Get or create the {@link HystrixCollapserMetrics} instance for a given {@link HystrixCollapserKey}.
     * <p>
     * This is thread-safe and ensures only 1 {@link HystrixCollapserMetrics} per {@link HystrixCollapserKey}.
     * 
     * @param key
     *            {@link HystrixCollapserKey} of {@link HystrixCollapser} instance requesting the {@link HystrixCollapserMetrics}
     * @return {@link HystrixCollapserMetrics}
     */
    public static HystrixCollapserMetrics getInstance(HystrixCollapserKey key, HystrixCollapserProperties properties) {
        return metrics.get().getOrLoad(key, () -> new HystrixCollapserMetrics(key, properties));
    }

    /**
     * All registered instances of {@link HystrixCollapserMetrics}
     * 
     * @return {@code Collection<HystrixCollapserMetrics>}
     */
    public static Collection<HystrixCollapserMetrics> getInstances() {
        return Collections.unmodifiableCollection(metrics.get().values());
    }

    private static final HystrixEventType.Collapser[] ALL_EVENT_TYPES = HystrixEventType.Collapser.values();

    public static final Func2<long[], HystrixCollapserEvent, long[]> appendEventToBucket = new Func2<long[], HystrixCollapserEvent, long[]>() {
        @Override
        public long[] call(long[] initialCountArray, HystrixCollapserEvent collapserEvent) {
            HystrixEventType.Collapser eventType = collapserEvent.getEventType();
            int count = collapserEvent.getCount();
            initialCountArray[eventType.ordinal()] += count;
            return initialCountArray;
        }
    };

    public static final Func2<long[], long[], long[]> bucketAggregator = new Func2<long[], long[], long[]>() {
        @Override
        public long[] call(long[] cumulativeEvents, long[] bucketEventCounts) {
            for (HystrixEventType.Collapser eventType: ALL_EVENT_TYPES) {
                cumulativeEvents[eventType.ordinal()] += bucketEventCounts[eventType.ordinal()];
            }
            return cumulativeEvents;
        }
    };

    /**
     * Clears all state from metrics. If new requests come in instances will be recreated and metrics started from scratch.
     */
    /* package */ static void reset() {
        metrics.get().clear();
    }

    private final HystrixCollapserKey collapserKey;
    private final HystrixCollapserProperties properties;

    private final RollingCollapserEventCounterStream rollingCollapserEventCounterStream;
    private final CumulativeCollapserEventCounterStream cumulativeCollapserEventCounterStream;
    private final RollingCollapserBatchSizeDistributionStream rollingCollapserBatchSizeDistributionStream;

    /* package */HystrixCollapserMetrics(HystrixCollapserKey key, HystrixCollapserProperties properties) {
        super(null);
        this.collapserKey = key;
        this.properties = properties;

        rollingCollapserEventCounterStream = RollingCollapserEventCounterStream.getInstance(key, properties);
        cumulativeCollapserEventCounterStream = CumulativeCollapserEventCounterStream.getInstance(key, properties);
        rollingCollapserBatchSizeDistributionStream = RollingCollapserBatchSizeDistributionStream.getInstance(key, properties);
    }

    /**
     * {@link HystrixCollapserKey} these metrics represent.
     * 
     * @return HystrixCollapserKey
     */
    public HystrixCollapserKey getCollapserKey() {
        return collapserKey;
    }

    public HystrixCollapserProperties getProperties() {
        return properties;
    }

    public long getRollingCount(HystrixEventType.Collapser collapserEventType) {
        return rollingCollapserEventCounterStream.getLatest(collapserEventType);
    }

    public long getCumulativeCount(HystrixEventType.Collapser collapserEventType) {
        return cumulativeCollapserEventCounterStream.getLatest(collapserEventType);
    }

    @Override
    public long getCumulativeCount(HystrixRollingNumberEvent event) {
        return getCumulativeCount(HystrixEventType.Collapser.from(event));
    }

    @Override
    public long getRollingCount(HystrixRollingNumberEvent event) {
        return getRollingCount(HystrixEventType.Collapser.from(event));
    }

    /**
     * Retrieve the batch size for the {@link HystrixCollapser} being invoked at a given percentile.
     * <p>
     * Percentile capture and calculation is configured via {@link HystrixCollapserProperties#metricsRollingStatisticalWindowInMilliseconds()} and other related properties.
     *
     * @param percentile
     *            Percentile such as 50, 99, or 99.5.
     * @return batch size
     */
    public int getBatchSizePercentile(double percentile) {
        return rollingCollapserBatchSizeDistributionStream.getLatestPercentile(percentile);
    }

    public int getBatchSizeMean() {
        return rollingCollapserBatchSizeDistributionStream.getLatestMean();
    }

    /**
     * Retrieve the shard size for the {@link HystrixCollapser} being invoked at a given percentile.
     * <p>
     * Percentile capture and calculation is configured via {@link HystrixCollapserProperties#metricsRollingStatisticalWindowInMilliseconds()} and other related properties.
     *
     * @param percentile
     *            Percentile such as 50, 99, or 99.5.
     * @return batch size
     */
    public int getShardSizePercentile(double percentile) {
        return 0;
        //return rollingCollapserUsageDistributionStream.getLatestBatchSizePercentile(percentile);
    }

    public int getShardSizeMean() {
        return 0;
        //return percentileShardSize.getMean();
    }

    public void markRequestBatched() {
    }

    public void markResponseFromCache() {
        HystrixThreadEventStream.getInstance().collapserResponseFromCache(collapserKey);
    }

    public void markBatch(int batchSize) {
        HystrixThreadEventStream.getInstance().collapserBatchExecuted(collapserKey, batchSize);
    }

    public void markShards(int numShards) {
    }
}
