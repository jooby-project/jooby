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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.jooby.Managed;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.assets.AssetCompiler;
import org.jooby.assets.AssetException;
import org.jooby.assets.AssetProblem;
import org.jooby.handlers.AssetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

public class LiveCompiler implements Route.Handler, Managed {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

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
      log.error("Found " + ex.getProblems().size() + " problem(s): \n" +
          ex.getProblems().stream()
              .map(AssetProblem::toString)
              .collect(Collectors.joining("\n")));
      lastErr.set(ex);
    } catch (Exception ex) {
      log.error("Found 1 problem(s): \n", ex);
      lastErr.set(new AssetException("compiler",
          new AssetProblem("unknown", -1, -1, ex.getMessage(), null)));
    }
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    String path = req.path();
    if (path.startsWith("/org/jooby/assets/live")) {
      new AssetHandler("/").handle(req, rsp);
      return;
    }
    AssetException ex = lastErr.get();
    if (ex != null) {
      reportErr(req, rsp, ex);
    }
  }

  private void reportErr(final Request req, final Response rsp, final AssetException ex)
      throws Exception {
    StringBuilder buff = new StringBuilder();
    buff.append("<!doctype html>\n")
        .append("<html>\n")
        .append("<head>\n")
        .append("<title>Asset compiler</title>\n")
        .append("<style>\n")
        .append(
            "body {font-family: monospace; margin-left: 20px;background-color:#f8f8f8}\n")
        .append("footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n")
        .append("hr {background-color: #f7f7f9;}\n")
        .append("ul {padding: 0;}\n")
        .append("ul li {list-style: none; border-bottom: 1px solid #ccc;}\n")
        .append("</style>\n")
        .append("<link href=\"/org/jooby/assets/live/styles/github.css\" rel=\"stylesheet\">\n")
        .append(
            "<script type=\"text/javascript\" src=\"/org/jooby/assets/live/highlight.pack.js\"></script>\n")
        .append("</head>\n")
        .append("\n")
        .append("<body>\n")
        .append("<h2> ").append(ex.getId()).append(" found ").append(ex.getProblems().size())
        .append(" problem(s):</h2>\n")
        .append("<ul>\n");

    ex.getProblems().forEach(problem -> {
      buff.append("  <li>").append("<pre><code class=\"nohighlight\">")
          .append(problem.getFilename())
          .append(":")
          .append(problem.getLine())
          .append(":")
          .append(problem.getColumn())
          .append(": ")
          .append(problem.getMessage())
          .append("</code></pre>");
      String evidence = problem.getEvidence();
      if (evidence.length() > 0) {
        buff.append("<pre><code>").append(evidence.trim()).append("</code></pre>\n");
      }
      buff.append("</li>\n");
    });

    buff.append("</ul>\n")
        .append("<script>hljs.initHighlightingOnLoad();</script>")
        .append("</body>\n")
        .append("\n")
        .append("</html>");

    rsp.send(Results.ok(buff.toString()).type(MediaType.html).status(200));

  }

  @Override
  public void start() throws Exception {
    watcher.start();
  }

  @Override
  public void stop() throws Exception {
    watcher.stop();
  }

}
