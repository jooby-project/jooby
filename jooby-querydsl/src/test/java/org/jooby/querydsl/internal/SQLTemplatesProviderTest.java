package org.jooby.querydsl.internal;

import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLTemplates;
import org.jooby.test.MockUnit;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;

public class SQLTemplatesProviderTest {

  @Test
  public void getH2() throws Exception {
    new MockUnit(DatabaseMetaData.class, Connection.class, DataSource.class).run(unit -> {
      SQLTemplates templates = new SQLTemplatesProvider("h2").get();
      assertTrue(templates.getClass().equals(H2Templates.class));
    });
  }

}
