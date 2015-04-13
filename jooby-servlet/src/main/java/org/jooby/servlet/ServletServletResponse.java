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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;

public class ServletServletResponse implements NativeResponse {

  private HttpServletResponse rsp;

  private boolean committed;

  public ServletServletResponse(final HttpServletResponse rsp) {
    this.rsp = requireNonNull(rsp, "A response is required.");
  }

  @Override
  public List<String> headers(final String name) {
    Collection<String> headers = rsp.getHeaders(name);
    if (headers == null) {
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
  public OutputStream out(final int bufferSize) throws IOException {
    rsp.setBufferSize(bufferSize);
    return rsp.getOutputStream();
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
    // NOOP
    committed = true;
  }

  @Override
  public void reset() {
    rsp.reset();
  }

}
