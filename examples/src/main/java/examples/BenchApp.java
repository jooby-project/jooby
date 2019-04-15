/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
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
    get("/plaintext", ctx -> {
      return ctx.send(MESSAGE_BYTE);
    });

    get("/", ctx -> {
      System.out.println(ctx.pathString());
      return ctx.send(MESSAGE_BYTE);
    });

    get("/json", ctx -> Thread.currentThread().getName());

    get("/fortune", ctx -> Thread.currentThread().getName());
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, BenchApp::new);
  }
}
