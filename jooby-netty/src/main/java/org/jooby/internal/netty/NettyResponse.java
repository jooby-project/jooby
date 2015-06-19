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

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.Attribute;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

public class NettyResponse implements NativeResponse {

  private ChannelHandlerContext ctx;

  private boolean keepAlive;

  private HttpResponseStatus status;

  private HttpHeaders headers;

  private boolean committed;

  private int bufferSize;

  public NettyResponse(final ChannelHandlerContext ctx, final int bufferSize,
      final boolean keepAlive) {
    this.ctx = ctx;
    this.bufferSize = bufferSize;
    this.keepAlive = keepAlive;
    this.headers = new DefaultHttpHeaders();
    this.status = HttpResponseStatus.OK;
  }

  @Override
  public List<String> headers(final String name) {
    List<String> headers = this.headers.getAll(name);
    return headers == null ? Collections.emptyList() : ImmutableList.copyOf(headers);
  }

  @Override
  public Optional<String> header(final String name) {
    return Optional.ofNullable(this.headers.get(name));
  }

  @Override
  public void header(final String name, final String value) {
    headers.set(name, value);
  }

  @Override
  public void header(final String name, final Iterable<String> values) {
    headers.remove(name)
        .add(name, values);
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    send(Unpooled.wrappedBuffer(bytes));
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    send(Unpooled.wrappedBuffer(buffer));
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    byte[] chunk = new byte[bufferSize];
    int count = ByteStreams.read(stream, chunk, 0, bufferSize);
    if (count <= 0) {
      return;
    }
    ByteBuf buffer = Unpooled.wrappedBuffer(chunk, 0, count);
    if (count < bufferSize) {
      send(buffer);
    } else {
      DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);

      if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
        headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
      } else {
        if (keepAlive) {
          headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
      }

      // dump headers
      rsp.headers().set(headers);
      // send headers
      ctx.write(rsp).addListener(FIRE_EXCEPTION_ON_FAILURE);
      // send head chunk
      ctx.write(buffer).addListener(FIRE_EXCEPTION_ON_FAILURE);
      // send tail
      ctx.write(new ChunkedStream(stream, bufferSize)).addListener(FIRE_EXCEPTION_ON_FAILURE);
      keepAlive(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
    }

    committed = true;
  }

  @Override
  public void send(final FileChannel channel) throws Exception {
    DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);

    if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
      headers.remove(HttpHeaders.Names.TRANSFER_ENCODING);
      headers.set(HttpHeaders.Names.CONTENT_LENGTH, channel.size());
    }

    if (keepAlive) {
      headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // dump headers
    rsp.headers().set(headers);
    // send headers
    ctx.write(rsp).addListener(FIRE_EXCEPTION_ON_FAILURE);
    ctx.write(new DefaultFileRegion(channel, 0, channel.size()))
        .addListener(FIRE_EXCEPTION_ON_FAILURE);
    keepAlive(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));

    committed = true;
  }

  private void send(final ByteBuf buffer) throws Exception {
    DefaultFullHttpResponse rsp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buffer);

    if (!headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
      headers.remove(HttpHeaders.Names.TRANSFER_ENCODING)
          .set(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
    }

    if (keepAlive) {
      headers.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // dump headers
    rsp.headers().set(headers);
    keepAlive(ctx.writeAndFlush(rsp));

    committed = true;
  }

  private void keepAlive(final ChannelFuture future) {
    future.addListener(FIRE_EXCEPTION_ON_FAILURE);
    if (headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
      if (!keepAlive) {
        future.addListener(CLOSE);
      }
    } else {
      // content len is not set, just close the connection regardless keep alive or not.
      future.addListener(CLOSE);
    }
  }

  @Override
  public int statusCode() {
    return status.code();
  }

  @Override
  public void statusCode(final int code) {
    this.status = HttpResponseStatus.valueOf(code);
  }

  @Override
  public boolean committed() {
    return committed;
  }

  @Override
  public void end() {
    if (ctx != null) {
      Attribute<NettyWebSocket> ws = ctx.attr(NettyWebSocket.KEY);
      if (ws != null && ws.get() != null) {
        status = HttpResponseStatus.SWITCHING_PROTOCOLS;
        ws.get().hankshake();
        ctx = null;
        committed = true;
        return;
      }
      if (!committed) {
        DefaultHttpResponse rsp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        // dump headers
        rsp.headers().set(headers);
        keepAlive(ctx.writeAndFlush(rsp));
      }
      committed = true;
      ctx = null;
    }
  }

  @Override
  public void reset() {
    headers.clear();
    status = HttpResponseStatus.OK;
  }

}
