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

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.assets.AssetCompiler;

import javaslang.Function1;

public class AssetVars implements Route.Handler {

  private AssetCompiler compiler;

  private String cpath;

  private Function1<String, List<String>> styles;
  private Function1<String, List<String>> scripts;

  public AssetVars(final AssetCompiler compiler, final String cpath, final boolean cache) {
    this.compiler = compiler;
    this.cpath = cpath.equals("/") ? "" : cpath;
    styles = compiler::styles;
    scripts = compiler::scripts;
    if (cache) {
      styles = styles.memoized();
      scripts = scripts.memoized();
    }
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    compiler.fileset().forEach(asset -> {
      /** Styles */
      List<String> css = this.styles.apply(asset);
      String styles = css.stream().reduce(new StringBuilder(),
          (buff, it) -> buff.append("<link href=\"")
              .append(cpath)
              .append(it)
              .append("\" rel=\"stylesheet\">\n"),
          StringBuilder::append)
          .toString();
      req.set(asset + "_css", css);
      req.set(asset + "_styles", styles);

      /** Scripts */
      List<String> js = this.scripts.apply(asset);
      String scripts = js.stream().reduce(new StringBuilder(),
          (buff, it) -> buff.append("<script src=\"")
              .append(cpath)
              .append(it)
              .append("\"></script>\n"),
          StringBuilder::append)
          .toString();
      req.set(asset + "_js", js);
      req.set(asset + "_scripts", scripts);
    });
  }

}
