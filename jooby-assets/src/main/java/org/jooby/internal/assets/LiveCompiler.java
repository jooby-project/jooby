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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.assets.AssetCompiler;
import org.jooby.assets.AssetException;
import org.jooby.assets.AssetProblem;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;

public class LiveCompiler implements Route.Handler {

  private final Config conf;

  private final AssetCompiler compiler;

  private final AtomicReference<AssetException> lastErr = new AtomicReference<AssetException>(null);

  private final Watcher watcher;

  public LiveCompiler(final Config conf, final AssetCompiler compiler) throws IOException {
    this.conf = requireNonNull(conf, "Config is required.");
    this.compiler = requireNonNull(compiler, "Asset compiler is required.");
    this.watcher = new Watcher(this::onChange, Paths.get("public"));
  }

  private void onChange(final Kind<?> kind, final Path path) {
    File outputdir = new File(conf.getString("application.tmpdir"), "__public_");
    outputdir.mkdirs();
    try {
      compiler.build(conf.getString("application.env"), outputdir);
      lastErr.set(null);
    } catch (AssetException ex) {
      lastErr.set(rewrite(ex));
    } catch (Exception ex) {
      lastErr.set(rewrite(new AssetException("compiler",
          new AssetProblem(path.toString(), -1, -1, ex.getMessage(), null))));
    }
  }

  /**
   * Add a virtual/fake stacktrace element with the offending file, useful for whoops.
   *
   * @param ex Stack trace to rewrite.
   * @return Same exception with new stacktrace.
   */
  private AssetException rewrite(final AssetException ex) {
    List<StackTraceElement> stacktrace = Lists.newArrayList(ex.getStackTrace());
    List<AssetProblem> problems = ex.getProblems();
    AssetProblem head = problems.get(0);
    stacktrace.add(0,
        new StackTraceElement(head.getFilename(), "", head.getFilename(), head.getLine()));
    ex.setStackTrace(stacktrace.toArray(new StackTraceElement[stacktrace.size()]));
    return ex;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    AssetException ex = lastErr.get();
    if (ex != null) {
      throw ex;
    }
  }

  public void start() {
    watcher.start();
  }

  public void stop() throws Exception {
    watcher.stop();
  }

}
