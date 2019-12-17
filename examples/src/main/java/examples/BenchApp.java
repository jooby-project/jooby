/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BenchApp extends Jooby {

  private static final String MESSAGE = "Hello, World!";

  private static final byte[] MESSAGE_BYTE = MESSAGE.getBytes(StandardCharsets.UTF_8);

  private static final ByteBuffer MESSAGE_BUFFER = ByteBuffer.allocateDirect(MESSAGE.length());

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  static {
    try {
      MESSAGE_BUFFER.put(MESSAGE.getBytes("US-ASCII")).flip();
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  {
    get("/gc", ctx -> {
      long before = Runtime.getRuntime().freeMemory();
      System.gc();
      long after = Runtime.getRuntime().freeMemory();
      return before + ":" + after + "(" + (before - after) + ")";
    });
    get("/plaintext", ctx -> {
      return ctx.send(MESSAGE_BYTE);
    });

    get("/", ctx -> {
      System.out.println(ctx.getRequestPath());
      return ctx.send(MESSAGE_BYTE);
    });

    get("/json", ctx -> Thread.currentThread().getName());

    get("/fortune", ctx -> Thread.currentThread().getName());
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, BenchApp::new);
  }
}
