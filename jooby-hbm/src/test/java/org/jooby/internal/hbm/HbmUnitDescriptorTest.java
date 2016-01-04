package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import javax.inject.Provider;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmUnitDescriptorTest {

  @SuppressWarnings("unchecked")
  @Test
  public void failPackageScanning() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          ClassLoader loader = unit.get(ClassLoader.class);
          expect(loader.getResources("x/y/z")).andThrow(new IOException("Missing x.y.z"));
        })
        .run(unit -> {
          new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
              .get(Config.class), Sets.newHashSet("x.y.z"));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = UnsupportedOperationException.class)
  public void pushClassTransformer() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .run(unit -> {
          new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
              .get(Config.class), Collections.emptySet())
                  .pushClassTransformer(null);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void useQuotedIdentifiers() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.useQuotedIdentifiers")).andReturn(true);
          expect(config.getAnyRef("hibernate.useQuotedIdentifiers")).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isUseQuotedIdentifiers());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dontUseQuotedIdentifiers() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.useQuotedIdentifiers")).andReturn(true);
          expect(config.getAnyRef("hibernate.useQuotedIdentifiers")).andReturn(false);
        })
        .run(unit -> {
          assertEquals(false,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isUseQuotedIdentifiers());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void excludeUnlistedClasses() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.excludeUnlistedClasses")).andReturn(true);
          expect(config.getAnyRef("hibernate.excludeUnlistedClasses")).andReturn(false);
        })
        .run(unit -> {
          assertEquals(false,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isExcludeUnlistedClasses());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void validationMode() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("javax.persistence.validation.mode")).andReturn(true);
          expect(config.getAnyRef("javax.persistence.validation.mode")).andReturn("AUTO");
        })
        .run(unit -> {
          assertEquals(ValidationMode.AUTO,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getValidationMode());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void transactionType() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .run(unit -> {
          assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getTransactionType());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sharedCacheMode() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("javax.persistence.sharedCache.mode")).andReturn(true);
          expect(config.getAnyRef("javax.persistence.sharedCache.mode")).andReturn("ALL");
        })
        .run(unit -> {
          assertEquals(SharedCacheMode.ALL,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getSharedCacheMode());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void providerClassName() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .run(unit -> {
          assertEquals("org.hibernate.jpa.HibernatePersistenceProvider",
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getProviderClassName());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void properties() throws Exception {
    Properties expected = new Properties();
    expected.setProperty("javax.persistence.sharedCache.mode", "ALL");
    expected.setProperty("javax.persistence.validation.mode", "AUTO");

    Config config = ConfigFactory.empty()
        .withValue("javax.persistence.sharedCache.mode", ConfigValueFactory.fromAnyRef("ALL"))
        .withValue("javax.persistence.validation.mode", ConfigValueFactory.fromAnyRef("AUTO"))
        .withValue("hibernate.excludeUnlistedClasses", ConfigValueFactory.fromAnyRef("false"));
    new MockUnit(ClassLoader.class, Provider.class)
        .run(unit -> {
          assertEquals(expected,
              new HbmUnitDescriptor(
                  unit.get(ClassLoader.class), unit.get(Provider.class), config,
                  Collections.emptySet())
                      .getProperties());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void persistenceUnitRootUrl() throws Exception {
    new MockUnit(Config.class, Provider.class)
        .run(unit -> {
          assertNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getPersistenceUnitRootUrl());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void persistenceUnitRootUrlNoScan() throws Exception {
    new MockUnit(Config.class, Provider.class)
        .run(unit -> {
          assertNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getPersistenceUnitRootUrl());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void nonJtaDataSource() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .expect(unit -> {
          Provider<DataSource> provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(DataSource.class));
        })
        .run(unit -> {
          assertEquals(unit.get(DataSource.class), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getNonJtaDataSource());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dsname() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(unit.get(Provider.class).toString(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getName());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void mappingFileNames() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getMappingFileNames());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void managedClassNames() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getManagedClassNames());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jtaDataSource() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(null, new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getJtaDataSource());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void noJarFileUrls() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getJarFileUrls());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jarFileUrls() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          HbmUnitDescriptor desc = new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Sets.newLinkedHashSet(Arrays.asList("x.y", "x.y.z")));
          assertEquals(getClass().getResource("/x/y"), desc.getPersistenceUnitRootUrl());
          assertEquals(Arrays.asList(getClass().getResource("/x/y/z")), desc.getJarFileUrls());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getClassLoader() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(getClass().getClassLoader(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class),
              Collections.emptySet()).getClassLoader());
        });
  }

}
