/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CorsTest {

  @Test
  @DisplayName("Verify default initialization values")
  void testDefaultConstructor() {
    var cors = new Cors();

    assertTrue(cors.anyOrigin());
    assertTrue(cors.allowOrigin("http://jooby.io"));
    assertTrue(cors.getUseCredentials());
    assertEquals(List.of("GET", "POST"), cors.getMethods());
    assertEquals(
        List.of("X-Requested-With", "Content-Type", "Accept", "Origin"), cors.getHeaders());
    assertEquals(Duration.ofMinutes(30), cors.getMaxAge());
    assertTrue(cors.getExposedHeaders().isEmpty());
  }

  @Test
  @DisplayName("Verify origin matching with wildcards, regex, and exact strings")
  void testOriginMatching() {
    var cors = new Cors().setOrigin("http://*.jooby.io", "http://localhost");

    assertFalse(cors.anyOrigin());
    assertEquals(List.of("http://*.jooby.io", "http://localhost"), cors.getOrigin());

    assertTrue(cors.allowOrigin("http://api.jooby.io"));
    assertTrue(cors.allowOrigin("http://localhost"));
    assertFalse(cors.allowOrigin("http://external.com"));
  }

  @Test
  @DisplayName("Verify method and header access lists with allMatch and firstMatch algorithms")
  void testMethodsAndHeaders() {
    var cors = new Cors().setMethods("PUT", "DELETE").setHeaders("X-Custom-A", "X-Custom-B");

    assertTrue(cors.allowMethod("PUT"));
    assertFalse(cors.allowMethod("GET"));

    assertFalse(cors.anyHeader());
    assertTrue(cors.allowHeader("X-Custom-A"));
    assertTrue(cors.allowHeader("X-Custom-A", "X-Custom-B"));
    // allMatch requires ALL provided headers to match
    assertFalse(cors.allowHeader("X-Custom-A", "X-Invalid"));
  }

  @Test
  @DisplayName("Verify exposed headers, max age and credentials setters")
  void testAdditionalProperties() {
    var cors =
        new Cors()
            .setExposedHeaders("X-Exposed")
            .setMaxAge(Duration.ofHours(1))
            .setUseCredentials(false);

    assertEquals(List.of("X-Exposed"), cors.getExposedHeaders());
    assertEquals(Duration.ofHours(1), cors.getMaxAge());
    assertFalse(cors.getUseCredentials());
  }

  @Test
  @DisplayName("Verify Config parsing with full overrides and singleton lists")
  void testFromConfigFull() {
    var rootConf = mock(Config.class);
    var corsConf = mock(Config.class);

    when(rootConf.hasPath("cors")).thenReturn(true);
    when(rootConf.getConfig("cors")).thenReturn(corsConf);

    when(corsConf.hasPath("origin")).thenReturn(true);
    when(corsConf.getAnyRef("origin")).thenReturn(List.of("http://test.com"));

    when(corsConf.hasPath("credentials")).thenReturn(true);
    when(corsConf.getBoolean("credentials")).thenReturn(false);

    when(corsConf.hasPath("methods")).thenReturn(true);
    // Supplying a single string forces the `list(Object)` fallback to Collections.singletonList
    when(corsConf.getAnyRef("methods")).thenReturn("PATCH");

    when(corsConf.hasPath("headers")).thenReturn(true);
    when(corsConf.getAnyRef("headers")).thenReturn(List.of("X-Req"));

    when(corsConf.hasPath("maxAge")).thenReturn(true);
    when(corsConf.getDuration("maxAge", TimeUnit.SECONDS)).thenReturn(3600L);

    when(corsConf.hasPath("exposedHeaders")).thenReturn(true);
    // Supplying a single string for fallback coverage
    when(corsConf.getAnyRef("exposedHeaders")).thenReturn("X-Exp");

    var cors = Cors.from(rootConf);

    assertEquals(List.of("http://test.com"), cors.getOrigin());
    assertFalse(cors.getUseCredentials());
    assertEquals(List.of("PATCH"), cors.getMethods());
    assertEquals(List.of("X-Req"), cors.getHeaders());
    assertEquals(Duration.ofSeconds(3600), cors.getMaxAge());
    assertEquals(List.of("X-Exp"), cors.getExposedHeaders());
  }

  @Test
  @DisplayName("Verify Config parsing with absent paths yields default configuration")
  void testFromConfigEmpty() {
    var rootConf = mock(Config.class);

    // Bypasses "cors" sub-path and relies on empty root
    when(rootConf.hasPath("cors")).thenReturn(false);
    when(rootConf.hasPath("origin")).thenReturn(false);
    when(rootConf.hasPath("credentials")).thenReturn(false);
    when(rootConf.hasPath("methods")).thenReturn(false);
    when(rootConf.hasPath("headers")).thenReturn(false);
    when(rootConf.hasPath("maxAge")).thenReturn(false);
    when(rootConf.hasPath("exposedHeaders")).thenReturn(false);

    var cors = Cors.from(rootConf);

    assertTrue(cors.anyOrigin());
    assertTrue(cors.getUseCredentials());
    assertEquals(List.of("GET", "POST"), cors.getMethods());
  }

  @Test
  @DisplayName("Verify NullPointerExceptions on invalid initializations")
  void testNullValidations() {
    var cors = new Cors();
    assertThrows(NullPointerException.class, () -> cors.setOrigin((List<String>) null));
    assertThrows(NullPointerException.class, () -> cors.setExposedHeaders((List<String>) null));
  }

  @Test
  @DisplayName("Verify internal Matcher toString execution via Reflection")
  void testMatcherToString() throws Exception {
    var cors = new Cors();
    Field originField = Cors.class.getDeclaredField("origin");
    originField.setAccessible(true);

    Object matcherInstance = originField.get(cors);
    assertEquals("[*]", matcherInstance.toString());
  }

  @Test
  public void defaults() {
    cors(
        cors -> {
          assertEquals(true, cors.anyOrigin());
          assertEquals(Arrays.asList("*"), cors.getOrigin());
          assertEquals(true, cors.getUseCredentials());

          assertEquals(true, cors.allowMethod("get"));
          assertEquals(true, cors.allowMethod("post"));
          assertEquals(Arrays.asList("GET", "POST"), cors.getMethods());

          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.allowHeader("Content-Type"));
          assertEquals(true, cors.allowHeader("Accept"));
          assertEquals(true, cors.allowHeader("Origin"));
          assertEquals(
              true, cors.allowHeader("X-Requested-With", "Content-Type", "Accept", "Origin"));
          assertEquals(
              Arrays.asList("X-Requested-With", "Content-Type", "Accept", "Origin"),
              cors.getHeaders());

          assertEquals(Duration.ofMinutes(30), cors.getMaxAge());

          assertEquals(Arrays.asList(), cors.getExposedHeaders());

          assertEquals(false, cors.setUseCredentials(false).getUseCredentials());
        });
  }

  @Test
  public void origin() {
    cors(
        baseconf().withValue("origin", fromAnyRef("*")),
        cors -> {
          assertEquals(true, cors.anyOrigin());
          assertEquals(true, cors.allowOrigin("http://foo.com"));
        });

    cors(
        baseconf().withValue("origin", fromAnyRef("http://*.com")),
        cors -> {
          assertEquals(false, cors.anyOrigin());
          assertEquals(true, cors.allowOrigin("http://foo.com"));
          assertEquals(true, cors.allowOrigin("http://bar.com"));
        });

    cors(
        baseconf().withValue("origin", fromAnyRef("http://foo.com")),
        cors -> {
          assertEquals(false, cors.anyOrigin());
          assertEquals(true, cors.allowOrigin("http://foo.com"));
          assertEquals(false, cors.allowOrigin("http://bar.com"));
        });
  }

  @Test
  public void allowedMethods() {
    cors(
        baseconf().withValue("methods", fromAnyRef("GET")),
        cors -> {
          assertEquals(true, cors.allowMethod("GET"));
          assertEquals(true, cors.allowMethod("get"));
          assertEquals(false, cors.allowMethod("POST"));
        });

    cors(
        baseconf().withValue("methods", fromAnyRef(asList("get", "post"))),
        cors -> {
          assertEquals(true, cors.allowMethod("GET"));
          assertEquals(true, cors.allowMethod("get"));
          assertEquals(true, cors.allowMethod("POST"));
        });
  }

  @Test
  public void requestHeaders() {
    cors(
        baseconf().withValue("headers", fromAnyRef("*")),
        cors -> {
          assertEquals(true, cors.anyHeader());
          assertEquals(true, cors.allowHeader("Custom-Header"));
        });

    cors(
        baseconf().withValue("headers", fromAnyRef(asList("X-Requested-With", "*"))),
        cors -> {
          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.anyHeader());
        });

    cors(
        baseconf()
            .withValue(
                "headers",
                fromAnyRef(asList("X-Requested-With", "Content-Type", "Accept", "Origin"))),
        cors -> {
          assertEquals(false, cors.anyHeader());
          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.allowHeader("Content-Type"));
          assertEquals(true, cors.allowHeader("Accept"));
          assertEquals(true, cors.allowHeader("Origin"));
          assertEquals(
              true,
              cors.allowHeaders(asList("X-Requested-With", "Content-Type", "Accept", "Origin")));
          assertEquals(
              false, cors.allowHeaders(asList("X-Requested-With", "Content-Type", "Custom")));
        });
  }

  private void cors(final Config conf, final Consumer<Cors> callback) {
    callback.accept(Cors.from(conf));
  }

  private void cors(final Consumer<Cors> callback) {
    callback.accept(new Cors());
  }

  private Config baseconf() {
    return ConfigFactory.empty()
        .withValue("credentials", fromAnyRef(true))
        .withValue("maxAge", fromAnyRef("30m"))
        .withValue("origin", fromAnyRef(Lists.newArrayList()))
        .withValue("exposedHeaders", fromAnyRef(Lists.newArrayList("X")))
        .withValue("methods", fromAnyRef(Lists.newArrayList()))
        .withValue("headers", fromAnyRef(Lists.newArrayList()));
  }
}
