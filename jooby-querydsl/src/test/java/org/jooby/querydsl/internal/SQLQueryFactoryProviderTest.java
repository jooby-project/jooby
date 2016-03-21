package org.jooby.querydsl.internal;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLCloseListener;
import com.querydsl.sql.SQLQueryFactory;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.sql.DataSource;

import static org.easymock.EasyMock.expectLastCall;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class, SQLQueryFactory.class, SQLQueryFactoryProvider.class, SQLCloseListener.class})
public class SQLQueryFactoryProviderTest {

  @Test
  public void get() throws Exception {
    new MockUnit(Configuration.class, DataSource.class, SQLQueryFactory.class, SQLQueryFactoryProvider.class, SQLCloseListener.class)
        .expect(unit -> {
          Configuration config = unit.get(Configuration.class);
          config.addListener(SQLCloseListener.DEFAULT);
          expectLastCall();
        }).run(unit -> {
      new SQLQueryFactoryProvider(unit.get(Configuration.class), unit.get(DataSource.class)).get();
    });
  }

}