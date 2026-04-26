/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.jooby.Context;

public class ContextInitializerListTest {

  @Test
  public void testInitializerFlow() {
    Context ctx = mock(Context.class);
    AtomicInteger counter = new AtomicInteger(0);

    // 1. Test Constructor and Apply
    ContextInitializer first = c -> counter.incrementAndGet();
    ContextInitializerList list = new ContextInitializerList(first);

    list.apply(ctx);
    assertEquals(1, counter.get(), "First initializer should have run");

    // 2. Test Add (New)
    ContextInitializer second = c -> counter.addAndGet(10);
    list.add(second);

    list.apply(ctx);
    // counter was 1. Now runs first (+1) and second (+10) = 12
    assertEquals(12, counter.get());

    // 3. Test Add Duplicate (should be ignored by !initializers.contains check)
    list.add(first);
    list.apply(ctx);
    // counter was 12. Runs first (+1) and second (+10) = 23.
    // If duplicate was added, it would be 24.
    assertEquals(23, counter.get());

    // 4. Test Remove
    list.remove(second);
    list.apply(ctx);
    // counter was 23. Runs only first (+1) = 24.
    assertEquals(24, counter.get());
  }

  @Test
  public void testChainAdd() {
    ContextInitializer first = mock(ContextInitializer.class);
    ContextInitializer second = mock(ContextInitializer.class);
    ContextInitializerList list = new ContextInitializerList(first);

    // Verify the method returns 'this' for chaining
    ContextInitializer result = list.add(second);
    assertEquals(list, result);
  }
}
