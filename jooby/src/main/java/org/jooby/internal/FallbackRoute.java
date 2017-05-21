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
package org.jooby.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

public class FallbackRoute implements RouteWithFilter {

  private Route.Filter filter;

  private String path;

  private String method;

  private String name;

  private List<MediaType> produces;

  public FallbackRoute(final String name, final String method, final String path,
      final List<MediaType> produces, final Route.Filter filter) {
    this.name = name;
    this.path = path;
    this.method = method;
    this.filter = filter;
    this.produces = produces;
  }

  @Override
  public String renderer() {
    return null;
  }

  @Override
  public String path() {
    return Route.unerrpath(path);
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public String pattern() {
    return Route.unerrpath(path);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Map<Object, String> vars() {
    return Collections.emptyMap();
  }

  @Override
  public List<MediaType> consumes() {
    return MediaType.ALL;
  }

  @Override
  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public Map<String, Object> attributes() {
    return Collections.emptyMap();
  }

  @Override
  public boolean glob() {
    return false;
  }

  @Override
  public String reverse(final Map<String, Object> vars) {
    return Route.unerrpath(path);
  }

  @Override
  public String reverse(final Object... values) {
    return Route.unerrpath(path);
  }

  @Override
  public Source source() {
    return Source.BUILTIN;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
    filter.handle(req, rsp, chain);
  }

  @Override
  public boolean apply(final String prefix) {
    return true;
  }

}
