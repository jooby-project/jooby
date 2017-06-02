package org.jooby.internal.ehcache;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.Assert.assertEquals;

import java.util.List;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheDecoratorFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExceptionHandlerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExtensionFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration.WriteMode;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.PinningConfiguration.Store;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration.MaxDepthExceededBehavior;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.readthrough.ReadThroughCache;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.event.NotificationScope;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.jooby.internal.ehcache.CacheConfigurationBuilder;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CacheConfigurationBuilderTest {

  @Test
  public void cacheLoaderTimeout() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheLoaderTimeout", fromAnyRef("3s"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(3000, cache.getCacheLoaderTimeoutMillis());
  }

  @Test
  public void cacheLoaderTimeoutMillis() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheLoaderTimeoutMillis", fromAnyRef(500));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(500, cache.getCacheLoaderTimeoutMillis());
  }

  @Test
  public void clearOnFlush() {
    Config config = ConfigFactory
        .empty()
        .withValue("clearOnFlush", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.isClearOnFlush());
  }

  @Test
  public void copyOnRead() {
    Config config = ConfigFactory
        .empty()
        .withValue("copyOnRead", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.isCopyOnRead());
  }

  @Test
  public void diskAccessStripes() {
    Config config = ConfigFactory
        .empty()
        .withValue("diskAccessStripes", fromAnyRef(10));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(10, cache.getDiskAccessStripes());
  }

  @Test
  public void diskExpiryThreadInterval() {
    Config config = ConfigFactory
        .empty()
        .withValue("diskExpiryThreadInterval", fromAnyRef("1m"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(60, cache.getDiskExpiryThreadIntervalSeconds());
  }

  @Test
  public void diskExpiryThreadIntervalSeconds() {
    Config config = ConfigFactory
        .empty()
        .withValue("diskExpiryThreadIntervalSeconds", fromAnyRef(60));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(60, cache.getDiskExpiryThreadIntervalSeconds());
  }

  @Test
  public void diskSpoolBufferSizeMB() {
    Config config = ConfigFactory
        .empty()
        .withValue("diskSpoolBufferSizeMB", fromAnyRef(5));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(5, cache.getDiskSpoolBufferSizeMB());
  }

  @Test
  public void eternal() {
    Config config = ConfigFactory
        .empty()
        .withValue("eternal", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.isEternal());
  }

  @Test
  public void logging() {
    Config config = ConfigFactory
        .empty()
        .withValue("logging", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.getLogging());
  }

  @Test
  public void copyOnWrite() {
    Config config = ConfigFactory
        .empty()
        .withValue("copyOnWrite", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.isCopyOnWrite());
  }

  @Test
  public void maxBytesLocalDisk() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxBytesLocalDisk", fromAnyRef("1k"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1024, cache.getMaxBytesLocalDisk());
  }

  @Test
  public void maxBytesLocalHeap() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxBytesLocalHeap", fromAnyRef("1k"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1024, cache.getMaxBytesLocalHeap());
  }

  @Test
  public void maxBytesLocalOffHeap() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxBytesLocalOffHeap", fromAnyRef("1k"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1024, cache.getMaxBytesLocalOffHeap());
  }

  @Test
  public void maxElementsInMemory() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxElementsInMemory", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxEntriesLocalHeap());
  }

  @Test
  public void maxElementsOnDisk() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxElementsOnDisk", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxEntriesLocalDisk());
  }

  @Test
  public void maxEntriesInCache() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxEntriesInCache", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxEntriesInCache());
  }

  @Test
  public void maxEntriesLocalDisk() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxEntriesLocalDisk", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxEntriesLocalDisk());
  }

  @Test
  public void maxEntriesLocalHeap() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxEntriesLocalHeap", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxEntriesLocalHeap());
  }

  @Test
  public void maxMemoryOffHeap() {
    Config config = ConfigFactory
        .empty()
        .withValue("maxMemoryOffHeap", fromAnyRef(99));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(99, cache.getMaxBytesLocalOffHeap());
  }

  @Test
  public void memoryStoreEvictionPolicy() {
    Config config = ConfigFactory
        .empty()
        .withValue("memoryStoreEvictionPolicy", fromAnyRef("LRU"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(MemoryStoreEvictionPolicy.LRU, cache.getMemoryStoreEvictionPolicy());
  }

  @Test
  public void overflowToOffHeap() {
    Config config = ConfigFactory
        .empty()
        .withValue("overflowToOffHeap", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(true, cache.isOverflowToOffHeap());
  }

  @Test
  public void timeToIdle() {
    Config config = ConfigFactory
        .empty()
        .withValue("timeToIdle", fromAnyRef("1s"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1, cache.getTimeToIdleSeconds());
  }

  @Test
  public void timeToIdleSeconds() {
    Config config = ConfigFactory
        .empty()
        .withValue("timeToIdleSeconds", fromAnyRef(1));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1, cache.getTimeToIdleSeconds());
  }

  @Test
  public void timeToLive() {
    Config config = ConfigFactory
        .empty()
        .withValue("timeToLive", fromAnyRef("1s"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1, cache.getTimeToLiveSeconds());
  }

  @Test
  public void timeToLiveSeconds() {
    Config config = ConfigFactory
        .empty()
        .withValue("timeToLiveSeconds", fromAnyRef(1));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(1, cache.getTimeToLiveSeconds());
  }

  @Test
  public void transactionalMode() {
    Config config = ConfigFactory
        .empty()
        .withValue("transactionalMode", fromAnyRef("local"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    assertEquals(TransactionalMode.LOCAL, cache.getTransactionalMode());
  }

  @Test
  public void persistence() {
    Config config = ConfigFactory
        .empty()
        .withValue("persistence.strategy", fromAnyRef("LOCALRESTARTABLE"))
        .withValue("persistence.synchronousWrites", fromAnyRef(true));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    PersistenceConfiguration persistence = cache.getPersistenceConfiguration();

    assertEquals(Strategy.LOCALRESTARTABLE, persistence.getStrategy());
    assertEquals(true, persistence.getSynchronousWrites());
  }

  @Test
  public void bootstrapCacheLoaderFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("bootstrapCacheLoaderFactory.class",
            fromAnyRef("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory"))
        .withValue("bootstrapCacheLoaderFactory.bootstrapAsynchronously",
            fromAnyRef(true))
        .withValue("bootstrapCacheLoaderFactory.maximumChunkSizeBytes",
            fromAnyRef(5000000));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoader = cache
        .getBootstrapCacheLoaderFactoryConfiguration();
    assertEquals("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory",
        bootstrapCacheLoader.getFullyQualifiedClassPath());
    assertEquals("bootstrapAsynchronously=true;maximumChunkSizeBytes=5000000",
        bootstrapCacheLoader.getProperties());
  }

  @Test
  public void cacheDecoratorFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheDecoratorFactory.class",
            fromAnyRef(BlockingCache.class.getName()));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheDecoratorFactoryConfiguration> decorators = cache.getCacheDecoratorConfigurations();
    assertEquals(1, decorators.size());
    assertEquals(BlockingCache.class.getName(), decorators.iterator().next()
        .getFullyQualifiedClassPath());
  }

  @Test
  public void cacheDecoratorFactories() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheDecoratorFactory.blocking.class",
            fromAnyRef(BlockingCache.class.getName()))
        .withValue("cacheDecoratorFactory.blocking.p1",
            fromAnyRef(BlockingCache.class.getName()))
        .withValue("cacheDecoratorFactory.readT.class",
            fromAnyRef(ReadThroughCache.class.getName()));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheDecoratorFactoryConfiguration> decorators = cache.getCacheDecoratorConfigurations();
    assertEquals(2, decorators.size());
    assertEquals(BlockingCache.class.getName(), decorators.get(0).getFullyQualifiedClassPath());
    assertEquals(ReadThroughCache.class.getName(), decorators.get(1).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheEventListenerFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheEventListenerFactory.class",
            fromAnyRef("my.Listener"))
        .withValue("cacheEventListenerFactory.listenFor",
            fromAnyRef("local"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheEventListenerFactoryConfiguration> listeners = cache
        .getCacheEventListenerConfigurations();
    assertEquals(1, listeners.size());
    assertEquals("my.Listener", listeners.get(0).getFullyQualifiedClassPath());
    assertEquals(NotificationScope.LOCAL, listeners.get(0).getListenFor());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheEventListenerFactories() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheEventListenerFactory.l1.class",
            fromAnyRef("my.Listener"))
        .withValue("cacheEventListenerFactory.l1.listenFor",
            fromAnyRef("local"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheEventListenerFactoryConfiguration> listeners = cache
        .getCacheEventListenerConfigurations();
    assertEquals(1, listeners.size());
    assertEquals("my.Listener", listeners.get(0).getFullyQualifiedClassPath());
    assertEquals(NotificationScope.LOCAL, listeners.get(0).getListenFor());

  }

  @Test
  public void cacheExceptionHandlerFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheExceptionHandlerFactory.class",
            fromAnyRef("my.ExceptionHandler"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    CacheExceptionHandlerFactoryConfiguration ex = cache
        .getCacheExceptionHandlerFactoryConfiguration();
    assertEquals("my.ExceptionHandler", ex.getFullyQualifiedClassPath());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheExtensionFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheExtensionFactory.class",
            fromAnyRef(ScheduledRefreshCacheExtension.class.getName()));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheExtensionFactoryConfiguration> extensions = cache.getCacheExtensionConfigurations();
    assertEquals(ScheduledRefreshCacheExtension.class.getName(), extensions.get(0)
        .getFullyQualifiedClassPath());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheExtensionFactories() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheExtensionFactory.e1.class",
            fromAnyRef("Ext1"))
        .withValue("cacheExtensionFactory.e2.class",
            fromAnyRef("Ext2"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheExtensionFactoryConfiguration> extensions = cache.getCacheExtensionConfigurations();
    assertEquals("Ext1", extensions.get(0).getFullyQualifiedClassPath());
    assertEquals("Ext2", extensions.get(1).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheLoaderFactory() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheLoaderFactory.class",
            fromAnyRef("CacheLoaderFactory1"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheLoaderFactoryConfiguration> extensions = cache.getCacheLoaderConfigurations();
    assertEquals("CacheLoaderFactory1", extensions.get(0).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void cacheLoaderFactories() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheLoaderFactory.f1.class",
            fromAnyRef("CacheLoaderFactory1"))
        .withValue("cacheLoaderFactory.f2.class",
            fromAnyRef("CacheLoaderFactory2"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    List<CacheLoaderFactoryConfiguration> extensions = cache.getCacheLoaderConfigurations();
    assertEquals("CacheLoaderFactory1", extensions.get(0).getFullyQualifiedClassPath());
    assertEquals("CacheLoaderFactory2", extensions.get(1).getFullyQualifiedClassPath());
  }

  @Test
  public void cacheWriter() {
    Config config = ConfigFactory
        .empty()
        .withValue("cacheWriter.maxWriteDelay", fromAnyRef(100))
        .withValue("cacheWriter.minWriteDelay", fromAnyRef(10))
        .withValue("cacheWriter.notifyListenersOnException", fromAnyRef(true))
        .withValue("cacheWriter.rateLimitPerSecond", fromAnyRef(1))
        .withValue("cacheWriter.retryAttemptDelay", fromAnyRef("1m"))
        .withValue("cacheWriter.retryAttempts", fromAnyRef(5))
        .withValue("cacheWriter.writeBatching", fromAnyRef(true))
        .withValue("cacheWriter.writeBatchSize", fromAnyRef(10))
        .withValue("cacheWriter.writeBehindConcurrency", fromAnyRef(3))
        .withValue("cacheWriter.writeBehindMaxQueueSize", fromAnyRef(31))
        .withValue("cacheWriter.writeCoalescing", fromAnyRef(true))
        .withValue("cacheWriter.writeMode", fromAnyRef("WRITE_BEHIND"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    CacheWriterConfiguration writer = cache.getCacheWriterConfiguration();
    assertEquals(100, writer.getMaxWriteDelay());
    assertEquals(10, writer.getMinWriteDelay());
    assertEquals(true, writer.getNotifyListenersOnException());
    assertEquals(1, writer.getRateLimitPerSecond());
    assertEquals(60, writer.getRetryAttemptDelaySeconds());
    assertEquals(5, writer.getRetryAttempts());
    assertEquals(true, writer.getWriteBatching());
    assertEquals(10, writer.getWriteBatchSize());
    assertEquals(3, writer.getWriteBehindConcurrency());
    assertEquals(WriteMode.WRITE_BEHIND, writer.getWriteMode());
  }

  @Test
  public void pinning() {
    Config config = ConfigFactory
        .empty()
        .withValue("pinning.store", fromAnyRef("localMemory"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    PinningConfiguration pinning = cache.getPinningConfiguration();
    assertEquals(Store.LOCALMEMORY, pinning.getStore());
  }

  @Test
  public void sizeOfPolicy() {
    Config config = ConfigFactory
        .empty()
        .withValue("sizeOfPolicy.maxDepth", fromAnyRef(100))
        .withValue("sizeOfPolicy.maxDepthExceededBehavior", fromAnyRef("abort"));

    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    SizeOfPolicyConfiguration size = cache.getSizeOfPolicyConfiguration();
    assertEquals(100, size.getMaxDepth());
    assertEquals(MaxDepthExceededBehavior.ABORT, size.getMaxDepthExceededBehavior());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void terracota() {
    Config config = ConfigFactory
        .empty()
        .withValue("terracotta.nonstop.timeout.type", fromAnyRef("noop"))
        .withValue("terracotta.nonstop.timeout.p1", fromAnyRef("v1"))
        .withValue("terracotta.nonstop.searchTimeoutMillis", fromAnyRef("1s"))
        .withValue("terracotta.nonstop.bulkOpsTimeoutMultiplyFactor", fromAnyRef(99))
        .withValue("terracotta.nonstop.enabled", fromAnyRef(true))
        .withValue("terracotta.nonstop.immediateTimeout", fromAnyRef(true))
        .withValue("terracotta.nonstop.timeoutMillis", fromAnyRef("5s"))
        .withValue("terracotta.cacheXA", fromAnyRef(false))
        .withValue("terracotta.clustered", fromAnyRef(false))
        .withValue("terracotta.coherent", fromAnyRef(true))
        .withValue("terracotta.compressionEnabled", fromAnyRef(true))
        .withValue("terracotta.concurrency", fromAnyRef(87))
        .withValue("terracotta.consistency", fromAnyRef("STRONG"))
        .withValue("terracotta.localCacheEnabled", fromAnyRef(true))
        .withValue("terracotta.localKeyCache", fromAnyRef(true))
        .withValue("terracotta.localKeyCacheSize", fromAnyRef(99))
        .withValue("terracotta.orphanEviction", fromAnyRef(true))
        .withValue("terracotta.orphanEvictionPeriod", fromAnyRef(98))
        .withValue("terracotta.synchronousWrites", fromAnyRef(true));


    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    TerracottaConfiguration terracota = cache.getTerracottaConfiguration();
    NonstopConfiguration nonstop = terracota.getNonstopConfiguration();
    TimeoutBehaviorConfiguration timeout = nonstop.getTimeoutBehavior();
    assertEquals("noop", timeout.getType());
    assertEquals("p1=v1", timeout.getProperties());
    assertEquals(5000, nonstop.getSearchTimeoutMillis());
    assertEquals(99, nonstop.getBulkOpsTimeoutMultiplyFactor());
    assertEquals(true, nonstop.isEnabled());
    assertEquals(true, nonstop.isImmediateTimeout());
    assertEquals(30000, nonstop.getTimeoutMillis());

    assertEquals(false, terracota.isCacheXA());
    assertEquals(false, terracota.isClustered());
    assertEquals(true, terracota.isCoherent());
    assertEquals(true, terracota.isCompressionEnabled());
    assertEquals(87, terracota.getConcurrency());
    assertEquals(Consistency.STRONG, terracota.getConsistency());
    assertEquals(true, terracota.isLocalCacheEnabled());
    assertEquals(true, terracota.getLocalKeyCache());
    assertEquals(99, terracota.getLocalKeyCacheSize());
    assertEquals(true, terracota.getOrphanEviction());
    assertEquals(98, terracota.getOrphanEvictionPeriod());
    assertEquals(true, terracota.isSynchronousWrites());
  }

  @Test
  public void terracotaNoCoherent() {
    Config config = ConfigFactory
        .empty()
        .withValue("terracotta.consistency", fromAnyRef("EVENTUAL"));


    CacheConfigurationBuilder builder = new CacheConfigurationBuilder("c1");
    CacheConfiguration cache = builder.build(config);

    TerracottaConfiguration terracota = cache.getTerracottaConfiguration();

    assertEquals(Consistency.EVENTUAL, terracota.getConsistency());
  }

}
