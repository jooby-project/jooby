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
  public void handle(final Request req, final Response rsp) throws Throwable {
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
      throws Throwable {
    StringBuilder buff = new StringBuilder();
    buff.append("<!doctype html>\n" +
        "<html lang=\"en\">\n" +
        " <head> \n" +
        "  <meta charset=\"UTF-8\"> \n" +
        "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"> \n" +
        "  <title>jooby: assets compiler</title>\n" +
        "  <link href=\"/org/jooby/assets/live/images/apps/favicon.ico\" rel=\"shortcut icon\"> \n"
        +
        "  <link href=\"/org/jooby/assets/live/images/apps/favicon.png\" rel=\"icon\" sizes=\"16x16\" type=\"image/png\"> \n"
        +
        "  <link href=\"/org/jooby/assets/live/images/apps/favicon32.png\" rel=\"icon\" sizes=\"32x32\" type=\"image/png\"> \n"
        +
        "  <link href=\"/org/jooby/assets/live/images/apps/favicon96.png\" rel=\"icon\" sizes=\"96x96\" type=\"image/png\"> \n"
        +
        "  <link href=\"/org/jooby/assets/live/images/apps/android-chrome.png\" rel=\"icon\" sizes=\"192x192\" type=\"image/png\"> \n"
        +
        "  <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Raleway:400,300,500,700\"> \n"
        +
        "  <link rel=\"stylesheet\" href=\"/org/jooby/assets/live/styles/application.css\"> \n" +
        "  <link rel=\"stylesheet\" href=\"/org/jooby/assets/live/styles/github.css\"> \n" +
        " </head> \n" +
        " <body class=\"page-section\"> \n" +
        "  <header class=\"site-header\"> \n" +
        "   <div class=\"row\">\n" +
        "    <a href=\"/\" title=\"Home\" class=\"site-logo\"><img src=\"/org/jooby/assets/live/images/logo_jooby-2x.png\" alt=\"Logo Jooby\" width=\"76\" height=\"31\"></a>\n"
        +
        "   </div> \n" +
        "  </header> \n" +
        "  <main role=\"main\" class=\"site-content\"> \n" +
        "   <div class=\"section-title\"> \n" +
        "    <div class=\"row\"> \n" +
        "     <h1>Asset compiler</h1> \n" +
        "    </div> \n" +
        "   </div> \n" +
        "   <div class=\"row\"> \n" +
        "   <div class=\"section-content\"> ")
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
        buff.append("<div class=\"highlighter-rouge\">");
        buff.append("<pre class=\"highlight\"><code>").append(evidence.trim())
            .append("</code></pre></div>\n");
      }
      buff.append("</li>\n");
    });

    buff.append("</ul>\n")
        .append(
            "<script type=\"text/javascript\" src=\"/org/jooby/assets/live/highlight.pack.js\"></script>\n")
        .append("<script>hljs.initHighlightingOnLoad();</script>")
        .append("</div> \n" +
            "</div> \n" +
            "  </main> \n" +
            " </body>\n" +
            "</html>");

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
