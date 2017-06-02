package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

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


  @Test
  public void failPackageScanning() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          ClassLoader loader = unit.get(ClassLoader.class);
          expect(loader.getResources("x/y/z")).andThrow(new IOException("Missing x.y.z"));
        })
        .run(unit -> {
          new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
              .get(Config.class), Sets.newHashSet("x.y.z"));
        });
  }


  @Test(expected = UnsupportedOperationException.class)
  public void pushClassTransformer() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .run(unit -> {
          new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
              .get(Config.class), Collections.emptySet())
                  .pushClassTransformer(null);
        });
  }


  @Test
  public void useQuotedIdentifiers() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.useQuotedIdentifiers")).andReturn(true);
          expect(config.getAnyRef("hibernate.useQuotedIdentifiers")).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isUseQuotedIdentifiers());
        });
  }


  @Test
  public void dontUseQuotedIdentifiers() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.useQuotedIdentifiers")).andReturn(true);
          expect(config.getAnyRef("hibernate.useQuotedIdentifiers")).andReturn(false);
        })
        .run(unit -> {
          assertEquals(false,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isUseQuotedIdentifiers());
        });
  }


  @Test
  public void excludeUnlistedClasses() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("hibernate.excludeUnlistedClasses")).andReturn(true);
          expect(config.getAnyRef("hibernate.excludeUnlistedClasses")).andReturn(false);
        })
        .run(unit -> {
          assertEquals(false,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .isExcludeUnlistedClasses());
        });
  }


  @Test
  public void validationMode() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("javax.persistence.validation.mode")).andReturn(true);
          expect(config.getAnyRef("javax.persistence.validation.mode")).andReturn("AUTO");
        })
        .run(unit -> {
          assertEquals(ValidationMode.AUTO,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getValidationMode());
        });
  }


  @Test
  public void transactionType() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .run(unit -> {
          assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getTransactionType());
        });
  }


  @Test
  public void sharedCacheMode() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("javax.persistence.sharedCache.mode")).andReturn(true);
          expect(config.getAnyRef("javax.persistence.sharedCache.mode")).andReturn("ALL");
        })
        .run(unit -> {
          assertEquals(SharedCacheMode.ALL,
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getSharedCacheMode());
        });
  }


  @Test
  public void providerClassName() throws Exception {
    new MockUnit(ClassLoader.class, DataSource.class, Config.class)
        .run(unit -> {
          assertEquals("org.hibernate.jpa.HibernatePersistenceProvider",
              new HbmUnitDescriptor(unit.get(ClassLoader.class), unit.get(DataSource.class), unit
                  .get(Config.class), Collections.emptySet())
                      .getProviderClassName());
        });
  }


  @Test
  public void properties() throws Exception {
    Properties expected = new Properties();
    expected.setProperty("javax.persistence.sharedCache.mode", "ALL");
    expected.setProperty("javax.persistence.validation.mode", "AUTO");

    Config config = ConfigFactory.empty()
        .withValue("javax.persistence.sharedCache.mode", ConfigValueFactory.fromAnyRef("ALL"))
        .withValue("javax.persistence.validation.mode", ConfigValueFactory.fromAnyRef("AUTO"))
        .withValue("hibernate.excludeUnlistedClasses", ConfigValueFactory.fromAnyRef("false"));
    new MockUnit(ClassLoader.class, DataSource.class)
        .run(unit -> {
          assertEquals(expected,
              new HbmUnitDescriptor(
                  unit.get(ClassLoader.class), unit.get(DataSource.class), config,
                  Collections.emptySet())
                      .getProperties());
        });
  }


  @Test
  public void persistenceUnitRootUrl() throws Exception {
    new MockUnit(Config.class, DataSource.class)
        .run(unit -> {
          assertNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getPersistenceUnitRootUrl());
        });
  }


  @Test
  public void persistenceUnitRootUrlNoScan() throws Exception {
    new MockUnit(Config.class, DataSource.class)
        .run(unit -> {
          assertNull(new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getPersistenceUnitRootUrl());
        });
  }


  @Test
  public void nonJtaDataSource() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(unit.get(DataSource.class), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getNonJtaDataSource());
        });
  }


  @Test
  public void dsname() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(unit.get(DataSource.class).toString(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getName());
        });
  }


  @Test
  public void mappingFileNames() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getMappingFileNames());
        });
  }


  @Test
  public void managedClassNames() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getManagedClassNames());
        });
  }


  @Test
  public void jtaDataSource() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(null, new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getJtaDataSource());
        });
  }


  @Test
  public void noJarFileUrls() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(Collections.emptyList(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getJarFileUrls());
        });
  }


  @Test
  public void jarFileUrls() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          HbmUnitDescriptor desc = new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Sets.newLinkedHashSet(Arrays.asList("x.y", "x.y.z")));
          assertEquals(getClass().getResource("/x/y"), desc.getPersistenceUnitRootUrl());
          assertEquals(Arrays.asList(getClass().getResource("/x/y/z")), desc.getJarFileUrls());
        });
  }


  @Test
  public void getClassLoader() throws Exception {
    new MockUnit(Config.class, DataSource.class, DataSource.class)
        .run(unit -> {
          assertEquals(getClass().getClassLoader(), new HbmUnitDescriptor(
              getClass().getClassLoader(), unit.get(DataSource.class), unit.get(Config.class),
              Collections.emptySet()).getClassLoader());
        });
  }

}
