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

import java.util.Map;

import org.eclipse.jetty.server.PushBuilder;
import org.eclipse.jetty.server.Request;
import org.jooby.spi.NativePushPromise;

public class JettyPush implements NativePushPromise {

  private Request req;

  public JettyPush(final Request req) {
    this.req = req;
  }

  @Override
  public void push(final String method, final String path, final Map<String, Object> headers) {
    PushBuilder pb = req.getPushBuilder()
        .path(path)
        .method(method);
    headers.forEach((n, v) -> pb.addHeader(n, v.toString()));
    pb.push();
  }

}
