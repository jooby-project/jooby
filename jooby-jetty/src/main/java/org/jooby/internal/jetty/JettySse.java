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

import java.util.Optional;
import java.util.concurrent.Executor;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.jooby.Sse;

import com.google.common.util.concurrent.MoreExecutors;

import javaslang.concurrent.Promise;
import javaslang.control.Try;

public class JettySse extends Sse {

  private Request req;

  private Response rsp;

  private HttpOutput out;

  public JettySse(final Request request, final Response rsp) {
    this.req = request;
    this.rsp = rsp;
    this.out = rsp.getHttpOutput();
  }

  @Override
  protected void closeInternal() {
    Try.run(() -> rsp.closeOutput())
        .onFailure(cause -> log.debug("error while closing connection", cause));
  }

  @Override
  protected void handshake(final Runnable handler) throws Exception {
    /** Infinite timeout because the continuation is never resumed but only completed on close. */
    req.getAsyncContext().setTimeout(0L);
    /** Server sent events headers. */
    rsp.setStatus(HttpServletResponse.SC_OK);
    rsp.setHeader("Connection", "Close");
    rsp.setContentType("text/event-stream; charset=utf-8");
    rsp.flushBuffer();

    HttpChannel channel = rsp.getHttpChannel();
    Connector connector = channel.getConnector();
    Executor executor = connector.getExecutor();
    executor.execute(handler);
  }

  @Override
  protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
    synchronized (this) {
      Promise<Optional<Object>> promise = Promise.make(MoreExecutors.newDirectExecutorService());
      try {
        out.write(data);
        out.flush();
        promise.success(id);
      } catch (Throwable ex) {
        promise.failure(ex);
        ifClose(ex);
      }
      return promise;
    }
  }

  @Override
  protected boolean shouldClose(final Throwable ex) {
    return ex instanceof EofException || super.shouldClose(ex);
  }

}
