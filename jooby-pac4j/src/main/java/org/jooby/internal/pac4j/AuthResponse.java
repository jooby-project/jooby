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
package org.jooby.internal.pac4j;

import org.jooby.Err;
import org.jooby.Response;
import org.pac4j.core.client.Client;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.BasicAuthClient;

public class AuthResponse {

  private Response rsp;

  public AuthResponse(final Response rsp) {
    this.rsp = rsp;
  }

  public void handle(final Client<?, ?> client, final RequiresHttpAction action) {
    if (!rsp.committed()) {
      int statusCode = action.getCode();
      // on error, let jooby handle it
      if (statusCode >= 400) {
        if (client instanceof BasicAuthClient) {
          rsp.status(statusCode).end();
        } else {
          throw new Err(statusCode, action);
        }
      }
    }
  }
}
