/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CacheControlTest {

  @Test
  @DisplayName("Verify default values from constructor and defaults() static method")
  void testDefaults() {
    // 1. Using standard constructor
    CacheControl cc1 = new CacheControl();
    assertTrue(cc1.isEtag());
    assertTrue(cc1.isLastModified());
    assertEquals(CacheControl.UNDEFINED, cc1.getMaxAge());

    // 2. Using static factory method
    CacheControl cc2 = CacheControl.defaults();
    assertTrue(cc2.isEtag());
    assertTrue(cc2.isLastModified());
    assertEquals(CacheControl.UNDEFINED, cc2.getMaxAge());
  }

  @Test
  @DisplayName("Verify fluent setters for ETag, LastModified, and MaxAge (long)")
  void testSetters() {
    CacheControl cc = new CacheControl().setETag(false).setLastModified(false).setMaxAge(3600L);

    assertFalse(cc.isEtag(), "ETag should be disabled");
    assertFalse(cc.isLastModified(), "LastModified should be disabled");
    assertEquals(3600L, cc.getMaxAge(), "MaxAge should be updated to 3600");
  }

  @Test
  @DisplayName("Verify setMaxAge correctly converts java.time.Duration to seconds")
  void testSetMaxAgeDuration() {
    CacheControl cc = new CacheControl().setMaxAge(Duration.ofHours(2));

    // 2 hours = 7200 seconds
    assertEquals(7200L, cc.getMaxAge());
  }

  @Test
  @DisplayName("Verify setNoCache() disables all caching parameters")
  void testSetNoCache() {
    CacheControl cc = new CacheControl().setNoCache();

    assertFalse(cc.isEtag());
    assertFalse(cc.isLastModified());
    assertEquals(CacheControl.NO_CACHE, cc.getMaxAge());
  }

  @Test
  @DisplayName("Verify static noCache() factory method disables all caching parameters")
  void testStaticNoCache() {
    CacheControl cc = CacheControl.noCache();

    assertFalse(cc.isEtag());
    assertFalse(cc.isLastModified());
    assertEquals(CacheControl.NO_CACHE, cc.getMaxAge());
  }
}
