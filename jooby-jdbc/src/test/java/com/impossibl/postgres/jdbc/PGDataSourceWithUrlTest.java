package com.impossibl.postgres.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PGDataSourceWithUrlTest {

  @Test
  public void setUrl() {
    PGDataSourceWithUrl ds = new PGDataSourceWithUrl();
    ds.setUrl("jdbc:pgsql://server/database");
    assertEquals("server", ds.getHost());
    assertEquals(5432, ds.getPort());
    assertEquals("database", ds.getDatabase());
    assertEquals("jdbc:pgsql://server/database", ds.getUrl());
  }

  @Test
  public void setUrlPort() {
    PGDataSourceWithUrl ds = new PGDataSourceWithUrl();
    ds.setUrl("jdbc:pgsql://server:1234/database");
    assertEquals("server", ds.getHost());
    assertEquals(1234, ds.getPort());
    assertEquals("database", ds.getDatabase());
  }
}
