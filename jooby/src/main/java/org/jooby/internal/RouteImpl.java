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

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.Verb;

public class RouteImpl implements Route, Route.Filter {

  private Verb verb;

  private String path;

  private String pattern;

  private String name;

  private Map<String, String> vars;

  private List<MediaType> consumes;

  private List<MediaType> produces;

  private Filter filter;

  public static RouteImpl notFound(final Verb verb, final String path,
      final List<MediaType> produces) {
    return fromStatus((req, rsp, chain) -> {
      if (!rsp.committed() && !rsp.status().isPresent()) {
        throw new Err(Status.NOT_FOUND, path);
      }
    }, verb, path, Status.NOT_FOUND, produces);
  }

  public static RouteImpl fromStatus(final Filter filter, final Verb verb,
      final String path, final Status status, final List<MediaType> produces) {
    return new RouteImpl(filter, verb, path, path, status.value() + "", Collections.emptyMap(),
        MediaType.ALL, produces);
  }

  public RouteImpl(final Filter filter, final Verb verb, final String path,
      final String pattern, final String name, final Map<String, String> vars,
      final List<MediaType> consumes, final List<MediaType> produces) {
    this.filter = filter;
    this.verb = verb;
    this.path = path;
    this.pattern = pattern;
    this.name = name;
    this.vars = vars;
    this.consumes = consumes;
    this.produces = produces;
  }

  @Override
  public void handle(final Request request, final Response response, final Chain chain)
      throws Exception {
    filter.handle(request, response, chain);
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public Verb verb() {
    return verb;
  }

  @Override
  public String pattern() {
    return pattern.substring(pattern.indexOf('/'));
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Map<String, String> vars() {
    return vars;
  }

  @Override
  public List<MediaType> consumes() {
    return consumes;
  }

  @Override
  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(verb()).append(" ").append(path()).append("\n");
    buffer.append("  pattern: ").append(pattern()).append("\n");
    buffer.append("  name: ").append(name()).append("\n");
    buffer.append("  vars: ").append(vars()).append("\n");
    buffer.append("  consumes: ").append(consumes()).append("\n");
    buffer.append("  produces: ").append(produces()).append("\n");
    return buffer.toString();
  }

}
