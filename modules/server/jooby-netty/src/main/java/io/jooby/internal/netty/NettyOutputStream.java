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
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.OutputStream;

public class NettyOutputStream extends OutputStream {
  private final ByteBuf buffer;

  private final ChannelHandlerContext ctx;

  private HttpResponse headers;

  public NettyOutputStream(ChannelHandlerContext ctx, HttpResponse rsp, int chunksize) {
    this.buffer = ctx.alloc().buffer(0, chunksize);
    this.ctx = ctx;
    this.headers = rsp;
  }

  @Override
  public void write(int b) {
    if (buffer.maxWritableBytes() < 1) {
      write(false);
    }
    buffer.writeByte(b);
  }

  @Override
  public void close() {
    try {
      write(true);
    } finally {
      if (buffer.refCnt() > 0) {
        buffer.release();
      }
    }
  }

  @Override
  public void write(byte[] bytes, int off, int len) {
    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;
    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(bytes, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      write(false);
    }
    if (dataLengthLeftToWrite > 0) {
      buffer.writeBytes(bytes, dataToWriteOffset, dataLengthLeftToWrite);
    }
  }

  private void write(boolean last) {
    int chunkSize = buffer.readableBytes();
    if (chunkSize > 0) {
      if (headers != null) {
        HttpHeaders h = headers.headers();
        if (last) {
          h.remove(HttpHeaderNames.TRANSFER_ENCODING);
          h.set(HttpHeaderNames.CONTENT_LENGTH, chunkSize);
        } else if (!h.contains(HttpHeaderNames.CONTENT_LENGTH)) {
          h.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        }
        ctx.write(headers, ctx.voidPromise());
        this.headers = null;
      }
      DefaultHttpContent chunk = new DefaultHttpContent(buffer.copy());
      if (last) {
        ctx.write(chunk, ctx.voidPromise());
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, ctx.voidPromise());
      } else {
        ctx.writeAndFlush(chunk, ctx.voidPromise());
      }
      buffer.clear();
    }
  }
}
