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
package org.jooby.metrics;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route.Handler;
import org.jooby.Status;

/**
 * Produces a:
 * <ul>
 * <li>200: with a body of <code>pong</code></li>
 * </ul>
 *
 * @author edgar
 * @since 0.13.0
 */
public class PingHandler implements Handler {

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    rsp.status(Status.OK)
        .type(MediaType.plain)
        .header("Cache-Control", "must-revalidate,no-cache,no-store")
        .send("pong");
    ;
  }

}
