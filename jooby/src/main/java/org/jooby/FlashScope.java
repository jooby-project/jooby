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
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jooby.internal.handlers.FlashScopeHandler;
import org.jooby.mvc.Flash;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>flash scope</h1>
 * <p>
 * The flash scope is designed to transport success and error messages, between requests. The flash
 * scope is similar to {@link Session} but lifecycle is shorter: data are kept for only one request.
 * </p>
 * <p>
 * The flash scope is implemented as client side cookie, so it helps to keep application stateless.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new FlashScope());
 *
 *   get("/", req -> {
 *     return req.ifFlash("success").orElse("Welcome!");
 *   });
 *
 *   post("/", req -> {
 *     req.flash("success", "The item has been created");
 *     return Results.redirect("/");
 *   });
 * }
 * }</pre>
 *
 * {@link FlashScope} is also available on mvc routes via {@link Flash} annotation:
 *
 * <pre>{@code
 * &#64;Path("/")
 * public class Controller {
 *
 *   &#64;GET
 *   public Object flashScope(@Flash Map&lt;String, String&gt; flash) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object flashAttr(@Flash String foo) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object optionlFlashAttr(@Flash Optional&lt;String&gt; foo) {
 *     ...
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Worth to mention that flash attributes are accessible from template engine by prefixing
 * attributes with <code>flash.</code>. Here is a <code>handlebars.java</code> example:
 * </p>
 *
 * <pre>{@code
 * {{#if flash.success}}
 *   {{flash.success}}
 * {{else}}
 *   Welcome!
 * {{/if}}
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR4
 */
public class FlashScope implements Jooby.Module {

  public static final String NAME = "flash";

  private Function<String, Map<String, String>> decoder = Cookie.URL_DECODER;

  private Function<Map<String, String>, String> encoder = Cookie.URL_ENCODER;

  private Optional<Cookie.Definition> cookie = Optional.empty();

  private String method = "*";

  private String path = "*";

  /**
   * Creates a new {@link FlashScope} and customize the flash cookie.
   *
   * @param cookie Cookie template for flash scope.
   */
  public FlashScope(final Cookie.Definition cookie) {
    this.cookie = Optional.of(requireNonNull(cookie, "Cookie required."));
  }

  /**
   * Creates a new {@link FlashScope}.
   */
  public FlashScope() {
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Config $cookie = conf.getConfig("flash.cookie");
    String cpath = $cookie.getString("path");
    boolean chttp = $cookie.getBoolean("httpOnly");
    boolean csecure = $cookie.getBoolean("secure");
    Cookie.Definition cookie = this.cookie
        .orElseGet(() -> new Cookie.Definition($cookie.getString("name")));

    // uses user provided or fallback to defaults
    cookie.path(cookie.path().orElse(cpath))
        .httpOnly(cookie.httpOnly().orElse(chttp))
        .secure(cookie.secure().orElse(csecure));

    env.router()
        .use(method, path, new FlashScopeHandler(cookie, decoder, encoder))
        .name("flash-scope");
  }

}
