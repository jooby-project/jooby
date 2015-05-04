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
package org.jooby.internal.jetty;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.jooby.servlet.ServletServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyResponse extends ServletServletResponse implements Callback {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(org.jooby.Response.class);

  public JettyResponse(final HttpServletResponse rsp) {
    super(rsp);
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    sender().sendContent(ByteBuffer.wrap(bytes), this);
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    sender().sendContent(buffer, this);
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    sender().sendContent(Channels.newChannel(stream), this);
  }

  @Override
  public void send(final FileChannel channel) throws Exception {
    sender().sendContent(channel, this);
  }

  private HttpOutput sender() {
    return ((Response) rsp).getHttpOutput();
  }

  @Override
  public void succeeded() {
    // NOOP
  }

  @Override
  public void failed(final Throwable cause) {
    // TODO: will be nice to log the path of the current request
    log.error(rsp.toString(), cause);
  }

}
