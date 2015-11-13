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
package org.jooby.internal.assets;

import static java.util.Objects.requireNonNull;

import org.jooby.Asset;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.assets.AssetCompiler;
import org.jooby.handlers.AssetHandler;

public class AssetHandlerWithCompiler extends AssetHandler {

  private AssetCompiler compiler;

  public AssetHandlerWithCompiler(final String pattern, final AssetCompiler compiler) {
    super(pattern);
    this.compiler = requireNonNull(compiler, "Asset compiler is required.");
  }

  @Override
  protected void send(final Request req, final Response rsp, final Asset asset) throws Exception {
    super.send(req, rsp, compiler.build(asset));
  }
}
