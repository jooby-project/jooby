/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JoobyRunAppOverloadsTest {

  private MockedStatic<Jooby> joobyMock;
  private MockedStatic<Server> serverMock;

  private Server defaultServer;
  private Server customServer;

  private String[] args;
  private TestSupplier supplier;
  private TestConsumer consumer;
  private Supplier<Jooby> consumerSupplier;

  // Concrete classes to ensure .getClass().getPackage() doesn't throw a NullPointerException
  static class TestSupplier implements Supplier<Jooby> {
    @Override
    public Jooby get() {
      return null;
    }
  }

  static class TestConsumer implements Consumer<Jooby> {
    @Override
    public void accept(Jooby jooby) {}
  }

  @BeforeEach
  @SuppressWarnings({"unchecked"})
  void setUp() {
    // Use CALLS_REAL_METHODS so the overloads execute their actual logic
    joobyMock = Mockito.mockStatic(Jooby.class, Mockito.CALLS_REAL_METHODS);
    serverMock = Mockito.mockStatic(Server.class);

    defaultServer = Mockito.mock(Server.class);
    customServer = Mockito.mock(Server.class);
    args = new String[] {"test-arg"};
    supplier = new TestSupplier();
    consumer = new TestConsumer();
    consumerSupplier = Mockito.mock(Supplier.class);

    // Stub the ServiceLoader default server mapping
    serverMock.when(Server::loadServer).thenReturn(defaultServer);

    // Stub utility methods triggered inside the overloads
    joobyMock.when(() -> Jooby.configurePackage(any(Package.class))).thenAnswer(inv -> null);
    joobyMock.when(() -> Jooby.consumerProvider(any(Consumer.class))).thenReturn(consumerSupplier);

    // Intercept the final base method to prevent actual execution/startup
    joobyMock
        .when(
            () ->
                Jooby.runApp(
                    any(String[].class),
                    any(Server.class),
                    any(ExecutionMode.class),
                    any(List.class)))
        .thenAnswer(inv -> null);
  }

  @AfterEach
  void tearDown() {
    joobyMock.close();
    serverMock.close();
  }

  @Test
  @DisplayName("Test: runApp(args, Supplier)")
  void testRunApp_Args_Supplier() {
    Jooby.runApp(args, supplier);
    verifyBaseRunApp(defaultServer, ExecutionMode.DEFAULT, List.of(supplier));
  }

  @Test
  @DisplayName("Test: runApp(args, Server, Supplier)")
  void testRunApp_Args_Server_Supplier() {
    Jooby.runApp(args, customServer, supplier);
    verifyBaseRunApp(customServer, ExecutionMode.DEFAULT, List.of(supplier));
    joobyMock.verify(() -> Jooby.configurePackage(supplier.getClass().getPackage()));
  }

  @Test
  @DisplayName("Test: runApp(args, Consumer)")
  void testRunApp_Args_Consumer() {
    Jooby.runApp(args, consumer);
    verifyBaseRunApp(defaultServer, ExecutionMode.DEFAULT, List.of(consumerSupplier));
    joobyMock.verify(() -> Jooby.configurePackage(consumer.getClass().getPackage()));
  }

  @Test
  @DisplayName("Test: runApp(args, Server, Consumer)")
  void testRunApp_Args_Server_Consumer() {
    Jooby.runApp(args, customServer, consumer);
    verifyBaseRunApp(customServer, ExecutionMode.DEFAULT, List.of(consumerSupplier));
  }

  @Test
  @DisplayName("Test: runApp(args, Server, ExecutionMode, Consumer)")
  void testRunApp_Args_Server_ExecutionMode_Consumer() {
    Jooby.runApp(args, customServer, ExecutionMode.WORKER, consumer);
    verifyBaseRunApp(customServer, ExecutionMode.WORKER, List.of(consumerSupplier));
    joobyMock.verify(() -> Jooby.configurePackage(consumer.getClass().getPackage()));
  }

  @Test
  @DisplayName("Test: runApp(args, ExecutionMode, Consumer)")
  void testRunApp_Args_ExecutionMode_Consumer() {
    Jooby.runApp(args, ExecutionMode.WORKER, consumer);
    verifyBaseRunApp(defaultServer, ExecutionMode.WORKER, List.of(consumerSupplier));
    joobyMock.verify(() -> Jooby.configurePackage(consumer.getClass().getPackage()));
  }

  @Test
  @DisplayName("Test: runApp(args, ExecutionMode, Supplier)")
  void testRunApp_Args_ExecutionMode_Supplier() {
    Jooby.runApp(args, ExecutionMode.WORKER, supplier);
    verifyBaseRunApp(defaultServer, ExecutionMode.WORKER, List.of(supplier));
  }

  @Test
  @DisplayName("Test: runApp(args, Server, ExecutionMode, Supplier)")
  void testRunApp_Args_Server_ExecutionMode_Supplier() {
    Jooby.runApp(args, customServer, ExecutionMode.WORKER, supplier);
    verifyBaseRunApp(customServer, ExecutionMode.WORKER, List.of(supplier));
    joobyMock.verify(() -> Jooby.configurePackage(supplier.getClass().getPackage()));
  }

  @Test
  @DisplayName("Test: runApp(args, List<Supplier>)")
  void testRunApp_Args_ListSupplier() {
    Jooby.runApp(args, List.of(supplier));
    verifyBaseRunApp(defaultServer, ExecutionMode.DEFAULT, List.of(supplier));
  }

  @Test
  @DisplayName("Test: runApp(args, ExecutionMode, List<Supplier>)")
  void testRunApp_Args_ExecutionMode_ListSupplier() {
    Jooby.runApp(args, ExecutionMode.WORKER, List.of(supplier));
    verifyBaseRunApp(defaultServer, ExecutionMode.WORKER, List.of(supplier));
  }

  @Test
  @DisplayName("Test: runApp(args, Server, List<Supplier>)")
  void testRunApp_Args_Server_ListSupplier() {
    Jooby.runApp(args, customServer, List.of(supplier));
    verifyBaseRunApp(customServer, ExecutionMode.DEFAULT, List.of(supplier));
  }

  /** Helper to verify the terminal base method was called exactly as expected. */
  private void verifyBaseRunApp(
      Server expectedServer, ExecutionMode expectedMode, List<Supplier<Jooby>> expectedList) {
    joobyMock.verify(
        () -> Jooby.runApp(eq(args), eq(expectedServer), eq(expectedMode), eq(expectedList)));
  }
}
