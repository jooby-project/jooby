/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.mysqlclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.internal.vertx.sqlclient.VertxThreadLocalSqlConnection;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mysqlclient.MySQLAuthOptions;
import io.vertx.mysqlclient.MySQLConnection;
import io.vertx.mysqlclient.MySQLSetOption;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.spi.DatabaseMetadata;

public class VertxMySQLConnectionProxyTest {

  private MockedStatic<VertxThreadLocalSqlConnection> threadLocalMock;
  private MySQLConnection delegate;
  private VertxMySQLConnectionProxy proxy;
  private final String dbName = "mysql_db";

  @BeforeEach
  void setUp() {
    threadLocalMock = mockStatic(VertxThreadLocalSqlConnection.class);
    delegate = mock(MySQLConnection.class);
    proxy = new VertxMySQLConnectionProxy(dbName);

    threadLocalMock.when(() -> VertxThreadLocalSqlConnection.get(dbName)).thenReturn(delegate);
  }

  @AfterEach
  void tearDown() {
    threadLocalMock.close();
  }

  @Test
  void testGenericSqlDelegation() {
    // Queries
    proxy.query("sql");
    verify(delegate).query("sql");

    proxy.preparedQuery("sql");
    verify(delegate).preparedQuery("sql");

    PrepareOptions options = new PrepareOptions();
    proxy.preparedQuery("sql", options);
    verify(delegate).preparedQuery("sql", options);

    proxy.prepare("sql");
    verify(delegate).prepare("sql");

    proxy.prepare("sql", options);
    verify(delegate).prepare("sql", options);

    // Lifecycle/Transaction
    when(delegate.begin()).thenReturn(Future.succeededFuture());
    assertNotNull(proxy.begin());

    when(delegate.transaction()).thenReturn(null);
    assertNull(proxy.transaction());

    when(delegate.close()).thenReturn(Future.succeededFuture());
    assertNotNull(proxy.close());

    // Metadata
    when(delegate.isSSL()).thenReturn(true);
    assertTrue(proxy.isSSL());

    DatabaseMetadata meta = mock(DatabaseMetadata.class);
    when(delegate.databaseMetadata()).thenReturn(meta);
    assertEquals(meta, proxy.databaseMetadata());
  }

  @Test
  void testMySQLSpecificDelegation() {
    // Handlers (Return delegate)
    Handler<Throwable> excH = h -> {};
    when(delegate.exceptionHandler(excH)).thenReturn(delegate);
    assertEquals(delegate, proxy.exceptionHandler(excH));

    Handler<Void> closeH = h -> {};
    when(delegate.closeHandler(closeH)).thenReturn(delegate);
    assertEquals(delegate, proxy.closeHandler(closeH));

    // Specific Commands
    proxy.ping();
    verify(delegate).ping();

    proxy.specifySchema("test");
    verify(delegate).specifySchema("test");

    proxy.getInternalStatistics();
    verify(delegate).getInternalStatistics();

    proxy.setOption(MySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON);
    verify(delegate).setOption(MySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON);

    proxy.resetConnection();
    verify(delegate).resetConnection();

    proxy.debug();
    verify(delegate).debug();

    MySQLAuthOptions auth = new MySQLAuthOptions();
    proxy.changeUser(auth);
    verify(delegate).changeUser(auth);
  }

  @Test
  void testRecordIdentity() {
    assertEquals(dbName, proxy.name());
    VertxMySQLConnectionProxy other = new VertxMySQLConnectionProxy(dbName);
    assertEquals(proxy, other);
    assertEquals(proxy.hashCode(), other.hashCode());
  }
}
