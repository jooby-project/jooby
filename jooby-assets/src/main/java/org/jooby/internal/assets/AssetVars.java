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

import java.util.List;
import java.util.stream.Collectors;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.assets.AssetCompiler;

public class AssetVars implements Route.Handler {

  private AssetCompiler compiler;

  private String cpath;

  public AssetVars(final AssetCompiler compiler, final String cpath) {
    this.compiler = compiler;
    this.cpath = cpath.equals("/") ? "" : cpath;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    compiler.fileset().forEach(asset -> {
      /** Styles */
      List<String> styles = compiler.styles(asset);
      req.set(asset + "_css", styles);
      req.set(asset + "_styles", styles.stream()
          .map(p -> "<link href=\"" + cpath + p + "\" rel=\"stylesheet\">\n")
          .collect(Collectors.joining()));

      /** Scripts */
      List<String> scripts = compiler.scripts(asset);
      req.set(asset + "_js", scripts);
      req.set(asset + "_scripts", scripts.stream()
          .map(p -> "<script src=\"" + cpath + p + "\"></script>\n")
          .collect(Collectors.joining()));
    });
  }

}
