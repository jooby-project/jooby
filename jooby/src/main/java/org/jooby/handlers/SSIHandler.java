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
package org.jooby.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.jooby.Asset;
import org.jooby.Env;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

import com.google.common.io.CharStreams;

/**
 * <h1>server side include</h1>
 * <p>
 * Custom {@link AssetHandler} with <code>server side include</code> function.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   get("/static/**", new SSIHandler());
 * }
 * }</pre>
 *
 * <p>
 * Request to <code>/static/index.html</code>:
 * </p>
 *
 * <pre>
 * &lt;html&gt;
 * &lt;-- /static/chunk.html --&gt;
 * &lt;/html&gt;
 * </pre>
 *
 * <p>
 * The {@link SSIHandler} will resolve and insert the content of <code>/static/chunk.html</code>.
 * </p>
 *
 * <h2>delimiters</h2>
 * <p>
 * Default delimiter are: <code>&lt;--</code> and <code>--&gt;</code>. You can override this using
 * {@link #delimiters(String, String)} function:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/static/**", new SSIHandler().delimiters("{{", "}}");
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.1.0
 */
public class SSIHandler extends AssetHandler {

  private String startDelimiter = "<!--";

  private String endDelimiter = "-->";

  /**
   * <p>
   * Creates a new {@link SSIHandler}. The location pattern can be one of.
   * </p>
   *
   * Given <code>/</code> like in <code>assets("/assets/**", "/")</code> with:
   *
   * <pre>
   *   GET /assets/js/index.js it translates the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>/assets</code> like in <code>assets("/js/**", "/assets")</code> with:
   *
   * <pre>
   *   GET /js/index.js it translate the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>/META-INF/resources/webjars/{0}</code> like in
   * <code>assets("/webjars/**", "/META-INF/resources/webjars/{0}")</code> with:
   *
   * <pre>
   *   GET /webjars/jquery/2.1.3/jquery.js it translate the path to: /META-INF/resources/webjars/jquery/2.1.3/jquery.js
   * </pre>
   *
   * @param pattern Pattern to locate static resources.
   */
  public SSIHandler(final String pattern) {
    super(pattern);
  }

  /**
   * <p>
   * Creates a new {@link SSIHandler}. Location pattern is set to: <code>/</code>.
   * </p>
   */
  public SSIHandler() {
    this("/");
  }

  /**
   * Set/override delimiters.
   *
   * @param start Start delimiter.
   * @param end Stop/end delimiter.
   * @return This handler.
   */
  public SSIHandler delimiters(final String start, final String end) {
    this.startDelimiter = start;
    this.endDelimiter = end;
    return this;
  }

  @Override
  protected void send(final Request req, final Response rsp, final Asset asset) throws Throwable {
    Env env = req.require(Env.class);
    CharSequence text = env.resolver()
        .delimiters(startDelimiter, endDelimiter)
        .source(this::file)
        .ignoreMissing()
        .resolve(text(asset.stream()));

    rsp.type(asset.type())
        .send(text);
  }

  private String file(final String key) {
    String file = Route.normalize(key.trim());
    return text(getClass().getResourceAsStream(file));
  }

  private String text(final InputStream stream) {
    try (InputStream in = stream) {
      return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch (IOException | NullPointerException x) {
      throw new NoSuchElementException();
    }
  }
}
