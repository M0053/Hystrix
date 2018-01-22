# Hystrix Cached DataStores

The default implementation of Hystrix DataStores are implemented as non-evicting ConcurrentMaps.
This means data about a key is retained indefinitely instead of only while in use.
This means that when using a large number or dynamic keys that all entries persist until the application is stopped.

Ideally all data related to a key would be evicted once a full time window has passed since it was last used.
This would be extremely difficult to manage given each key can have different windows and most caches use a global
eviction policy instead of a per entry eviction policy.

To mitigate this however this plugin provides an implementation of the DataStore backed by Guava Cache instead of a ConcurrentMap.
The general approach is to configure a cache eviction policy to evict entries some time after their windows expired.
For example if most of your commands use window of 10 seconds you could configure the cache to evict 60 seconds after
access. This means that any keys which haven't been used in 60 seconds will be evicted from all DataStores and will be
re-initialized on next access. Optionally you can also configure a maximum number of keys to Cache, this will prevent 
one-off keys from taking up space that can be used by more frequently used keys.

These numbers will probably need to be dialed in by running various configuration to determine what combination give the
right balance of performance and memory usage.