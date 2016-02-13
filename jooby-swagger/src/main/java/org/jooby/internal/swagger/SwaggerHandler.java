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
package org.jooby.internal.swagger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jooby.Request;
import org.jooby.Route;
import org.jooby.swagger.SwaggerUI;

import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;

/**
 * Serve the default: <code>swagger.html</code> file.
 *
 * @author edgar
 * @since 0.6.2
 */
public class SwaggerHandler implements Route.OneArgHandler {

  private String pattern;

  private String template;

  /**
   * Creates a new handler to serve the: <code>swagger.html</code> file.
   *
   * @param path Path to listen for, like <code>/swagger</code>.
   */
  public SwaggerHandler(final String path) {
    this.pattern = path;
    if (!this.pattern.startsWith("/")) {
      this.pattern = "/" + pattern;
    }
    this.template = template();
  }

  @Override
  public Object handle(final Request req) throws Exception {
    Config conf = req.require(Config.class);
    String path = conf.getString("application.path");
    if (path.endsWith("/")) {
      path += pattern.substring(1);
    } else {
      path += pattern;
    }
    String uipath = path;

    path += req.param("tag").toOptional().map(t -> "/" + t).orElse("");

    String html = template.replace("${ui.path}", uipath).replace("${path}", path);
    return html;
  }

  private static String template() {
    try (InputStream in = SwaggerUI.class.getResourceAsStream("swagger.html")) {
      return new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Not found: swagger.html", ex);
    }
  }

}
