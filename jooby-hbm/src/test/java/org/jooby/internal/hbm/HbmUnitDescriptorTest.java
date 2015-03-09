package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Properties;

import javax.inject.Provider;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.jooby.MockUnit;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmUnitDescriptorTest {

  @SuppressWarnings("unchecked")
  @Test(expected = UnsupportedOperationException.class)
  public void pushClassTransformer() throws Exception {
    new MockUnit(ClassLoader.class, Provider.class, Config.class)
        .run(unit -> {
          new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(Provider.class), unit
              .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  .get(Config.class), true)
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
                  unit.get(ClassLoader.class), unit.get(Provider.class), config, true
              ).getProperties());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void persistenceUnitRootUrl() throws Exception {
    new MockUnit(Config.class, Provider.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.ns")).andReturn(getClass().getPackage().getName());
        })
        .run(unit -> {
          assertNotNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
            ).getPersistenceUnitRootUrl());
          });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void persistenceUnitRootUrlNoScan() throws Exception {
    new MockUnit(Config.class, Provider.class)
        .run(unit -> {
          assertNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), false
          ).getPersistenceUnitRootUrl());
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
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getNonJtaDataSource());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void dsname() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(unit.get(Provider.class).toString(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getName());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void mappingFileNames() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getMappingFileNames());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void managedClassNames() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getManagedClassNames());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jtaDataSource() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(null, new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getJtaDataSource());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jarFileUrls() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(null, new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getJarFileUrls());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getClassLoader() throws Exception {
    new MockUnit(Config.class, Provider.class, DataSource.class)
        .run(unit -> {
          assertEquals(getClass().getClassLoader(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(Provider.class), unit.get(Config.class), true
              ).getClassLoader());
        });
  }

}
