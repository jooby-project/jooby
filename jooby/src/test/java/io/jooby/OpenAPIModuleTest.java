/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;

import io.jooby.handler.Asset;
import io.jooby.handler.AssetSource;

public class OpenAPIModuleTest {

  @Test
  public void formatEnumResolution() {
    assertEquals(OpenAPIModule.Format.JSON, OpenAPIModule.Format.from("openapi.json"));
    assertEquals(OpenAPIModule.Format.YAML, OpenAPIModule.Format.from("custom-file.yaml"));
    assertThrows(IllegalArgumentException.class, () -> OpenAPIModule.Format.from("file.xml"));
  }

  @Test
  public void installDefaultPathsNoUI() throws Exception {
    Jooby app = mock(Jooby.class);
    ClassLoader classLoader = mock(ClassLoader.class);

    when(app.getClassLoader()).thenReturn(classLoader);
    when(app.getBasePackage()).thenReturn("com.example");
    // Simulate UI tools NOT being on the classpath
    when(classLoader.getResource(any(String.class))).thenReturn(null);

    OpenAPIModule module = new OpenAPIModule();
    module.install(app);

    // Verifies OpenAPI JSON and YAML endpoints are exposed using defaults
    // Note: The second argument correctly expects a leading slash matching Jooby's path
    // normalization
    verify(app).assets(eq("/openapi.json"), eq("/com/example/openapi.json"));
    verify(app).assets(eq("/openapi.yaml"), eq("/com/example/openapi.yaml"));
  }

  @Test
  public void installCustomFileAndCustomContextPath() throws Exception {
    Jooby app = mock(Jooby.class);
    ClassLoader classLoader = mock(ClassLoader.class);

    when(app.getClassLoader()).thenReturn(classLoader);
    when(classLoader.getResource(any(String.class))).thenReturn(null);

    OpenAPIModule module =
        new OpenAPIModule("/docs")
            .contextPath("/api-v1")
            .file("my-custom-api.yaml")
            .format(OpenAPIModule.Format.YAML);

    module.install(app);

    // Verifies the custom path mappings
    verify(app).assets(eq("/docs/openapi.yaml"), eq("my-custom-api.yaml"));
  }

  @Test
  public void uiDisabledWhenJsonNotSupported() throws Exception {
    Jooby app = mock(Jooby.class);
    ClassLoader classLoader = mock(ClassLoader.class);
    Logger logger = mock(Logger.class);

    when(app.getClassLoader()).thenReturn(classLoader);
    when(app.getLog()).thenReturn(logger);

    // Simulate Swagger UI being on the classpath
    URL dummyUrl = new URL("http://dummy");
    when(classLoader.getResource("swagger-ui/index.html")).thenReturn(dummyUrl);

    // Initialize module with ONLY Yaml enabled
    OpenAPIModule module = new OpenAPIModule().format(OpenAPIModule.Format.YAML);

    module.install(app);

    // Verify UI is skipped because JSON is required for Swagger UI
    verify(logger).debug("{} is disabled when json format is not supported", "swagger-ui");
    verify(app, never())
        .assets(startsWith("/swagger"), any(AssetSource.class), any(AssetSource.class));
  }

  @Test
  public void installWithUI() throws Exception {
    Jooby app = mock(Jooby.class);
    ClassLoader classLoader = mock(ClassLoader.class);
    Logger logger = mock(Logger.class);

    when(app.getClassLoader()).thenReturn(classLoader);
    when(app.getLog()).thenReturn(logger);
    when(app.getContextPath()).thenReturn("/");
    when(app.getBasePackage()).thenReturn("com.example");

    // Simulate both Swagger UI and ReDoc on the classpath
    URL dummyUrl = new URL("http://dummy");
    when(classLoader.getResource("swagger-ui/index.html")).thenReturn(dummyUrl);
    when(classLoader.getResource("redoc/index.html")).thenReturn(dummyUrl);

    // Mock internal assets that get read during processAsset()
    AssetSource uiSource = mock(AssetSource.class);
    Asset indexAsset = mock(Asset.class);
    String fakeHtml = "<html>url: '${openAPIPath}' redoc: '${redocPath}'</html>";
    when(indexAsset.stream())
        .thenReturn(new ByteArrayInputStream(fakeHtml.getBytes(StandardCharsets.UTF_8)));
    when(indexAsset.getLastModified()).thenReturn(1000L);
    when(uiSource.resolve("index.html")).thenReturn(indexAsset);

    Asset swaggerJsAsset = mock(Asset.class);
    String fakeJs = "const url = '${openAPIPath}'";
    when(swaggerJsAsset.stream())
        .thenReturn(new ByteArrayInputStream(fakeJs.getBytes(StandardCharsets.UTF_8)));
    when(swaggerJsAsset.getLastModified()).thenReturn(1000L);
    when(uiSource.resolve("swagger-initializer.js")).thenReturn(swaggerJsAsset);

    // Intercept static AssetSource.create call
    try (MockedStatic<AssetSource> mockedStatic = mockStatic(AssetSource.class)) {
      mockedStatic.when(() -> AssetSource.create(classLoader, "swagger-ui")).thenReturn(uiSource);
      mockedStatic.when(() -> AssetSource.create(classLoader, "redoc")).thenReturn(uiSource);

      OpenAPIModule module = new OpenAPIModule().swaggerUI("/api-docs").redoc("/api-redoc");

      module.install(app);

      // Verify routing configuration
      ArgumentCaptor<AssetSource> sourceCaptor = ArgumentCaptor.forClass(AssetSource.class);

      // Check ReDoc
      verify(app).assets(eq("/api-redoc/?*"), sourceCaptor.capture(), eq(uiSource));
      AssetSource redocSource = sourceCaptor.getValue();
      Asset redocIndex = redocSource.resolve("index.html");
      assertNotNull(redocIndex);
      assertEquals(MediaType.html, redocIndex.getContentType());

      // Validate dynamic string replacement worked
      String redocContent = new String(readAllBytes(redocIndex.stream()), StandardCharsets.UTF_8);
      assertTrue(redocContent.contains("url: '/openapi.json'"));
      assertTrue(redocContent.contains("redoc: '/api-redoc'"));

      // Check Swagger UI
      verify(app).assets(eq("/api-docs/?*"), sourceCaptor.capture(), eq(uiSource));
      AssetSource swaggerSource = sourceCaptor.getValue();

      Asset swaggerJs = swaggerSource.resolve("swagger-initializer.js");
      assertNotNull(swaggerJs);
      String jsContent = new String(readAllBytes(swaggerJs.stream()), StandardCharsets.UTF_8);
      assertTrue(jsContent.contains("const url = '/openapi.json'"));
    }
  }

  // Helper method for Java 8 compatibility (can use input.readAllBytes() directly in Java 9+)
  private byte[] readAllBytes(InputStream input) throws Exception {
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[1024];
    while ((nRead = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }
}
