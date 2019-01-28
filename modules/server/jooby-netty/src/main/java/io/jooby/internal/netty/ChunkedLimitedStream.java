/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.io.InputStream;

public class ChunkedLimitedStream implements ChunkedInput<ByteBuf> {

  private final InputStream in;
  private final int chunkSize;
  private final long length;
  private long offset;
  private boolean closed;

  /**
   * Creates a new instance that fetches data from the specified stream.
   *
   * @param chunkSize the number of bytes to fetch on each
   *                  {@link #readChunk(ChannelHandlerContext)} call
   */
  public ChunkedLimitedStream(InputStream in, int chunkSize, long length) {
    this.in = in;
    this.chunkSize = chunkSize;
    this.length = length;
  }

  @Override
  public boolean isEndOfInput() {
    if (closed) {
      return true;
    }
    return offset >= length;
  }

  @Override
  public void close() throws Exception {
    closed = true;
    in.close();
  }

  @Deprecated
  @Override
  public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
    return readChunk(ctx.alloc());
  }

  @Override
  public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
    if (isEndOfInput()) {
      return null;
    }

    long next = offset + this.chunkSize;
    int chunkSize = next < length ? this.chunkSize : (int) (length - offset);

    boolean release = true;
    ByteBuf buffer = allocator.buffer(chunkSize);
    try {
      // transfer to buffer
      offset += buffer.writeBytes(in, chunkSize);
      release = false;
      return buffer;
    } finally {
      if (release) {
        buffer.release();
      }
    }
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public long progress() {
    return offset;
  }
}
