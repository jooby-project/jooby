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
package org.jooby.internal.elasticsearch;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CountDownLatch;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.jooby.Response;

public class EmbeddedHttpChannel extends HttpChannel {

  private Response rsp;

  private final CountDownLatch latch = new CountDownLatch(1);

  private Throwable failure;

  public EmbeddedHttpChannel(final RestRequest request, final Response rsp,
      final boolean detailedErrorsEnabled) {
    super(request, detailedErrorsEnabled);
    this.rsp = requireNonNull(rsp, "Response is required.");
  }

  @Override
  public void sendResponse(final RestResponse response) {
    try {
      String opaque = request.header("X-Opaque-Id");
      if (opaque != null) {
        rsp.header("X-Opaque-Id", opaque);
      }

      BytesReference content = response.content();
      rsp.status(response.status().getStatus())
          .length(content.length())
          .type(response.contentType())
          .send(content);
    } catch (Throwable ex) {
      failure = ex;
    } finally {
      latch.countDown();
    }

  }

  public void done() throws Throwable {
    latch.await();

    if (failure != null) {
      throw failure;
    }
  }

}
