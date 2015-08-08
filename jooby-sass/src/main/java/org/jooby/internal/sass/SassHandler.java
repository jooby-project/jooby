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
package org.jooby.internal.sass;

import java.net.URL;

import org.jooby.Asset;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Results;
import org.jooby.handlers.AssetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.sass.internal.ScssStylesheet;
import com.vaadin.sass.internal.handler.SCSSDocumentHandlerImpl;
import com.vaadin.sass.internal.resolver.ClassloaderResolver;
import com.vaadin.sass.internal.resolver.FilesystemResolver;

/**
 * <a href="http://sass-lang.com">Sass handler</a> transform a <code>.scss</code> to
 * <code>.css</code>.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * {
 *   assets("/css/**", new Sass());
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.9.2
 */
public class SassHandler extends AssetHandler {

  private static final String JAR_ENTRY = "!/";

  private static final String FILE = "file:";

  private static final String SASS_EXT = ".scss";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * <p>
   * Creates a new Sass handler. The location pattern can be one of.
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
   * @param pattern Location pattern.
   */
  public SassHandler(final String pattern) {
    super(pattern);
  }

  /**
   * Creates a new {@link SassHandler} handler with a location pattern of: <code>/</code>.
   */
  public SassHandler() {
    this("/");
  }

  @Override
  protected URL resolve(final String path) throws Exception {
    return super.resolve(path.replace(".css", SASS_EXT));
  }

  @Override
  protected void send(final Request req, final Response rsp, final Asset asset) throws Exception {
    String css = scss(asset).printState();

    rsp.type(asset.type())
        .send(Results.ok(css));
  }

  private ScssStylesheet scss(final Asset asset) throws Exception {
    String resource = asset.resource().toExternalForm();
    if (resource.startsWith(FILE)) {
      resource = resource.substring(FILE.length());
    }

    int entry = resource.indexOf(JAR_ENTRY);
    if (entry >= 0) {
      resource = resource.substring(entry + 2);
    }

    FileNotFoundResolver fnfResolver = new FileNotFoundResolver();
    SassErrHandler errHandler = new SassErrHandler(log);

    ScssStylesheet root = new ScssStylesheet();
    root.addResolver(new FilesystemResolver());
    root.addResolver(new ClassloaderResolver());
    root.addResolver(fnfResolver);

    ScssStylesheet scss = ScssStylesheet.get(
        resource,
        null,
        new SCSSDocumentHandlerImpl(root),
        errHandler
        );

    fnfResolver.validate(scss);

    scss.compile();

    fnfResolver.validate(scss);
    errHandler.validate();

    return scss;
  }

}
