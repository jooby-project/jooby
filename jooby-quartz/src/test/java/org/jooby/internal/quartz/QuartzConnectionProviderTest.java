package org.jooby.internal.quartz;

import static org.easymock.EasyMock.expect;

import java.sql.Connection;

import javax.sql.DataSource;

import org.jooby.MockUnit;
import org.junit.Test;

import com.google.inject.Provider;

public class QuartzConnectionProviderTest {

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullProvider() {
    new QuartzConnectionProvider(null);
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void shouldAcquireConnection() throws Exception {
    new MockUnit(Provider.class, DataSource.class)
        .expect(unit -> {
          DataSource ds = unit.get(DataSource.class);
          expect(ds.getConnection()).andReturn(unit.mock(Connection.class));

          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(ds);
        })
        .run(unit -> {
          new QuartzConnectionProvider(unit.get(Provider.class)).getConnection();
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void shouldInitializeConnection() throws Exception {
    new MockUnit(Provider.class, DataSource.class)
        .expect(unit -> {
          DataSource ds = unit.get(DataSource.class);

          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(ds);
        })
        .run(unit -> {
          new QuartzConnectionProvider(unit.get(Provider.class)).initialize();
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void shouldDoNothingOnShutdown() throws Exception {
    new MockUnit(Provider.class)
        .run(unit -> {
          new QuartzConnectionProvider(unit.get(Provider.class)).shutdown();
        });
  }
}
