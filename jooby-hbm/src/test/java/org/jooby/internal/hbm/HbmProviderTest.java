package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HbmProvider.class, Bootstrap.class })
public class HbmProviderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(HbmUnitDescriptor.class, Map.class)
        .run(unit -> {
          new HbmProvider(unit.get(HbmUnitDescriptor.class), unit.get(Map.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void startAndGet() throws Exception {
    new MockUnit(HibernateEntityManagerFactory.class, HbmUnitDescriptor.class, Map.class)
        .expect(unit -> {
          HibernateEntityManagerFactory emf = unit.get(HibernateEntityManagerFactory.class);

          EntityManagerFactoryBuilder emfb = unit.mock(EntityManagerFactoryBuilder.class);
          expect(emfb.build()).andReturn(emf);

          unit.mockStatic(Bootstrap.class);
          expect(
              Bootstrap.getEntityManagerFactoryBuilder(unit.get(HbmUnitDescriptor.class),
                  unit.get(Map.class))).andReturn(emfb);
        })
        .run(unit -> {
          HbmProvider hbm = new HbmProvider(unit.get(HbmUnitDescriptor.class), unit.get(Map.class));
          hbm.start();
          assertEquals(unit.get(HibernateEntityManagerFactory.class), hbm.get());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void stop() throws Exception {
    new MockUnit(HibernateEntityManagerFactory.class, HbmUnitDescriptor.class, Map.class)
        .expect(unit -> {
          HibernateEntityManagerFactory emf = unit.get(HibernateEntityManagerFactory.class);

          emf.close();

          EntityManagerFactoryBuilder emfb = unit.mock(EntityManagerFactoryBuilder.class);
          expect(emfb.build()).andReturn(emf);

          unit.mockStatic(Bootstrap.class);
          expect(
              Bootstrap.getEntityManagerFactoryBuilder(unit.get(HbmUnitDescriptor.class),
                  unit.get(Map.class))).andReturn(emfb);
        })
        .run(unit -> {
          HbmProvider hbm = new HbmProvider(unit.get(HbmUnitDescriptor.class), unit.get(Map.class));
          hbm.start();
          hbm.stop();
          hbm.stop();
        });
  }
}
