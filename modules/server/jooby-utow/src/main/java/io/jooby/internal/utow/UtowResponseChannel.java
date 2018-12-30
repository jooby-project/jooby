/**
 *                                  Apache License
 *                            Version 2.0, January 2004
 *                         http://www.apache.org/licenses/LICENSE-2.0
 *                         Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import org.xnio.channels.StreamSinkChannel;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class UtowResponseChannel implements WritableByteChannel, Closeable {

  private StreamSinkChannel channel;

  public UtowResponseChannel(StreamSinkChannel channel) {
    this.channel = channel;
  }

  @Override public int write(ByteBuffer src) throws IOException {
    return channel.write(src);
  }

  @Override public boolean isOpen() {
    return channel.isOpen();
  }

  @Override public void close() throws IOException {
    if (channel != null) {
      try {
        channel.shutdownWrites();
        channel.flush();
      } finally {
        channel.close();
      }
      channel = null;
    }
  }
}
