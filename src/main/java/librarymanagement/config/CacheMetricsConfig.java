package librarymanagement.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheMetricsConfig {

    public CacheMetricsConfig(MeterRegistry meterRegistry, CacheManager cacheManager) {
        if (cacheManager instanceof CaffeineCacheManager caffeineCacheManager) {

            Gauge.builder("cache_hit_rate_total", () -> getTotalHitRate(caffeineCacheManager))
                    .register(meterRegistry);

            Gauge.builder("cache_size_total", () -> getTotalSize(caffeineCacheManager))
                    .register(meterRegistry);
        }
    }

    private double getTotalHitRate(CaffeineCacheManager cacheManager) {
        long totalHits = 0;
        long totalRequests = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
                var stats = nativeCache.stats();
                totalHits += stats.hitCount();
                totalRequests += stats.requestCount();
            }
        }

        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    private long getTotalSize(CaffeineCacheManager cacheManager) {
        long totalSize = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
                totalSize += nativeCache.estimatedSize();
            }
        }

        return totalSize;
    }
}