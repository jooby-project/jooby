/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.internal.MutedServer;

class JoobyRunHookTest {

  private static final String PROPERTY_NAME = "___jooby_run_hook__";

  private MockedStatic<MutedServer> mutedServerMock;
  private Server mockServer;
  private Server mockMutedServer;

  @BeforeEach
  void setUp() {
    mockServer = mock(Server.class);
    mockMutedServer = mock(Server.class);

    // Mock the static MutedServer.mute() call
    mutedServerMock = mockStatic(MutedServer.class);
    mutedServerMock.when(() -> MutedServer.mute(mockServer)).thenReturn(mockMutedServer);

    // Ensure property is clean before each test
    System.clearProperty(PROPERTY_NAME);

    // Reset our test hook state
    TestHook.capturedServer = null;
  }

  @AfterEach
  void tearDown() {
    mutedServerMock.close();
    System.clearProperty(PROPERTY_NAME);
  }

  // --- Helper class to act as the valid hook ---
  public static class TestHook implements Consumer<Server> {
    public static Server capturedServer;

    public TestHook() {} // Must have a public no-arg constructor

    @Override
    public void accept(Server server) {
      capturedServer = server;
    }
  }

  @Test
  @DisplayName("Branch 1: ClassLoader is NOT ModuleClassLoader")
  void testNotModuleClassLoader() {
    ClassLoader standardLoader = new ClassLoader() {};
    System.setProperty(PROPERTY_NAME, "SomeClass");

    Jooby.joobyRunHook(standardLoader, mockServer);

    // Verification: The if-block is skipped, so the property is NEVER cleared
    assertEquals("SomeClass", System.getProperty(PROPERTY_NAME));
  }

  @Test
  @DisplayName("Branch 2: ModuleClassLoader, but property is null")
  void testModuleClassLoader_NullProperty() {
    ClassLoader jbossLoader = new org.jboss.modules.ModuleClassLoader();
    // Property is already null via setUp()

    Jooby.joobyRunHook(jbossLoader, mockServer);

    // Verification: The property gets actively set to empty string
    assertEquals("", System.getProperty(PROPERTY_NAME));
    assertNull(TestHook.capturedServer); // Hook logic skipped
  }

  @Test
  @DisplayName("Branch 3: ModuleClassLoader, but property is empty")
  void testModuleClassLoader_EmptyProperty() {
    ClassLoader jbossLoader = new org.jboss.modules.ModuleClassLoader();
    System.setProperty(PROPERTY_NAME, "");

    Jooby.joobyRunHook(jbossLoader, mockServer);

    // Verification: Property remains empty, hook logic skipped
    assertEquals("", System.getProperty(PROPERTY_NAME));
    assertNull(TestHook.capturedServer);
  }

  @Test
  @DisplayName("Branch 4: ModuleClassLoader and Valid Hook (Happy Path)")
  void testModuleClassLoader_ValidHook() {
    ClassLoader jbossLoader = new org.jboss.modules.ModuleClassLoader();
    System.setProperty(PROPERTY_NAME, TestHook.class.getName());

    Jooby.joobyRunHook(jbossLoader, mockServer);

    // Verification: Property cleared
    assertEquals("", System.getProperty(PROPERTY_NAME));

    // Verification: MutedServer.mute was called
    mutedServerMock.verify(() -> MutedServer.mute(mockServer));

    // Verification: The consumer was instantiated and accept() was called with the muted server
    assertEquals(mockMutedServer, TestHook.capturedServer);
  }

  @Test
  @DisplayName("Branch 5: ModuleClassLoader and Invalid Hook (Exception Path)")
  void testModuleClassLoader_InvalidHook() {
    ClassLoader jbossLoader = new org.jboss.modules.ModuleClassLoader();
    System.setProperty(PROPERTY_NAME, "com.example.DoesNotExist");

    // The try/catch will catch the ClassNotFoundException and wrap it in SneakyThrows.propagate
    // SneakyThrows.propagate usually throws a RuntimeException, so we expect an exception here.
    assertThrows(
        Exception.class,
        () -> {
          Jooby.joobyRunHook(jbossLoader, mockServer);
        });

    // Verification: Property was still cleared before the crash
    assertEquals("", System.getProperty(PROPERTY_NAME));
  }
}
