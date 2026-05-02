/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.output.Output;

@ExtendWith(MockitoExtension.class)
class JettyCallbacksTest {

  @Mock Response response;
  @Mock Callback delegateCallback;
  @Mock Output output;

  @Test
  void testByteBufferArrayCallback_SingleBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    ByteBuffer[] buffers = {buffer};

    JettyCallbacks.ByteBufferArrayCallback cb =
        JettyCallbacks.fromByteBufferArray(response, delegateCallback, buffers);

    assertNotNull(cb);

    // With a single buffer, it should immediately write with "last = true" and use the delegate
    // callback
    cb.send();
    verify(response).write(eq(true), same(buffer), same(delegateCallback));
  }

  @Test
  void testByteBufferArrayCallback_MultipleBuffers() {
    ByteBuffer buffer1 = ByteBuffer.allocate(10);
    ByteBuffer buffer2 = ByteBuffer.allocate(20);
    ByteBuffer[] buffers = {buffer1, buffer2};

    JettyCallbacks.ByteBufferArrayCallback cb =
        JettyCallbacks.fromByteBufferArray(response, delegateCallback, buffers);

    // Initial send should process the first buffer with "last = false" and use itself as the
    // callback
    cb.send();
    verify(response).write(eq(false), same(buffer1), same(cb));

    // Simulating Jetty calling succeeded() on the callback after the first write completes
    cb.succeeded();

    // It should now process the final buffer with "last = true" and use the delegate callback
    verify(response).write(eq(true), same(buffer2), same(delegateCallback));
  }

  @Test
  void testByteBufferArrayCallback_Failed() {
    ByteBuffer[] buffers = {ByteBuffer.allocate(10)};
    JettyCallbacks.ByteBufferArrayCallback cb =
        JettyCallbacks.fromByteBufferArray(response, delegateCallback, buffers);

    Throwable exception = new RuntimeException("Write failed");
    cb.failed(exception);

    verify(delegateCallback).failed(same(exception));
  }

  @Test
  void testOutputCallback_EmptyOutput() {
    when(output.iterator()).thenReturn(Collections.emptyIterator());

    JettyCallbacks.OutputCallback cb =
        JettyCallbacks.fromOutput(response, delegateCallback, output);
    assertNotNull(cb);

    // An empty output should immediately send a null buffer to trigger completion
    boolean closeOnLast = true;
    cb.send(closeOnLast);

    verify(response).write(eq(true), isNull(), same(delegateCallback));
  }

  @Test
  void testOutputCallback_SingleBuffer() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    when(output.iterator()).thenReturn(Collections.singletonList(buffer).iterator());

    JettyCallbacks.OutputCallback cb =
        JettyCallbacks.fromOutput(response, delegateCallback, output);

    boolean closeOnLast = true;
    cb.send(closeOnLast);

    // It should peek ahead, see no more elements, and write with the delegate callback
    verify(response).write(eq(true), same(buffer), same(delegateCallback));
  }

  @Test
  void testOutputCallback_MultipleBuffers() {
    ByteBuffer buffer1 = ByteBuffer.allocate(10);
    ByteBuffer buffer2 = ByteBuffer.allocate(20);
    when(output.iterator()).thenReturn(Arrays.asList(buffer1, buffer2).iterator());

    JettyCallbacks.OutputCallback cb =
        JettyCallbacks.fromOutput(response, delegateCallback, output);

    boolean closeOnLast = false;

    // First send
    cb.send(closeOnLast);
    verify(response).write(eq(false), same(buffer1), same(cb));

    // Jetty signals success on the first buffer, triggering the next send cycle
    cb.succeeded();
    verify(response).write(eq(false), same(buffer2), same(delegateCallback));
  }

  @Test
  void testOutputCallback_Failed() {
    when(output.iterator()).thenReturn(Collections.emptyIterator());
    JettyCallbacks.OutputCallback cb =
        JettyCallbacks.fromOutput(response, delegateCallback, output);

    Throwable exception = new RuntimeException("Chunk failed");
    cb.failed(exception);

    verify(delegateCallback).failed(same(exception));
  }

  @Test
  void testUtilityClassInstantiation() {
    // Included to achieve strictly 100% line coverage for JaCoCo on implicit default constructors
    // inside static utility classes.
    JettyCallbacks instance = new JettyCallbacks();
    assertNotNull(instance);
  }
}
