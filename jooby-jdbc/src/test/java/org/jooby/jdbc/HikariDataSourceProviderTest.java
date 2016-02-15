package org.jooby.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;

public class HikariDataSourceProviderTest {

  @Test
  public void toStringShouldReturnsDbUrl() {
    Properties props = new Properties();
    props.setProperty("url", "jdbc:db:");

    HikariConfig conf = new HikariConfig();
    conf.setDataSourceProperties(props);
    assertEquals("jdbc:db:", new HikariDataSourceProvider(conf).toString());
  }
}
