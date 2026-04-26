/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.mysqlclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.vertx.mysqlclient.VertxMySQLConnectionProxy;
import io.jooby.internal.vertx.sqlclient.VertxSqlClientProvider;
import io.vertx.core.Deployable;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLConnection;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.impl.SqlClientInternal;

public class VertxMySQLConnectionModuleTest {

  private Jooby app;
  private ServiceRegistry registry;

  @BeforeEach
  void setUp() {
    app = mock(Jooby.class);
    registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);
  }

  @Test
  public void testConstructors() {
    // Covers both "db" default and named paths
    assertNotNull(new VertxMySQLConnectionModule());
    assertNotNull(new VertxMySQLConnectionModule("mydb"));
  }

  @Test
  public void testConfigParsing() {
    VertxMySQLConnectionModule module = new VertxMySQLConnectionModule();

    // 1. fromUri
    String uri = "mysql://user:pass@localhost:3306/testdb";
    SqlConnectOptions optionsUri = module.fromUri(uri);
    assertTrue(optionsUri instanceof MySQLConnectOptions);
    assertEquals("testdb", optionsUri.getDatabase());

    // 2. fromMap
    JsonObject json = new JsonObject().put("host", "127.0.0.1").put("database", "mapdb");
    SqlConnectOptions optionsJson = module.fromMap(json);
    assertTrue(optionsJson instanceof MySQLConnectOptions);
    assertEquals("mapdb", optionsJson.getDatabase());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInstallLogic() {
    String name = "mysql";
    VertxMySQLConnectionModule module = new VertxMySQLConnectionModule(name);
    MySQLConnectOptions options = new MySQLConnectOptions().setDatabase("testdb");

    // Invoke protected method
    module.install(app, name, options);

    ArgumentCaptor<ServiceKey> keyCaptor = ArgumentCaptor.forClass(ServiceKey.class);
    ArgumentCaptor<Object> valCaptor = ArgumentCaptor.forClass(Object.class);

    // Capture from put and putIfAbsent
    verify(registry, atLeast(1)).put(keyCaptor.capture(), valCaptor.capture());
    verify(registry, atLeast(1)).putIfAbsent(keyCaptor.capture(), valCaptor.capture());

    List<ServiceKey> keys = keyCaptor.getAllValues();
    List<Object> values = valCaptor.getAllValues();

    boolean foundMySqlNamed = false;
    boolean foundMySqlDefault = false;
    boolean foundProviderNamed = false;
    boolean foundProviderDefault = false;

    for (int i = 0; i < keys.size(); i++) {
      ServiceKey<?> key = keys.get(i);
      Object val = values.get(i);
      String keyName = key.getName();

      if (key.getType().equals(MySQLConnection.class)) {
        assertTrue(val instanceof VertxMySQLConnectionProxy);
        if (name.equals(keyName)) foundMySqlNamed = true;
        if (keyName == null || "default".equals(keyName)) foundMySqlDefault = true;
      }

      if (key.getType().equals(SqlClientInternal.class)) {
        assertTrue(val instanceof VertxSqlClientProvider);
        if (name.equals(keyName)) foundProviderNamed = true;
        if (keyName == null || "default".equals(keyName)) foundProviderDefault = true;
      }
    }

    assertTrue(foundMySqlNamed);
    assertTrue(foundMySqlDefault);
  }

  @Test
  public void testNewSqlClient() {
    VertxMySQLConnectionModule module = new VertxMySQLConnectionModule();
    MySQLConnectOptions options = new MySQLConnectOptions().setDatabase("db");
    Map<String, List<String>> stmts = Collections.emptyMap();

    Deployable verticle = module.newSqlClient(options, stmts);

    assertNotNull(verticle);
    // Verifies it creates the performance-centric thread-local verticle
    assertEquals(
        "io.jooby.internal.vertx.sqlclient.VertxSqlConnectionVerticle",
        verticle.getClass().getName());
  }
}
