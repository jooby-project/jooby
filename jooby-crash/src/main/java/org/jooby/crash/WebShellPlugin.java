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
package org.jooby.crash;

import org.crsh.plugin.CRaSHPlugin;
import org.jooby.Env;
import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.Router;

import com.typesafe.config.Config;

class WebShellPlugin extends CRaSHPlugin<WebShellPlugin> {

  @Override
  public WebShellPlugin getImplementation() {
    return this;
  }

  static void install(final Env env, final Config conf) {
    Router routes = env.router();
    String path = conf.getString("crash.webshell.path");
    String title = conf.getString("application.name") + " shell";
    routes.assets(path + "/css/**", "META-INF/resources/css/{0}");
    routes.assets(path + "/js/**", "META-INF/resources/js/{0}");
    String rootpath = Route.normalize(conf.getString("application.path") + path);

    routes.get(path, req -> Results.ok("<!DOCTYPE HTML>\n" +
        "<html>\n" +
        "<head>\n" +
        "    <meta charset=\"utf-8\" />\n" +
        "    <title>" + title + "</title>\n" +
        "    <script src=\"" + rootpath + "/js/jquery-1.7.1.min.js\"></script>\n" +
        "    <script src=\"" + rootpath + "/js/jquery.mousewheel-min.js\"></script>\n" +
        "    <script src=\"" + rootpath + "/js/jquery.terminal-0.7.12.js\"></script>\n" +
        "    <script src=\"" + rootpath + "/js/crash.js\"></script>\n" +
        "    <link href=\"" + rootpath + "/css/jquery.terminal.css\" rel=\"stylesheet\"/>\n" +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "<script>\n" +
        "\n" +
        "  //\n" +
        "  $(function() {\n" +
        "    // Create web socket url\n" +
        "    var protocol;\n" +
        "    if (window.location.protocol == 'http:') {\n" +
        "      protocol = 'ws';\n" +
        "    } else {\n" +
        "      protocol = 'wss';\n" +
        "    }\n" +
        "    var url = protocol + '://' + window.location.host + '" + rootpath + "';\n" +
        "    var crash = new CRaSH($('#shell'));\n" +
        "    crash.connect(url);\n" +
        "  });\n" +
        "\n" +
        "</script>\n" +
        "\n" +
        "  <div id=\"shell\"></div>\n" +
        "\n" +
        "</body>\n" +
        "</html>").type(MediaType.html));

    routes.ws(path, new WebShellHandler()).consumes(MediaType.json).produces(MediaType.json);
  }

}
