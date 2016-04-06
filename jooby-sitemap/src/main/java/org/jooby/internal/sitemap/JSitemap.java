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
package org.jooby.internal.sitemap;

import static java.util.Objects.requireNonNull;
import static javaslang.API.Case;
import static javaslang.API.Match;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.sitemap.WebPageProvider;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import cz.jiripinkas.jsitemapgenerator.WebPage;
import javaslang.Function1;

@SuppressWarnings("rawtypes")
public abstract class JSitemap<T extends JSitemap> implements Jooby.Module {

  protected static final String SITEMAP = "/sitemap.xml";

  static final Predicate<Route.Definition> NOT_ME = r -> {
    return !r.pattern().equals(SITEMAP);
  };

  static final Predicate<Route.Definition> GET = r -> r.method().equals("GET");

  private static final String SITEMAP_BASEURL = "sitemap.url";

  private String path;

  private Consumer<Binder> wpp;

  private Predicate<Route.Definition> filter = GET;

  private Optional<String> baseurl;

  public JSitemap(final String path, final Optional<String> baseurl, final WebPageProvider wpp) {
    this.path = path;
    this.baseurl = baseurl;
    this.wpp = binder -> binder
        .bind(Key.get(WebPageProvider.class, Names.named(path))).toInstance(wpp);
  }

  @SuppressWarnings("unchecked")
  public T filter(final Predicate<Route.Definition> filter) {
    this.filter = requireNonNull(filter, "Filter is required.");
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T with(final WebPageProvider wpp) {
    requireNonNull(wpp, "WebPageProvider is required.");
    this.wpp = binder -> binder.bind(Key.get(WebPageProvider.class, Names.named(path)))
        .toInstance(wpp);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T with(final Class<? extends WebPageProvider> wpp) {
    requireNonNull(wpp, "WebPageProvider is required.");
    this.wpp = binder -> binder.bind(Key.get(WebPageProvider.class, Names.named(path)))
        .to(wpp);
    return (T) this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    String baseurl = this.baseurl.orElseGet(() -> Match(conf.hasPath(SITEMAP_BASEURL)).of(
        Case(true, () -> conf.getString(SITEMAP_BASEURL)),
        Case(false, () -> {
          Config $ = conf.getConfig("application");
          return "http://" + $.getString("host") + ":" + $.getString("port") + $.getString("path");
        })));

    wpp.accept(binder);
    env.routes().get(path, new SitemapHandler(path, NOT_ME.and(filter), gen(baseurl)));
  }

  protected abstract Function1<List<WebPage>, String> gen(String baseurl);

}
