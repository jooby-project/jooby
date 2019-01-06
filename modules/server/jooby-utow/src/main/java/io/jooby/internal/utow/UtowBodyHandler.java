package io.jooby.internal.utow;

import io.jooby.Body;
import io.jooby.Err;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class UtowBodyHandler implements Receiver.FullBytesCallback, Receiver.PartialBytesCallback {

  private final int bufferSize;
  private final long maxRequestSize;
  private Router.Match route;
  private UtowContext context;
  private long chunkSize;
  private List chunks;
  private File file;
  private FileChannel channel;

  public UtowBodyHandler(Router.Match route, UtowContext context, int bufferSize, long maxRequestSize) {
    this.route = route;
    this.context = context;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
  }

  @Override public void handle(HttpServerExchange exchange, byte[] bytes) {
    context.body = Body.of(bytes);
    route.execute(context);
  }

  @Override public void handle(HttpServerExchange exchange, byte[] chunk, boolean last) {
    try {
      if (chunk.length > 0) {
        chunkSize += chunk.length;
        if (chunkSize > maxRequestSize) {
          try {
            context.sendError(new Err(StatusCode.REQUEST_ENTITY_TOO_LARGE));
          } finally {
            closeChannel();
            channel = null;
            file = null;
          }
          return;
        }
        if (chunkSize <= bufferSize) {
          if (chunks == null) {
            chunks = new ArrayList<>();
          }
          chunks.add(chunk);
        } else {
          // overflow
          if (file == null) {
            file = context.router().tmpdir().resolve(System.currentTimeMillis() + ".tmp").toFile();
            FileOutputStream fos = new FileOutputStream(file);
            channel = fos.getChannel();
          }
          if (chunks != null) {
            List source = chunks;
            chunks = null;
            for (Object s : source) {
              channel.write(ByteBuffer.wrap((byte[]) s));
            }
            source.clear();
          }
          channel.write(ByteBuffer.wrap(chunk));
        }
      }
      if (last) {
        if (channel != null) {
          forceAndClose();
          channel = null;
          context.body = Body.of(file);
        } else {
          context.body = Body.of(bytes((int) chunkSize));
        }
        route.execute(context);
      }
    } catch (IOException x) {
      try {
        context.sendError(x);
      } finally {
        closeChannel();
        channel = null;
        file = null;
        exchange.endExchange();
      }
    }
  }

  private void closeChannel() {
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException x) {
        // close didn't succeed
      }
    }
  }

  private void forceAndClose() throws IOException {
    if (channel != null) {
      try {
        channel.force(false);
      } finally {
        closeChannel();
      }
    }
  }

  private byte[] bytes(int size) {
    byte[] bytes = new byte[size];
    int offset = 0;
    for (Object c : chunks) {
      byte[] chunk = (byte[]) c;
      System.arraycopy(chunk, 0, bytes, offset, chunk.length);
      offset += chunk.length;
    }
    return bytes;
  }
}
