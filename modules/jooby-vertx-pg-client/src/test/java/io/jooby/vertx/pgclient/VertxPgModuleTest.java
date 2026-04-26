/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.pgclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.ClientBuilder;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;

public class VertxPgModuleTest {

  @Test
  public void testConstructors() {
    // 1. Default Constructor (uses PgBuilder::pool)
    VertxPgModule defaultModule = new VertxPgModule();
    assertNotNull(defaultModule.newBuilder());

    // 2. Builder Supplier Constructor
    Supplier<ClientBuilder<? extends SqlClient>> clientSupplier = PgBuilder::client;
    VertxPgModule supplierModule = new VertxPgModule(clientSupplier);
    assertNotNull(supplierModule.newBuilder());

    // 3. Named and Builder Constructor
    VertxPgModule namedModule = new VertxPgModule("pgdb", PgBuilder::pool);
    assertNotNull(namedModule.newBuilder());
  }

  @Test
  public void testConfigParsing() {
    VertxPgModule module = new VertxPgModule();

    // Test URI parsing logic
    String uri = "postgresql://user:pass@localhost:5432/testdb";
    SqlConnectOptions fromUri = module.fromUri(uri);
    assertTrue(fromUri instanceof PgConnectOptions);
    assertEquals("testdb", fromUri.getDatabase());
    assertEquals(5432, fromUri.getPort());

    // Test Map/JSON parsing logic
    JsonObject json =
        new JsonObject().put("host", "127.0.0.1").put("port", 9999).put("database", "jsondb");
    SqlConnectOptions fromMap = module.fromMap(json);
    assertTrue(fromMap instanceof PgConnectOptions);
    assertEquals("jsondb", fromMap.getDatabase());
    assertEquals(9999, fromMap.getPort());
  }

  @Test
  public void testNewBuilderDelegation() {
    // Mock the supplier to ensure newBuilder() delegates correctly to builder.get()
    Supplier<ClientBuilder<? extends SqlClient>> mockSupplier = mock(Supplier.class);
    ClientBuilder mockBuilder = mock(ClientBuilder.class);
    when(mockSupplier.get()).thenReturn(mockBuilder);

    VertxPgModule module = new VertxPgModule("custom", mockSupplier);

    ClientBuilder<? extends SqlClient> result = module.newBuilder();

    assertEquals(mockBuilder, result);
    verify(mockSupplier).get();
  }
}
