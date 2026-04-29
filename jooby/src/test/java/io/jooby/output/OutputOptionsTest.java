/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class OutputOptionsTest {

  @Test
  @DisplayName("Verify defaults() dynamically applies the correct branch for the host JVM memory")
  void testDefaultsUsingJVMMemory() {
    OutputOptions options = OutputOptions.defaults();
    assertNotNull(options);

    // Dynamically verify the constructor hit the correct branch based on the actual test runner's
    // JVM
    long maxMemory = Runtime.getRuntime().maxMemory();

    if (maxMemory < 64 * 1024 * 1024) {
      assertFalse(options.isDirectBuffers());
      assertEquals(512, options.getSize());
    } else if (maxMemory < 128 * 1024 * 1024) {
      assertTrue(options.isDirectBuffers());
      assertEquals(1024, options.getSize());
    } else if (maxMemory < 512 * 1024 * 1024) {
      assertTrue(options.isDirectBuffers());
      assertEquals(4096, options.getSize());
    } else {
      assertTrue(options.isDirectBuffers());
      assertEquals(1024 * 16 - 20, options.getSize());
    }
  }

  @Test
  @DisplayName("Verify defaults")
  void testDefaults() {
    var size = new int[] {512, 1024, 4096, 1024 * 16 - 20};
    var directBuffer = new boolean[] {false, true, true, true};
    var memory =
        new int[] {64 * 1024 * 1024, 128 * 1024 * 1024, 512 * 1024 * 1024, 1024 * 1024 * 1024};

    for (int i = 0; i < size.length; i++) {
      var options = new OutputOptions(memory[i] - 1);
      assertEquals(size[i], options.getSize());
      assertEquals(directBuffer[i], options.isDirectBuffers());
    }
  }

  @Test
  @DisplayName("Verify small() factory method sets explicit limits")
  void testSmall() {
    OutputOptions options = OutputOptions.small();

    assertFalse(options.isDirectBuffers());
    assertEquals(512, options.getSize());
  }

  @Test
  @DisplayName("Verify getters and setters support chaining")
  void testGettersAndSetters() {
    OutputOptions options = new OutputOptions();

    OutputOptions returned = options.setSize(8192).setDirectBuffers(false);

    assertSame(options, returned); // Validates "return this;" for chaining
    assertEquals(8192, options.getSize());
    assertFalse(options.isDirectBuffers());
  }

  @Test
  @DisplayName("Verify toString format")
  void testToString() {
    OutputOptions options = new OutputOptions().setSize(2048).setDirectBuffers(true);

    assertEquals("{size: 2048, direct: true}", options.toString());
  }
}
