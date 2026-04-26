/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.pgclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.internal.vertx.sqlclient.VertxThreadLocalSqlConnection;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgNotice;
import io.vertx.pgclient.PgNotification;
import io.vertx.sqlclient.PrepareOptions;
import io.vertx.sqlclient.spi.DatabaseMetadata;

public class VertxPgConnectionProxyTest {

  private MockedStatic<VertxThreadLocalSqlConnection> threadLocalMock;
  private PgConnection delegate;
  private VertxPgConnectionProxy proxy;
  private final String dbName = "pgdb";

  @BeforeEach
  void setUp() {
    threadLocalMock = mockStatic(VertxThreadLocalSqlConnection.class);
    delegate = mock(PgConnection.class);
    proxy = new VertxPgConnectionProxy(dbName);

    // Ensure get(name) returns our mock delegate
    threadLocalMock.when(() -> VertxThreadLocalSqlConnection.get(dbName)).thenReturn(delegate);
  }

  @AfterEach
  void tearDown() {
    threadLocalMock.close();
  }

  @Test
  void testDelegatedMethods() {
    // Handlers
    // NOTE: The proxy returns exactly what the delegate returns.
    // Since the delegate is a mock, we verify it returns the delegate instance.
    Handler<PgNotification> notifyH = h -> {};
    when(delegate.notificationHandler(notifyH)).thenReturn(delegate);
    assertEquals(delegate, proxy.notificationHandler(notifyH));

    Handler<PgNotice> noticeH = h -> {};
    when(delegate.noticeHandler(noticeH)).thenReturn(delegate);
    assertEquals(delegate, proxy.noticeHandler(noticeH));

    Handler<Throwable> excH = h -> {};
    when(delegate.exceptionHandler(excH)).thenReturn(delegate);
    assertEquals(delegate, proxy.exceptionHandler(excH));

    Handler<Void> closeH = h -> {};
    when(delegate.closeHandler(closeH)).thenReturn(delegate);
    assertEquals(delegate, proxy.closeHandler(closeH));

    // Futures & Metadata
    when(delegate.cancelRequest()).thenReturn(Future.succeededFuture());
    assertNotNull(proxy.cancelRequest());

    when(delegate.processId()).thenReturn(123);
    assertEquals(123, proxy.processId());

    when(delegate.secretKey()).thenReturn(456);
    assertEquals(456, proxy.secretKey());

    when(delegate.isSSL()).thenReturn(true);
    assertTrue(proxy.isSSL());

    DatabaseMetadata metadata = mock(DatabaseMetadata.class);
    when(delegate.databaseMetadata()).thenReturn(metadata);
    assertEquals(metadata, proxy.databaseMetadata());

    // Queries & Transactions
    when(delegate.begin()).thenReturn(Future.succeededFuture());
    assertNotNull(proxy.begin());

    when(delegate.transaction()).thenReturn(null);
    assertNull(proxy.transaction());

    when(delegate.close()).thenReturn(Future.succeededFuture());
    assertNotNull(proxy.close());
  }

  @Test
  void testPrepareAndQuery() {
    PrepareOptions options = new PrepareOptions();

    proxy.prepare("sql");
    verify(delegate).prepare("sql");

    proxy.prepare("sql", options);
    verify(delegate).prepare("sql", options);

    proxy.query("sql");
    verify(delegate).query("sql");

    proxy.preparedQuery("sql");
    verify(delegate).preparedQuery("sql");

    proxy.preparedQuery("sql", options);
    verify(delegate).preparedQuery("sql", options);
  }

  @Test
  void testRecordIdentity() {
    assertEquals(dbName, proxy.name());
    VertxPgConnectionProxy other = new VertxPgConnectionProxy(dbName);
    assertEquals(proxy, other);
    assertEquals(proxy.hashCode(), other.hashCode());
  }
}
