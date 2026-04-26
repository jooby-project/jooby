/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.mysqlclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.ClientBuilder;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;

public class VertxMySQLModuleTest {

  @Test
  public void testConstructorsAndBuilder() {
    // 1. Default Constructor (uses MySQLBuilder::pool)
    VertxMySQLModule defaultModule = new VertxMySQLModule();
    assertNotNull(defaultModule.newBuilder());

    // 2. Supplier Constructor
    Supplier<ClientBuilder<? extends SqlClient>> clientSupplier = MySQLBuilder::client;
    VertxMySQLModule supplierModule = new VertxMySQLModule(clientSupplier);
    assertNotNull(supplierModule.newBuilder());

    // 3. Named and Builder Constructor
    VertxMySQLModule namedModule = new VertxMySQLModule("mysql-db", MySQLBuilder::pool);
    assertNotNull(namedModule.newBuilder());
  }

  @Test
  public void testConfigParsing() {
    VertxMySQLModule module = new VertxMySQLModule();

    // Test URI parsing logic
    String uri = "mysql://user:pass@localhost:3306/mydb";
    SqlConnectOptions fromUri = module.fromUri(uri);
    assertTrue(fromUri instanceof MySQLConnectOptions);
    assertEquals("mydb", fromUri.getDatabase());
    assertEquals(3306, fromUri.getPort());

    // Test Map/JSON parsing logic
    JsonObject json =
        new JsonObject().put("host", "127.0.0.1").put("port", 3307).put("database", "jsondb");
    SqlConnectOptions fromMap = module.fromMap(json);
    assertTrue(fromMap instanceof MySQLConnectOptions);
    assertEquals("jsondb", fromMap.getDatabase());
    assertEquals(3307, fromMap.getPort());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testNewBuilderDelegation() {
    // Mock the supplier to ensure newBuilder() delegates correctly to builder.get()
    Supplier<ClientBuilder<? extends SqlClient>> mockSupplier = mock(Supplier.class);
    ClientBuilder mockBuilder = mock(ClientBuilder.class);
    when(mockSupplier.get()).thenReturn(mockBuilder);

    VertxMySQLModule module = new VertxMySQLModule("custom", mockSupplier);

    ClientBuilder<? extends SqlClient> result = module.newBuilder();

    assertEquals(mockBuilder, result);
    verify(mockSupplier).get();
  }
}
