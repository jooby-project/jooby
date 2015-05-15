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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

import org.jooby.spi.HttpHandler;

import com.typesafe.config.Config;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {

  private EventExecutorGroup executor;

  private HttpHandler handler;

  private Config config;

  private int maxInitialLineLength;

  private int maxHeaderSize;

  private int maxChunkSize;

  private int maxContentLength;

  private long idleTimeOut;

  public NettyInitializer(final EventExecutorGroup executor, final HttpHandler handler,
      final Config config) {
    this.executor = executor;
    this.handler = handler;
    this.config = config;

    maxInitialLineLength = config.getBytes("netty.http.MaxInitialLineLength").intValue();
    maxHeaderSize = config.getBytes("netty.http.MaxHeaderSize").intValue();
    maxChunkSize = config.getBytes("netty.http.MaxChunkSize").intValue();
    maxContentLength = config.getBytes("netty.http.MaxContentLength").intValue();
    idleTimeOut = config.getDuration("netty.http.IdleTimeout", TimeUnit.MILLISECONDS);
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    ch.pipeline()
        .addLast(new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize))
        .addLast(new HttpObjectAggregator(maxContentLength))
        .addLast(new IdleStateHandler(0, 0, idleTimeOut, TimeUnit.MILLISECONDS))
        .addLast(new ChunkedWriteHandler())
        .addLast(executor, new NettyHandler(handler, config));
  }

}
