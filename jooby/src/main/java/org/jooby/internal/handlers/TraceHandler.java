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
package org.jooby.internal.handlers;

import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

public class TraceHandler implements Route.Handler {

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    String CRLF = "\r\n";
    StringBuilder buffer = new StringBuilder("TRACE ").append(req.path())
        .append(" ").append(req.protocol());

    for (Entry<String, Mutant> entry : req.headers().entrySet()) {
      buffer.append(CRLF).append(entry.getKey()).append(": ")
          .append(entry.getValue().toList(String.class).stream().collect(Collectors.joining(", ")));
    }

    buffer.append(CRLF);

    rsp.type(MediaType.valueOf("message/http"));
    rsp.length(buffer.length());
    rsp.send(buffer.toString());
  }

}
