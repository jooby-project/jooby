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

import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class EmbeddedHandler implements Route.Handler {

  private String path;

  private Supplier<RestController> controller;

  private boolean detailedErrorsEnabled;

  public EmbeddedHandler(final String path, final ManagedNode node, final boolean detailedErrors) {
    this.path = path;
    this.controller = Suppliers.memoize(() ->
        ((InternalNode) node.get()).injector().getInstance(RestController.class));
    detailedErrorsEnabled = detailedErrors;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {

    EmbeddedHttpRequest restReq = new EmbeddedHttpRequest(path, req);
    EmbeddedHttpChannel channel = new EmbeddedHttpChannel(restReq, rsp, detailedErrorsEnabled);
    controller.get().dispatchRequest(restReq, channel);

    channel.done();
  }

}
