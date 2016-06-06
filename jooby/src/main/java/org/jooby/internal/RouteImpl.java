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

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.List;
import java.util.Map;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.internal.mvc.MvcHandler;

import com.google.common.collect.ImmutableMap;

import javaslang.control.Option;

public class RouteImpl implements Route, Route.Filter {

  private static Map<Object, String> NO_VARS = ImmutableMap.of();

  private Definition route;

  private String path;

  private Map<Object, String> vars;

  private Filter filter;

  private List<MediaType> produces;

  private String method;

  private Source source;

  public static RouteImpl notFound(final String method, final String path,
      final List<MediaType> produces) {
    return fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        throw new Err(Status.NOT_FOUND, path);
      }
    }, method, path, "404", produces);
  }

  public static RouteImpl fromStatus(final Filter filter, final String method,
      final String path, final String name, final List<MediaType> produces) {
    return new RouteImpl(filter, new Route.Definition(method, path, filter)
        .name(name), method, path, produces, NO_VARS, null, Source.UNKNOWN) {
      @Override
      public boolean apply(final String filter) {
        return true;
      }
    };
  }

  public RouteImpl(final Filter filter, final Definition route, final String method,
      final String path, final List<MediaType> produces, final Map<Object, String> vars,
      final Mapper<?> mapper, final Source source) {
    this.filter = Option.of(mapper)
        .map(m -> Match(filter).of(
            Case(instanceOf(Route.OneArgHandler.class),
                f -> new MappedHandler((req, rsp) -> f.handle(req), mapper)),
            Case(instanceOf(Route.ZeroArgHandler.class),
                f -> new MappedHandler((req, rsp) -> f.handle(), mapper)),
            Case(instanceOf(MvcHandler.class), f -> {
              if (f.method().getReturnType() == void.class) {
                // ignore void results
                return filter;
              }
              return new MappedHandler((req, rsp) -> f.invoke(req, rsp), mapper);
            }),
            Case($(), filter)))
        .getOrElse(filter);
    this.route = route;
    this.method = method;
    this.produces = produces;
    this.path = path;
    this.vars = vars;
    this.source = source;
  }

  @Override
  public void handle(final Request request, final Response response, final Chain chain)
      throws Throwable {
    filter.handle(request, response, chain);
  }

  @Override
  public Map<String, Object> attributes() {
    return route.attributes();
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public String pattern() {
    return route.pattern().substring(route.pattern().indexOf('/'));
  }

  @Override
  public String name() {
    return route.name();
  }

  @Override
  public Map<Object, String> vars() {
    return vars;
  }

  @Override
  public List<MediaType> consumes() {
    return route.consumes();
  }

  @Override
  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public boolean glob() {
    return route.glob();
  }

  @Override
  public String reverse(final Map<String, Object> vars) {
    return route.reverse(vars);
  }

  @Override
  public String reverse(final Object... values) {
    return route.reverse(values);
  }

  @Override
  public Source source() {
    return source;
  }

  @Override
  public String toString() {
    return print();
  }

}
