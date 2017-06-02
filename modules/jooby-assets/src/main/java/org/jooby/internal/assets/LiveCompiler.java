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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.assets.AssetCompiler;
import org.jooby.assets.AssetException;
import org.jooby.assets.AssetProblem;
import org.jooby.internal.URLAsset;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;

import javaslang.control.Try;

public class LiveCompiler implements Route.Handler {

  private final Config conf;

  private final AssetCompiler compiler;

  private final Map<String, AssetException> errors = new ConcurrentHashMap<>();

  private final Watcher watcher;

  private final AtomicBoolean firstRun = new AtomicBoolean(true);

  private Path basedir;

  public LiveCompiler(final Config conf, final AssetCompiler compiler) throws IOException {
    this.conf = requireNonNull(conf, "Config is required.");
    this.compiler = requireNonNull(compiler, "Asset compiler is required.");
    this.basedir = Paths.get("public");
    this.watcher = new Watcher(this::onChange, basedir);

  }

  private void onChange(final Kind<?> kind, final Path path) {
    File outputdir = new File(conf.getString("application.tmpdir"), "__public_");
    outputdir.mkdirs();
    String filename = Route.normalize(Try.of(() -> path.subpath(1, path.getNameCount()).toString())
        .getOrElse(path.toString()));
    try {
      boolean firstRun = this.firstRun.compareAndSet(true, false);
      if (!firstRun && !path.equals(basedir)) {
        if (!compiler.contains(path.toString())) {
          return;
        }
      }
      if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
        errors.remove(filename);
      } else {
        if (Files.isDirectory(path)) {
          errors.clear();
          compiler.build(conf.getString("application.env"), outputdir);
        } else {
          MediaType type = MediaType.byPath(path).orElse(MediaType.octetstream);
          compiler.build(new URLAsset(path.toUri().toURL(), filename, type));
          errors.remove(filename);
        }
      }
    } catch (AssetException ex) {
      String localname = ex.getProblems().stream()
          .findFirst()
          .map(AssetProblem::getFilename)
          .orElse(filename);
      errors.put(localname, rewrite(ex));
    } catch (Exception ex) {
      AssetException assetEx = rewrite(new AssetException("compiler",
          new AssetProblem(filename, -1, -1, ex.getMessage(), null), ex));
      errors.put(filename, assetEx);
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
    if (req.param("assets.sync").isSet()) {
      onChange(StandardWatchEventKinds.ENTRY_MODIFY, basedir);
    }
    if (errors.size() > 0) {
      throw errors.values().iterator().next();
    }
  }

  public void start() {
    watcher.start();
  }

  public void stop() throws Exception {
    watcher.stop();
  }

}
