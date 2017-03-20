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
package org.jooby.servlet;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

public class ServletServletResponse implements NativeResponse {

  protected HttpServletRequest req;

  protected HttpServletResponse rsp;

  private boolean committed;

  public ServletServletResponse(final HttpServletRequest req, final HttpServletResponse rsp) {
    this.req = requireNonNull(req, "A request is required.");
    this.rsp = requireNonNull(rsp, "A response is required.");
  }

  @Override
  public List<String> headers(final String name) {
    Collection<String> headers = rsp.getHeaders(name);
    if (headers == null || headers.size() == 0) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(headers);
  }

  @Override
  public Optional<String> header(final String name) {
    String header = rsp.getHeader(name);
    return header == null || header.isEmpty() ? Optional.empty() : Optional.of(header);
  }

  @Override
  public void header(final String name, final String value) {
    rsp.setHeader(name, value);
  }

  @Override
  public void header(final String name, final Iterable<String> values) {
    for (String value : values) {
      rsp.addHeader(name, value);
    }
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    ServletOutputStream output = rsp.getOutputStream();
    output.write(bytes);
    output.close();
    committed = true;
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    WritableByteChannel channel = Channels.newChannel(rsp.getOutputStream());
    channel.write(buffer);
    channel.close();
    committed = true;
  }

  @Override
  public void send(final FileChannel file) throws Exception {
    try (FileChannel src = file) {
      WritableByteChannel channel = Channels.newChannel(rsp.getOutputStream());
      src.transferTo(0, file.size(), channel);
      channel.close();
      committed = true;
    }
  }

  @Override
  public void send(final FileChannel channel, final long position, final long count)
      throws Exception {
    try (FileChannel src = channel) {
      WritableByteChannel dest = Channels.newChannel(rsp.getOutputStream());
      src.transferTo(position, count, dest);
      dest.close();
      committed = true;
    }
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    ServletOutputStream output = rsp.getOutputStream();
    ByteStreams.copy(stream, output);
    output.close();
    stream.close();
    committed = true;
  }

  @Override
  public int statusCode() {
    return rsp.getStatus();
  }

  @Override
  public void statusCode(final int statusCode) {
    rsp.setStatus(statusCode);
  }

  @Override
  public boolean committed() {
    if (committed) {
      return true;
    }
    return rsp.isCommitted();
  }

  @Override
  public void end() {
    if (!committed) {
      if (req.isAsyncStarted()) {
        AsyncContext ctx = req.getAsyncContext();
        ctx.complete();
      } else {
        close();
      }
      committed = true;
    }
    req = null;
    rsp = null;
  }

  protected void close() {
  }

  @Override
  public void reset() {
    rsp.reset();
  }

}
