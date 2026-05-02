/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Context;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpServerSession;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AbstractMcpTransportProviderTest {

  @Mock McpJsonMapper jsonMapper;
  @Mock McpTransportContextExtractor<Context> contextExtractor;
  @Mock McpServerSession session1;
  @Mock McpServerSession session2;
  @Mock Logger mockLogger;

  private AbstractMcpTransportProvider provider;

  @BeforeEach
  void setup() throws Exception {
    // Create a concrete instance of the abstract class for testing
    provider =
        new AbstractMcpTransportProvider(jsonMapper, contextExtractor) {
          @Override
          protected String transportName() {
            return "test-transport";
          }
        };

    // Inject the mock SLF4J Logger using reflection to verify logging branches
    Field logField = AbstractMcpTransportProvider.class.getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(provider, mockLogger);
  }

  @Test
  void testSetSessionFactory() throws Exception {
    McpServerSession.Factory factory = mock(McpServerSession.Factory.class);
    provider.setSessionFactory(factory);

    Field factoryField = AbstractMcpTransportProvider.class.getDeclaredField("sessionFactory");
    factoryField.setAccessible(true);
    assertEquals(factory, factoryField.get(provider));
  }

  // --- NOTIFY CLIENTS TESTS ---

  @Test
  void testNotifyClients_EmptySessions() {
    provider.notifyClients("method", "params").block();

    verify(mockLogger).debug("No active {} sessions to broadcast a message to", "test-transport");
    verify(mockLogger, never())
        .debug("Attempting to broadcast to {} active {} sessions", 0, "test-transport");
  }

  @Test
  void testNotifyClients_Populated_DebugEnabled() {
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    provider.sessions.put("sess-1", session1);
    when(session1.sendNotification("method", "params")).thenReturn(Mono.empty());

    provider.notifyClients("method", "params").block();

    // Verify debug branch was entered
    verify(mockLogger)
        .debug("Attempting to broadcast to {} active {} sessions", 1, "test-transport");
    verify(session1).sendNotification("method", "params");
  }

  @Test
  void testNotifyClients_Populated_DebugDisabled() {
    when(mockLogger.isDebugEnabled()).thenReturn(false);
    provider.sessions.put("sess-1", session1);
    when(session1.sendNotification("method", "params")).thenReturn(Mono.empty());

    provider.notifyClients("method", "params").block();

    // Verify debug branch was skipped
    verify(mockLogger, never())
        .debug("Attempting to broadcast to {} active {} sessions", 1, "test-transport");
    verify(session1).sendNotification("method", "params");
  }

  @Test
  void testNotifyClients_HandlesNotificationErrorGracefully() {
    // We don't care about debug mode for this test
    when(mockLogger.isDebugEnabled()).thenReturn(false);

    provider.sessions.put("sess-1", session1);
    when(session1.getId()).thenReturn("sess-1-id");

    // Simulate an error occurring during the send operation
    RuntimeException simulatedError = new RuntimeException("Simulated I/O failure");
    when(session1.sendNotification("method", "params")).thenReturn(Mono.error(simulatedError));

    // The onErrorComplete() should swallow the error, so block() will not throw an exception
    provider.notifyClients("method", "params").block();

    // Verify the doOnError block correctly logged the failure
    verify(mockLogger)
        .error(
            "Failed to send a message to {} session {}: {}",
            "test-transport",
            "sess-1-id",
            "Simulated I/O failure");
  }

  // --- CLOSE GRACEFULLY TESTS ---

  @Test
  void testCloseGracefully_DebugEnabled() {
    when(mockLogger.isDebugEnabled()).thenReturn(true);

    provider.sessions.put("sess-1", session1);
    provider.sessions.put("sess-2", session2);

    when(session1.closeGracefully()).thenReturn(Mono.empty());
    when(session2.closeGracefully()).thenReturn(Mono.empty());

    assertFalse(provider.isClosing.get()); // Initially false

    provider.closeGracefully().block();

    // Verify doFirst actions
    assertTrue(provider.isClosing.get());
    verify(mockLogger)
        .debug("Initiating graceful shutdown for {} {} sessions", 2, "test-transport");

    // Verify flatMap actions
    verify(session1).closeGracefully();
    verify(session2).closeGracefully();

    // Verify doFinally actions
    assertTrue(provider.sessions.isEmpty());
  }

  @Test
  void testCloseGracefully_DebugDisabled() {
    when(mockLogger.isDebugEnabled()).thenReturn(false);

    provider.sessions.put("sess-1", session1);
    when(session1.closeGracefully()).thenReturn(Mono.empty());

    provider.closeGracefully().block();

    // Verify the debug logging branch was bypassed
    verify(mockLogger, never())
        .debug("Initiating graceful shutdown for {} {} sessions", 1, "test-transport");

    // Verify state still updated properly
    assertTrue(provider.isClosing.get());
    assertTrue(provider.sessions.isEmpty());
    verify(session1).closeGracefully();
  }
}
