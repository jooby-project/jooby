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

import java.util.Map;
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
 *   public Object flashScope(@Flash Map<String, String> flash) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object flashAttr(@Flash String foo) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object optionlFlashAttr(@Flash Optional<String> foo) {
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

  private String cookie = "flash";

  private String method = "*";

  private String path = "*";

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    env.routes().use(method, path, new FlashScopeHandler(cookie, decoder, encoder))
        .name("flash-scope");
  }

}
