/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

public class LoggingServiceTest {

  private String originalJoobyDir;
  private String originalUserDir;
  private String originalLogback;
  private String originalLog4j;

  @BeforeEach
  public void setUp() {
    originalJoobyDir = System.getProperty("jooby.dir");
    originalUserDir = System.getProperty("user.dir");
    originalLogback = System.getProperty("logback.configurationFile");
    originalLog4j = System.getProperty("log4j.configurationFile");

    System.clearProperty("jooby.dir");
    System.clearProperty("logback.configurationFile");
    System.clearProperty("log4j.configurationFile");
  }

  @AfterEach
  public void tearDown() {
    restore("jooby.dir", originalJoobyDir);
    restore("user.dir", originalUserDir);
    restore("logback.configurationFile", originalLogback);
    restore("log4j.configurationFile", originalLog4j);
  }

  private void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  @Test
  public void isBinaryPath() {
    assertFalse(LoggingService.isBinary(Paths.get("src", "main", "java")));
    assertTrue(LoggingService.isBinary(Paths.get("target")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "target")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "build")));
    assertTrue(LoggingService.isBinary(Paths.get("project", "bin")));
  }

  @Test
  public void configureExplicitProperty() {
    System.setProperty("logback.configurationFile", "custom.xml");
    assertEquals(
        "custom.xml", LoggingService.configure(getClass().getClassLoader(), List.of("dev")));

    System.clearProperty("logback.configurationFile");
    System.setProperty("log4j.configurationFile", "custom4j.xml");
    assertEquals(
        "custom4j.xml", LoggingService.configure(getClass().getClassLoader(), List.of("dev")));
  }

  @Test
  public void configureNoServiceLoader() {
    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      when(loader.findFirst()).thenReturn(Optional.empty());

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      assertNull(LoggingService.configure(getClass().getClassLoader(), List.of("dev")));
    }
  }

  @Test
  public void configureNoBaseDir() {
    System.clearProperty("jooby.dir");
    System.clearProperty("user.dir");

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      when(loader.findFirst()).thenReturn(Optional.of(loggingService));
      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> LoggingService.configure(getClass().getClassLoader(), List.of("dev")));

      assertEquals("No base directory found", ex.getMessage());
    }
  }

  @Test
  public void configureNoLogFiles(@TempDir Path tempDir) {
    System.setProperty("jooby.dir", tempDir.toString());

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      // Empty log file list to trigger early exit
      when(loggingService.getLogFileName()).thenReturn(Collections.emptyList());
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));
      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      assertNull(LoggingService.configure(getClass().getClassLoader(), List.of("dev")));
    }
  }

  @Test
  public void configureFileSystemFound(@TempDir Path tempDir) throws Exception {
    System.setProperty("jooby.dir", tempDir.toString());
    Path confDir = tempDir.resolve("conf");
    Files.createDirectories(confDir);
    Path logFile = confDir.resolve("logback.dev.xml");
    Files.writeString(logFile, "<configuration></configuration>");

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      when(loggingService.getLogFileName()).thenReturn(List.of("logback.xml"));
      when(loggingService.getPropertyName()).thenReturn("logback.configurationFile");
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      String result = LoggingService.configure(getClass().getClassLoader(), List.of("dev"));

      assertEquals(logFile.toAbsolutePath().toString(), result);
      assertEquals(
          logFile.toAbsolutePath().toString(), System.getProperty("logback.configurationFile"));
    }
  }

  @Test
  public void configureFileSystemFoundInBaseDir(@TempDir Path tempDir) throws Exception {
    System.setProperty("jooby.dir", tempDir.toString());
    Path logFile = tempDir.resolve("logback.xml");
    Files.writeString(logFile, "<configuration></configuration>");

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      when(loggingService.getLogFileName()).thenReturn(List.of("logback.xml"));
      when(loggingService.getPropertyName()).thenReturn("logback.configurationFile");
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      String result = LoggingService.configure(getClass().getClassLoader(), List.of("dev"));
      assertEquals(logFile.toAbsolutePath().toString(), result);
    }
  }

  @Test
  public void configureFileSystemFoundButBinarySkipped(@TempDir Path tempDir) throws Exception {
    Path targetDir = tempDir.resolve("target");
    Files.createDirectories(targetDir);
    System.setProperty("jooby.dir", targetDir.toString());

    Path actualLog = targetDir.resolve("logback.xml");
    Files.writeString(actualLog, "<configuration></configuration>");

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      when(loggingService.getLogFileName()).thenReturn(List.of("logback.xml"));
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      ClassLoader mockClassLoader = mock(ClassLoader.class);
      when(mockClassLoader.getResource(anyString())).thenReturn(null);

      // Skips the binary directory file, attempts classpath fallback, returns null
      assertNull(LoggingService.configure(mockClassLoader, List.of("dev")));
    }
  }

  @Test
  public void configureClasspathFound(@TempDir Path tempDir) throws Exception {
    System.setProperty("jooby.dir", tempDir.toString());

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      when(loggingService.getLogFileName()).thenReturn(List.of("logback.xml"));
      when(loggingService.getPropertyName()).thenReturn("logback.configurationFile");
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      ClassLoader mockClassLoader = mock(ClassLoader.class);
      URL mockUrl = new URL("file:///mock/logback.dev.xml");
      when(mockClassLoader.getResource("logback.dev.xml")).thenReturn(mockUrl);

      String result = LoggingService.configure(mockClassLoader, List.of("dev"));

      assertEquals(mockUrl.toString(), result);

      assertEquals("logback.dev.xml", System.getProperty("logback.configurationFile"));
    }
  }

  @Test
  public void configureNothingFound(@TempDir Path tempDir) {
    System.setProperty("jooby.dir", tempDir.toString());

    try (MockedStatic<ServiceLoader> serviceLoaderMock = mockStatic(ServiceLoader.class)) {
      ServiceLoader<LoggingService> loader = mock(ServiceLoader.class);
      LoggingService loggingService = mock(LoggingService.class);

      // Providing multiple names evaluates the loop safely
      when(loggingService.getLogFileName()).thenReturn(List.of("logback.xml", "log4j.xml"));
      when(loader.findFirst()).thenReturn(Optional.of(loggingService));

      serviceLoaderMock
          .when(() -> ServiceLoader.load(eq(LoggingService.class), any(ClassLoader.class)))
          .thenReturn(loader);

      ClassLoader mockClassLoader = mock(ClassLoader.class);
      when(mockClassLoader.getResource(anyString())).thenReturn(null);

      assertNull(LoggingService.configure(mockClassLoader, List.of("dev")));
    }
  }
}
