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
package org.jooby.internal.less;

import java.net.URL;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Results;
import org.jooby.handlers.AssetHandler;
import org.jooby.less.Less;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.LessSource;

public class LessHandler extends AssetHandler {

  private LessCompiler compiler;

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Less.class);

  public LessHandler(final String location, final LessCompiler compiler) {
    super(location);
    this.compiler = compiler;
  }

  @Override
  protected URL resolve(final String path) throws Exception {
    return super.resolve(path.replace(".css", ".less").replace(".map", ""));
  }

  @Override
  protected void send(final Request req, final Response rsp, final Asset asset) throws Exception {
    LessSource.URLSource src = new LessSource.URLSource(asset.resource(), req.charset().name());

    CompilationResult result = compiler.compile(src);

    result.getWarnings().forEach(warning -> log.warn(warning.toString()));

    if (req.path().endsWith(".map")) {
      rsp.type(MediaType.json)
          .send(Results.ok(result.getSourceMap()));
    } else {
      rsp.type(MediaType.css)
          .send(Results.ok(result.getCss()));
    }
  }
}
