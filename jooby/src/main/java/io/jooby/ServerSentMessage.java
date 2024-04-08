/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.function.IntPredicate;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.DataBuffer;

/**
 * Server-Sent message.
 *
 * @author edgar
 * @since 2.5.0
 */
public class ServerSentMessage {
  private static final byte SEPARATOR = '\n';

  private static final byte[] ID = "id:".getBytes(UTF_8);

  private static final byte[] EVENT = "event:".getBytes(UTF_8);

  private static final byte[] RETRY = "retry:".getBytes(UTF_8);

  private static final byte[] DATA = "data:".getBytes(UTF_8);

  private Object id;

  private String event;

  private final Object data;

  private Long retry;

  /**
   * Creates a new message.
   *
   * @param data Data. Must not be <code>null</code>.
   */
  public ServerSentMessage(@NonNull Object data) {
    this.data = data;
  }

  /**
   * The event ID to set the EventSource object's last event ID value.
   *
   * @return The event ID to set the EventSource object's last event ID value.
   */
  public @Nullable Object getId() {
    return id;
  }

  /**
   * Set The event ID to set the EventSource object's last event ID value.
   *
   * @param id Event ID. Converted to String.
   * @return This message.
   */
  public @NonNull ServerSentMessage setId(@Nullable Object id) {
    this.id = id == null ? null : id.toString();
    return this;
  }

  /**
   * A string identifying the type of event described. If this is specified, an event will be
   * dispatched on the browser to the listener for the specified event name; the website source code
   * should use addEventListener() to listen for named events. The onmessage handler is called if no
   * event name is specified for a message.
   *
   * @return Event type.
   */
  public @Nullable String getEvent() {
    return event;
  }

  /**
   * Set event type.
   *
   * @param event Event type.
   * @return This message.
   */
  public @NonNull ServerSentMessage setEvent(@Nullable String event) {
    this.event = event;
    return this;
  }

  /**
   * The data field for the message. When the EventSource receives multiple consecutive lines that
   * begin with data:, it will concatenate them, inserting a newline character between each one.
   * Trailing newlines are removed.
   *
   * @return Data.
   */
  public @NonNull Object getData() {
    return data;
  }

  /**
   * The reconnection time to use when attempting to send the event. This must be an integer,
   * specifying the reconnection time in milliseconds. If a non-integer value is specified, the
   * field is ignored.
   *
   * @return Retry option.
   */
  public @Nullable Long getRetry() {
    return retry;
  }

  /**
   * Set retry option.
   *
   * @param retry Retry option.
   * @return This message.
   */
  public @NonNull ServerSentMessage setRetry(@Nullable Long retry) {
    this.retry = retry;
    return this;
  }

  /**
   * Encode message as event source stream format.
   *
   * @param ctx Web context. To encode complex objects.
   * @return Encoded data.
   */
  public @NonNull DataBuffer toByteArray(@NonNull Context ctx) {
    try {
      Route route = ctx.getRoute();
      MessageEncoder encoder = route.getEncoder();
      var bufferFactory = ctx.getBufferFactory();
      var buffer = bufferFactory.allocateBuffer();
      var message = encoder.encode(ctx, data);

      if (id != null) {
        buffer.write(ID);
        buffer.write(id.toString().getBytes(UTF_8));
        buffer.write(SEPARATOR);
      }
      if (event != null) {
        buffer.write(EVENT);
        buffer.write(event.getBytes(UTF_8));
        buffer.write(SEPARATOR);
      }
      if (retry != null) {
        buffer.write(RETRY);
        buffer.write(retry.toString().getBytes(UTF_8));
        buffer.write(SEPARATOR);
      }
      /* do multi-line processing: */
      buffer.write(DATA);
      IntPredicate nl = ch -> ch == '\n';
      var i = message.indexOf(nl, 0);
      while (i > 0) {
        buffer.write(message.split(i + 1));
        if (message.readableByteCount() > 0) {
          buffer.write(DATA);
        }
        i = message.indexOf(nl, 1);
      }
      // write any pending bytes
      buffer.write(message);
      buffer.write(SEPARATOR);
      buffer.write(SEPARATOR);
      return buffer;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
