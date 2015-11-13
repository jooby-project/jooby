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

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Renderer.Context;
import org.jooby.spi.NativeResponse;

public class HttpRendererContext extends AbstractRendererContext {

  private Consumer<Long> length;

  private Consumer<MediaType> type;

  private NativeResponse rsp;

  public HttpRendererContext(final Set<Renderer> renderers,
      final NativeResponse rsp, final Consumer<Long> len, final Consumer<MediaType> type,
      final Map<String, Object> locals, final List<MediaType> produces, final Charset charset) {
    super(renderers, produces, charset, locals);
    this.rsp = rsp;
    this.length = len;
    this.type = type;
  }

  @Override
  public Context length(final long length) {
    this.length.accept(length);
    return this;
  }

  @Override
  public Context type(final MediaType type) {
    this.type.accept(type);
    return this;
  }

  @Override
  protected void _send(final ByteBuffer buffer) throws Exception {
    requireNonNull(buffer, "Buffer is required.");
    rsp.send(buffer);
  }

  @Override
  protected void _send(final byte[] bytes) throws Exception {
    requireNonNull(bytes, "Bytes are required.");
    rsp.send(bytes);
  }

  @Override
  protected void _send(final FileChannel file) throws Exception {
    requireNonNull(file, "File channel is required.");
    rsp.send(file);
  }

  @Override
  protected void _send(final InputStream stream) throws Exception {
    rsp.send(stream);
  }

}
