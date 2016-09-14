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
package org.jooby.unbescape;

import static java.util.Objects.requireNonNull;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.unbescape.css.CssEscape;
import org.unbescape.css.CssStringEscapeLevel;
import org.unbescape.css.CssStringEscapeType;
import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;
import org.unbescape.javascript.JavaScriptEscape;
import org.unbescape.javascript.JavaScriptEscapeLevel;
import org.unbescape.javascript.JavaScriptEscapeType;
import org.unbescape.json.JsonEscape;
import org.unbescape.json.JsonEscapeLevel;
import org.unbescape.json.JsonEscapeType;
import org.unbescape.uri.UriEscape;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>unbescape</h1>
 * <p>
 * <a href="https://github.com/unbescape/unbescape">Unbescape</a> is a Java library aimed at
 * performing fully-featured and high-performance escape and unescape operations for:
 * <code>HTML</code>, <code>JavaScript</code> and lot more.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li><strong>html</strong> escaper.</li>
 * <li><strong>js</strong> escaper.</li>
 * <li><strong>json</strong> escaper.</li>
 * <li><strong>css</strong> escaper.</li>
 * <li><strong>uri</strong> escaper.</li>
 * <li><strong>queryParam</strong> escaper.</li>
 * <li><strong>uriFragmentId</strong> escaper.</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   use(new XSS());
 *
 *   post("/", req -> {
 *     String safeHtml = req.param("text", "html").value();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Nested context are supported by providing multiple encoders:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new XSS());
 *
 *   post("/", req -> {
 *     String safeHtml = req.param("text", "js", "html", "uri").value();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Encoders run in the order they are provided.
 * </p>
 *
 * @author edgar
 * @since 1.0.0
 */
public class XSS implements Module {

  private HtmlEscapeType htmltype = HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL;
  private HtmlEscapeLevel htmllevel = HtmlEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC;

  private JavaScriptEscapeLevel jslevel = JavaScriptEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC;
  private JavaScriptEscapeType jstype = JavaScriptEscapeType.SINGLE_ESCAPE_CHARS_DEFAULT_TO_XHEXA_AND_UHEXA;

  private JsonEscapeLevel jsonlevel = JsonEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC;
  private JsonEscapeType jsontype = JsonEscapeType.SINGLE_ESCAPE_CHARS_DEFAULT_TO_UHEXA;

  private CssStringEscapeType csstype = CssStringEscapeType.BACKSLASH_ESCAPES_DEFAULT_TO_COMPACT_HEXA;
  private CssStringEscapeLevel csslevel = CssStringEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC;

  /**
   * Set JavaScript escape type and level.
   *
   * @param type Type.
   * @param level Level.
   * @return This module.
   */
  public XSS js(final JavaScriptEscapeType type, final JavaScriptEscapeLevel level) {
    this.jslevel = requireNonNull(level, "Level required.");
    this.jstype = requireNonNull(type, "Type required.");
    return this;
  }

  /**
   * Set HTML escape type and level.
   *
   * @param type Type.
   * @param level Level.
   * @return This module.
   */
  public XSS html(final HtmlEscapeType type, final HtmlEscapeLevel level) {
    this.htmllevel = requireNonNull(level, "Level required.");
    this.htmltype = requireNonNull(type, "Type required.");
    return this;
  }

  /**
   * Set JSON escape type and level.
   *
   * @param type Type.
   * @param level Level.
   * @return This module.
   */
  public XSS json(final JsonEscapeType type, final JsonEscapeLevel level) {
    this.jsonlevel = requireNonNull(level, "Level required.");
    this.jsontype = requireNonNull(type, "Type required.");
    return this;
  }

  /**
   * Set CSS escape type and level.
   *
   * @param type Type.
   * @param level Level.
   * @return This module.
   */
  public XSS css(final CssStringEscapeType type, final CssStringEscapeLevel level) {
    this.csslevel = requireNonNull(level, "Level required.");
    this.csstype = requireNonNull(type, "Type required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    env.xss("html", it -> HtmlEscape.escapeHtml(it, htmltype, htmllevel))
        .xss("js", it -> JavaScriptEscape.escapeJavaScript(it, jstype, jslevel))
        .xss("json", it -> JsonEscape.escapeJson(it, jsontype, jsonlevel))
        .xss("css", it -> CssEscape.escapeCssString(it, csstype, csslevel))
        .xss("uri", UriEscape::escapeUriPath)
        .xss("queryParam", UriEscape::escapeUriQueryParam)
        .xss("uriFragmentId", UriEscape::escapeUriFragmentId);
  }

}
