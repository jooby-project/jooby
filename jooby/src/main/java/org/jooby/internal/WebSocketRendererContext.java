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
package org.jooby.internal;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.WebSocket.ErrCallback;
import org.jooby.WebSocket.SuccessCallback;
import org.jooby.spi.NativeWebSocket;

import com.google.common.collect.ImmutableList;

public class WebSocketRendererContext extends AbstractRendererContext {

  private NativeWebSocket ws;

  private SuccessCallback success;

  private ErrCallback err;

  private MediaType type;

  public WebSocketRendererContext(final List<Renderer> renderers, final NativeWebSocket ws,
      final MediaType type, final Charset charset, final SuccessCallback success,
      final ErrCallback err) {
    super(renderers, ImmutableList.of(type), charset, Collections.emptyMap());
    this.ws = ws;
    this.type = type;
    this.success = success;
    this.err = err;
  }

  @Override
  public void send(final String text) throws Exception {
    ws.sendText(text, success, err);
    setCommitted();
  }

  @Override
  protected void _send(final byte[] bytes) throws Exception {
    if (type.isText()) {
      ws.sendText(bytes, success, err);
    } else {
      ws.sendBytes(bytes, success, err);
    }
  }

  @Override
  protected void _send(final ByteBuffer buffer) throws Exception {
    if (type.isText()) {
      ws.sendText(buffer, success, err);
    } else {
      ws.sendBytes(buffer, success, err);
    }
  }

  @Override
  protected void _send(final FileChannel file) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void _send(final InputStream stream) throws Exception {
    throw new UnsupportedOperationException();
  }

}
