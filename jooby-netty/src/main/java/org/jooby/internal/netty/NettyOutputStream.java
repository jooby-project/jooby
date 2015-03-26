/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class to help application that are built to write to an
 * OutputStream to chunk the content
 *
 * <pre>
 * {@code
 * DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
 * HttpHeaders.setTransferEncodingChunked(response);
 * response.headers().set(CONTENT_TYPE, "application/octet-stream");
 * // other headers
 * ctx.write(response);
 * // code of the application that use the ChunkOutputStream
 * // Don't forget to close the ChunkOutputStream after use!
 * ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
 * }
 * </pre>
 *
 * @author tbussier
 */
class NettyOutputStream extends OutputStream {

  private NettyResponse rsp;

  private final ByteBuf buffer;

  private final ChannelHandlerContext ctx;

  private int chunkState;

  private boolean keepAlive;

  private HttpHeaders headers;

  NettyOutputStream(final NettyResponse rsp, final ChannelHandlerContext ctx, final ByteBuf buffer,
      final boolean keepAlive, final HttpHeaders headers) {
    this.rsp = rsp;
    this.buffer = buffer;
    this.ctx = ctx;
    this.keepAlive = keepAlive;
    this.headers = headers;
  }

  @Override
  public void write(final int b) throws IOException {
    write(new byte[b], 0, 1);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void reset() {
    chunkState = 0;
    buffer.clear();
  }

  @Override
  public void close() throws IOException {
    try {
      flush();
    } finally {
      if (chunkState > 0) {
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
            .addListener(FIRE_EXCEPTION_ON_FAILURE);
        /**
         * no Keep alive?
         */
        if (!keepAlive || !headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
          future.addListener(ChannelFutureListener.CLOSE);
        }
      }
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    int dataLengthLeftToWrite = len;
    int dataToWriteOffset = off;
    int spaceLeftInCurrentChunk;
    while ((spaceLeftInCurrentChunk = buffer.maxWritableBytes()) < dataLengthLeftToWrite) {
      buffer.writeBytes(b, dataToWriteOffset, spaceLeftInCurrentChunk);
      dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
      dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
      if (dataLengthLeftToWrite > 0) {
        chunkState += 1;
        flush();
      }
    }
    if (dataLengthLeftToWrite > 0) {
      buffer.writeBytes(b, dataToWriteOffset, dataLengthLeftToWrite);
    }
  }

  @Override
  public void flush() throws IOException {
    /**
     * Case 0; response size is small than current buffer size (we don't need to write chunks).
     */
    if (chunkState == 0) {
      /**
       * Mambo jambo around, content-length, transfer-encoding and keep alive.
       */
      DefaultHttpResponse rsp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, this.rsp.status,
          buffer);
      String len = headers.get(HttpHeaders.Names.CONTENT_LENGTH);
      /**
       * Is Content-Length present? When not we check for transfer-encoding, if transfer-encoding
       * was set, we do nothing. Otherwise, we set the Content-Length bc we know it.
       */
      if (len == null) {
        // remove transfer encoding
        headers.remove(HttpHeaders.Names.TRANSFER_ENCODING);
        // override len
        len = Integer.toString(buffer.readableBytes());
        headers.set(HttpHeaders.Names.CONTENT_LENGTH, len);
      }
      /**
       * Keep alive can be set IF and ONLY IF, content-lenght is present.
       */
      if (keepAlive && len != null) {
        // send keep alive and don't close the channel
        headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        rsp.headers().set(headers);
        ctx.writeAndFlush(rsp).addListener(FIRE_EXCEPTION_ON_FAILURE);
      } else {
        // don't send keep alive and close the channel.
        rsp.headers().set(headers);
        ctx.writeAndFlush(rsp).addListener(FIRE_EXCEPTION_ON_FAILURE);
      }
      return;
    }
    /**
     * Case 1; we need to send chunks and check if content-length was set or not.
     */
    if (chunkState == 1) {
      /**
       * Set headers, if Content-Length wasn't set force/set Transfer-Encoding
       */
      DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, this.rsp.status);
      String len = headers.get(HttpHeaders.Names.CONTENT_LENGTH);
      if (len == null) {
        headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
      }
      /**
       * Keep alive if len was set
       */
      if (keepAlive && len != null) {
        headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      // dump headers
      rsp.headers().set(headers);
      // send headers
      ctx.write(rsp).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }
    /**
     * Write chunk and clear the buffer.
     */
    ctx.writeAndFlush(new DefaultHttpContent(buffer.copy()))
        .addListener(FIRE_EXCEPTION_ON_FAILURE);
    buffer.clear();
  }

}
