/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.pgclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.vertx.pgclient.VertxPgConnectionProxy;
import io.jooby.internal.vertx.sqlclient.VertxSqlClientProvider;
import io.vertx.core.Deployable;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.impl.SqlClientInternal;

public class VertxPgConnectionModuleTest {

  @Test
  public void testConstructors() {
    // Default constructor
    VertxPgConnectionModule m1 = new VertxPgConnectionModule();
    // Using reflection or checking internal state if possible,
    // but here we just ensure they don't crash and initialize.
    assertNotNull(m1);

    // Named constructor
    VertxPgConnectionModule m2 = new VertxPgConnectionModule("db2");
    assertNotNull(m2);
  }

  @Test
  public void testConfigParsing() {
    VertxPgConnectionModule module = new VertxPgConnectionModule();

    // Test URI parsing
    String uri = "postgresql://user:pass@localhost:5432/db";
    SqlConnectOptions optionsUri = module.fromUri(uri);
    assertTrue(optionsUri instanceof PgConnectOptions);
    assertEquals("db", optionsUri.getDatabase());

    // Test Map/JSON parsing
    JsonObject json = new JsonObject().put("host", "localhost").put("database", "mydb");
    SqlConnectOptions optionsJson = module.fromMap(json);
    assertTrue(optionsJson instanceof PgConnectOptions);
    assertEquals("mydb", optionsJson.getDatabase());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInstallLogic() {
    String name = "pg";
    VertxPgConnectionModule module = new VertxPgConnectionModule(name);
    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);

    PgConnectOptions options = new PgConnectOptions().setDatabase("testdb");

    // Invoke the protected install method
    module.install(app, name, options);

    // Capture all calls to put and putIfAbsent
    ArgumentCaptor<ServiceKey> keyCaptor = ArgumentCaptor.forClass(ServiceKey.class);
    ArgumentCaptor<Object> valCaptor = ArgumentCaptor.forClass(Object.class);

    // Capture from both possible registration methods
    verify(registry, atLeast(1)).put(keyCaptor.capture(), valCaptor.capture());
    verify(registry, atLeast(1)).putIfAbsent(keyCaptor.capture(), valCaptor.capture());

    List<ServiceKey> keys = keyCaptor.getAllValues();
    List<Object> values = valCaptor.getAllValues();

    boolean foundPgNamed = false;
    boolean foundPgDefault = false;
    boolean foundProviderNamed = false;
    boolean foundProviderDefault = false;

    for (int i = 0; i < keys.size(); i++) {
      ServiceKey<?> key = keys.get(i);
      Object val = values.get(i);
      String keyName = key.getName();

      if (key.getType().equals(PgConnection.class)) {
        assertTrue(val instanceof VertxPgConnectionProxy);
        if (name.equals(keyName)) {
          foundPgNamed = true;
        } else if (keyName == null || "default".equals(keyName)) {
          foundPgDefault = true;
        }
      }

      if (key.getType().equals(SqlClientInternal.class)) {
        assertTrue(val instanceof VertxSqlClientProvider);
        if (name.equals(keyName)) {
          foundProviderNamed = true;
        } else if (keyName == null || "default".equals(keyName)) {
          foundProviderDefault = true;
        }
      }
    }

    assertTrue(foundPgNamed, "Named PgConnection should be registered with name: " + name);
    assertTrue(foundPgDefault, "Default PgConnection should be registered");
  }

  @Test
  public void testNewSqlClientVerticle() {
    VertxPgConnectionModule module = new VertxPgConnectionModule();
    PgConnectOptions options = new PgConnectOptions().setDatabase("db");
    Map<String, List<String>> stmts = Collections.emptyMap();

    Deployable verticle = module.newSqlClient(options, stmts);

    assertNotNull(verticle);
    // Based on source: return new VertxSqlConnectionVerticle<>(...)
    assertEquals(
        "io.jooby.internal.vertx.sqlclient.VertxSqlConnectionVerticle",
        verticle.getClass().getName());
  }
}
