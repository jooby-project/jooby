/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import io.jooby.annotation.htmx.HxTrigger;

public class HxTriggerTest {

  @Test
  void makeHappyEnumCoverage() {
    assertTrue(EnumSet.allOf(HxTrigger.Phase.class).contains(HxTrigger.Phase.TRIGGER));
    assertTrue(EnumSet.allOf(HxTrigger.Phase.class).contains(HxTrigger.Phase.AFTER_SETTLE));
    assertTrue(EnumSet.allOf(HxTrigger.Phase.class).contains(HxTrigger.Phase.AFTER_SWAP));
  }
}
