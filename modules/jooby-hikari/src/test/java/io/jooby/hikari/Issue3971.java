/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hikari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import io.jooby.Environment;

public class Issue3971 {
  @Test
  public void shouldSetMariaDbDatasource() {
    HikariConfig conf =
        HikariModule.build(
            new Environment(
                getClass().getClassLoader(),
                HikariModuleTest.mapOf(
                    "db.url", "jdbc:mariadb://localhost/db", "db.user", "root", "db.password", ""),
                "test"),
            "db");
    assertEquals("org.mariadb.jdbc.MariaDbDataSource", conf.getDataSourceClassName());
    assertEquals(null, conf.getJdbcUrl());
    assertEquals("mariadb.db", conf.getPoolName());
    assertNotNull(conf.getDataSourceProperties());
    assertEquals("root", conf.getUsername());
    assertEquals("", conf.getPassword());
    assertEquals("jdbc:mariadb://localhost/db", conf.getDataSourceProperties().getProperty("url"));
  }
}
