package org.jooby.internal.ehcache;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.Assert.assertEquals;

import java.util.List;

import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.Configuration.Monitoring;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration.MaxDepthExceededBehavior;
import net.sf.ehcache.config.TerracottaClientConfiguration;

import org.jooby.internal.ehcache.ConfigurationBuilder;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigurationBuilderTest {

  @Test
  public void defaultTransactionTimeoutInSeconds() {

    Config config = ConfigFactory.empty()
        .withValue("defaultTransactionTimeoutInSeconds", fromAnyRef(4));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(4, eh.getDefaultTransactionTimeoutInSeconds());
  }

  @Test
  public void defaultTransactionTimeout() {

    Config config = ConfigFactory.empty()
        .withValue("defaultTransactionTimeout", fromAnyRef("4s"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(4, eh.getDefaultTransactionTimeoutInSeconds());
  }

  @Test
  public void dynamicConfig() {

    Config config = ConfigFactory.empty()
        .withValue("dynamicConfig", fromAnyRef(true));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(true, eh.getDynamicConfig());
  }

  @Test
  public void maxBytesLocalDisk() {

    Config config = ConfigFactory.empty()
        .withValue("maxBytesLocalDisk", fromAnyRef("1k"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(1024, eh.getMaxBytesLocalDisk());
  }

  @Test
  public void maxBytesLocalHeap() {

    Config config = ConfigFactory.empty()
        .withValue("maxBytesLocalHeap", fromAnyRef("1k"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(1024, eh.getMaxBytesLocalHeap());
  }

  @Test
  public void maxBytesLocalOffHeap() {

    Config config = ConfigFactory.empty()
        .withValue("maxBytesLocalOffHeap", fromAnyRef("1k"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(1024, eh.getMaxBytesLocalOffHeap());
  }

  @Test
  public void monitoring() {

    Config config = ConfigFactory.empty()
        .withValue("monitoring", fromAnyRef("off"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals(Monitoring.OFF, eh.getMonitoring());
  }

  @Test
  public void name() {

    Config config = ConfigFactory.empty()
        .withValue("name", fromAnyRef("Name"));

    Configuration eh = new ConfigurationBuilder().build(config);

    assertEquals("Name", eh.getName());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheManagerEventListenerFactory() {

    Config config = ConfigFactory.empty()
        .withValue("cacheManagerEventListenerFactory.class", fromAnyRef("MyEventListener"));

    Configuration eh = new ConfigurationBuilder().build(config);
    FactoryConfiguration factory = eh.getCacheManagerEventListenerFactoryConfiguration();
    assertEquals("MyEventListener", factory.getFullyQualifiedClassPath());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheManagerPeerListenerFactory() {

    Config config = ConfigFactory.empty()
        .withValue("cacheManagerPeerListenerFactory.class", fromAnyRef("PeerListener"));

    Configuration eh = new ConfigurationBuilder().build(config);
    List<FactoryConfiguration> factory = eh.getCacheManagerPeerListenerFactoryConfigurations();
    assertEquals("PeerListener", factory.get(0).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheManagerPeerListenerFactories() {

    Config config = ConfigFactory.empty()
        .withValue("cacheManagerPeerListenerFactory.p1.class", fromAnyRef("PeerListener1"));

    Configuration eh = new ConfigurationBuilder().build(config);
    List<FactoryConfiguration> factory = eh.getCacheManagerPeerListenerFactoryConfigurations();
    assertEquals("PeerListener1", factory.get(0).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheManagerPeerProviderFactory() {

    Config config = ConfigFactory.empty()
        .withValue("cacheManagerPeerProviderFactory.class", fromAnyRef("PeerProvider"));

    Configuration eh = new ConfigurationBuilder().build(config);
    List<FactoryConfiguration> factory = eh.getCacheManagerPeerProviderFactoryConfiguration();
    assertEquals("PeerProvider", factory.get(0).getFullyQualifiedClassPath());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void cacheManagerPeerProviderFactories() {

    Config config = ConfigFactory.empty()
        .withValue("cacheManagerPeerProviderFactory.pp1.class", fromAnyRef("PeerProvider"));

    Configuration eh = new ConfigurationBuilder().build(config);
    List<FactoryConfiguration> factory = eh.getCacheManagerPeerProviderFactoryConfiguration();
    assertEquals("PeerProvider", factory.get(0).getFullyQualifiedClassPath());
  }

  @Test
  public void diskStore() {

    Config config = ConfigFactory.empty()
        .withValue("diskStore.path", fromAnyRef("target"));

    Configuration eh = new ConfigurationBuilder().build(config);
    DiskStoreConfiguration diskStore = eh.getDiskStoreConfiguration();
    assertEquals("target", diskStore.getPath());
  }

  @Test
  public void sizeOfPolicy() {

    Config config = ConfigFactory.empty()
        .withValue("sizeOfPolicy.maxDepth", fromAnyRef(100))
        .withValue("sizeOfPolicy.maxDepthExceededBehavior", fromAnyRef("abort"));


    Configuration eh = new ConfigurationBuilder().build(config);
    SizeOfPolicyConfiguration size = eh.getSizeOfPolicyConfiguration();
    assertEquals(100, size.getMaxDepth());
    assertEquals(MaxDepthExceededBehavior.ABORT, size.getMaxDepthExceededBehavior());
  }

  @Test
  public void terracottaConfig() {

    Config config = ConfigFactory.empty()
        .withValue("terracottaConfig.rejoin", fromAnyRef(true))
        .withValue("terracottaConfig.url", fromAnyRef("http://localhost:6897"))
        .withValue("terracottaConfig.wanEnabledTSA", fromAnyRef(true));


    Configuration eh = new ConfigurationBuilder().build(config);
    TerracottaClientConfiguration terracota = eh.getTerracottaConfiguration();
    assertEquals(true, terracota.isRejoin());
    assertEquals("http://localhost:6897", terracota.getUrl());
    assertEquals(true, terracota.isWanEnabledTSA());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void transactionManagerLookup() {

    Config config = ConfigFactory.empty()
        .withValue("transactionManagerLookup.class", fromAnyRef("TrxML"));


    Configuration eh = new ConfigurationBuilder().build(config);
    FactoryConfiguration terracota = eh.getTransactionManagerLookupConfiguration();
    assertEquals("TrxML", terracota.getFullyQualifiedClassPath());
  }

}
