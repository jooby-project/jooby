package org.jooby.internal.undertow;

import static org.xnio.Bits.allAreClear;
import static org.xnio.Bits.anyAreClear;
import static org.xnio.Bits.anyAreSet;
import io.undertow.UndertowMessages;
import io.undertow.io.BufferWritableOutputStream;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.xnio.Buffers;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

/**
 * Tmp class to add a resetBuffer method, see https://issues.jboss.org/browse/UNDERTOW-361
 */
public class TmpOutputStream extends OutputStream implements BufferWritableOutputStream {

  private final HttpServerExchange exchange;
  private ByteBuffer buffer;
  private Pooled<ByteBuffer> pooledBuffer;
  private StreamSinkChannel channel;
  private int state;
  private int written;
  private final long contentLength;

  private static final int FLAG_CLOSED = 1;
  private static final int FLAG_WRITE_STARTED = 1 << 1;

  private static final int MAX_BUFFERS_TO_ALLOCATE = 10;

  /**
   * Construct a new instance. No write timeout is configured.
   *
   * @param exchange The exchange
   */
  public TmpOutputStream(final HttpServerExchange exchange) {
    this.exchange = exchange;
    this.contentLength = exchange.getResponseContentLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final int b) throws IOException {
    write(new byte[]{(byte) b }, 0, 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (len < 1) {
      return;
    }
    if (anyAreSet(state, FLAG_CLOSED)) {
      throw UndertowMessages.MESSAGES.streamIsClosed();
    }
    // if this is the last of the content
    ByteBuffer buffer = buffer();
    if (len == contentLength - written || buffer.remaining() < len) {
      if (buffer.remaining() < len) {

        // so what we have will not fit.
        // We allocate multiple buffers up to MAX_BUFFERS_TO_ALLOCATE
        // and put it in them
        // if it still dopes not fit we loop, re-using these buffers

        StreamSinkChannel channel = this.channel;
        if (channel == null) {
          this.channel = channel = exchange.getResponseChannel();
        }
        final Pool<ByteBuffer> bufferPool = exchange.getConnection().getBufferPool();
        ByteBuffer[] buffers = new ByteBuffer[MAX_BUFFERS_TO_ALLOCATE + 1];
        Pooled[] pooledBuffers = new Pooled[MAX_BUFFERS_TO_ALLOCATE];
        try {
          buffers[0] = buffer;
          int bytesWritten = 0;
          int rem = buffer.remaining();
          buffer.put(b, bytesWritten + off, rem);
          buffer.flip();
          bytesWritten += rem;
          int bufferCount = 1;
          for (int i = 0; i < MAX_BUFFERS_TO_ALLOCATE; ++i) {
            Pooled<ByteBuffer> pooled = bufferPool.allocate();
            pooledBuffers[bufferCount - 1] = pooled;
            buffers[bufferCount++] = pooled.getResource();
            ByteBuffer cb = pooled.getResource();
            int toWrite = len - bytesWritten;
            if (toWrite > cb.remaining()) {
              rem = cb.remaining();
              cb.put(b, bytesWritten + off, rem);
              cb.flip();
              bytesWritten += rem;
            } else {
              cb.put(b, bytesWritten + off, len - bytesWritten);
              bytesWritten = len;
              cb.flip();
              break;
            }
          }
          Channels.writeBlocking(channel, buffers, 0, bufferCount);
          while (bytesWritten < len) {
            // ok, it did not fit, loop and loop and loop until it is done
            bufferCount = 0;
            for (int i = 0; i < MAX_BUFFERS_TO_ALLOCATE + 1; ++i) {
              ByteBuffer cb = buffers[i];
              cb.clear();
              bufferCount++;
              int toWrite = len - bytesWritten;
              if (toWrite > cb.remaining()) {
                rem = cb.remaining();
                cb.put(b, bytesWritten + off, rem);
                cb.flip();
                bytesWritten += rem;
              } else {
                cb.put(b, bytesWritten + off, len - bytesWritten);
                bytesWritten = len;
                cb.flip();
                break;
              }
            }
            Channels.writeBlocking(channel, buffers, 0, bufferCount);
          }
          buffer.clear();
        } finally {
          for (Pooled p : pooledBuffers) {
            if (p == null) {
              break;
            }
            p.free();
          }
        }
      } else {
        buffer.put(b, off, len);
        if (buffer.remaining() == 0) {
          writeBufferBlocking(false);
        }
      }
    } else {
      buffer.put(b, off, len);
      if (buffer.remaining() == 0) {
        writeBufferBlocking(false);
      }
    }
    updateWritten(len);
  }

  @Override
  public void write(final ByteBuffer[] buffers) throws IOException {
    if (anyAreSet(state, FLAG_CLOSED)) {
      throw UndertowMessages.MESSAGES.streamIsClosed();
    }
    int len = 0;
    for (ByteBuffer buf : buffers) {
      len += buf.remaining();
    }
    if (len < 1) {
      return;
    }

    // if we have received the exact amount of content write it out in one go
    // this is a common case when writing directly from a buffer cache.
    if (this.written == 0 && len == contentLength) {
      if (channel == null) {
        channel = exchange.getResponseChannel();
      }
      Channels.writeBlocking(channel, buffers, 0, buffers.length);
      state |= FLAG_WRITE_STARTED;
    } else {
      ByteBuffer buffer = buffer();
      if (len < buffer.remaining()) {
        Buffers.copy(buffer, buffers, 0, buffers.length);
      } else {
        if (channel == null) {
          channel = exchange.getResponseChannel();
        }
        if (buffer.position() == 0) {
          Channels.writeBlocking(channel, buffers, 0, buffers.length);
        } else {
          final ByteBuffer[] newBuffers = new ByteBuffer[buffers.length + 1];
          buffer.flip();
          newBuffers[0] = buffer;
          System.arraycopy(buffers, 0, newBuffers, 1, buffers.length);
          Channels.writeBlocking(channel, newBuffers, 0, newBuffers.length);
          buffer.clear();
        }
        state |= FLAG_WRITE_STARTED;
      }
    }
    updateWritten(len);
  }

  @Override
  public void write(final ByteBuffer byteBuffer) throws IOException {
    write(new ByteBuffer[]{byteBuffer });
  }

  public void resetBuffer() {
    if (allAreClear(state, FLAG_WRITE_STARTED)) {
        if (pooledBuffer != null) {
            pooledBuffer.free();
            pooledBuffer = null;
        }
        buffer = null;
    } else {
        throw UndertowMessages.MESSAGES.responseAlreadyStarted();
    }
}

  void updateWritten(final long len) throws IOException {
    this.written += len;
    if (contentLength != -1 && this.written >= contentLength) {
      flush();
      close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException {
    if (anyAreSet(state, FLAG_CLOSED)) {
      throw UndertowMessages.MESSAGES.streamIsClosed();
    }
    if (buffer != null && buffer.position() != 0) {
      writeBufferBlocking(false);
    }
    if (channel == null) {
      channel = exchange.getResponseChannel();
    }
    Channels.flushBlocking(channel);
  }

  private void writeBufferBlocking(final boolean writeFinal) throws IOException {
    if (channel == null) {
      channel = exchange.getResponseChannel();
    }
    buffer.flip();

    while (buffer.hasRemaining()) {
      if (writeFinal) {
        channel.writeFinal(buffer);
      } else {
        channel.write(buffer);
      }
      if (buffer.hasRemaining()) {
        channel.awaitWritable();
      }
    }
    buffer.clear();
    state |= FLAG_WRITE_STARTED;
  }

  @Override
  public void transferFrom(final FileChannel source) throws IOException {
    if (anyAreSet(state, FLAG_CLOSED)) {
      throw UndertowMessages.MESSAGES.streamIsClosed();
    }
    if (buffer != null && buffer.position() != 0) {
      writeBufferBlocking(false);
    }
    if (channel == null) {
      channel = exchange.getResponseChannel();
    }
    long position = source.position();
    long size = source.size();
    Channels.transferBlocking(channel, source, position, size);
    updateWritten(size - position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    if (anyAreSet(state, FLAG_CLOSED)) {
      return;
    }
    try {
      state |= FLAG_CLOSED;
      if (anyAreClear(state, FLAG_WRITE_STARTED) && channel == null) {
        if (buffer == null) {
          exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "0");
        } else {
          exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, "" + buffer.position());
        }
      }
      if (buffer != null) {
        writeBufferBlocking(true);
      }
      if (channel == null) {
        channel = exchange.getResponseChannel();
      }
      if (channel == null) {
        return;
      }
      StreamSinkChannel channel = this.channel;
      channel.shutdownWrites();
      Channels.flushBlocking(channel);
    } finally {
      if (pooledBuffer != null) {
        pooledBuffer.free();
        buffer = null;
      } else {
        buffer = null;
      }
    }
  }

  private ByteBuffer buffer() {
    ByteBuffer buffer = this.buffer;
    if (buffer != null) {
      return buffer;
    }
    this.pooledBuffer = exchange.getConnection().getBufferPool().allocate();
    this.buffer = pooledBuffer.getResource();
    return this.buffer;
  }

}
