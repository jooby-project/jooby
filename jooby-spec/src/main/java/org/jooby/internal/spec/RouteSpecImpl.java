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
package org.jooby.internal.spec;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.spec.RouteParam;
import org.jooby.spec.RouteResponse;
import org.jooby.spec.RouteSpec;

public class RouteSpecImpl extends SerObject implements RouteSpec {

  /** default serial. */
  private static final long serialVersionUID = 1L;

  public RouteSpecImpl(final Route.Definition route, final String summary, final String doc,
      final List<RouteParam> params, final RouteResponse rsp) {
    put("method", route.method());
    put("pattern", route.pattern());
    String name = route.name();
    if (!name.equals("/anonymous")) {
      put("name", name);
    }
    put("consumes", route.consumes().stream()
        .map(MediaType::name)
        .collect(Collectors.toList()));
    put("produces", route.consumes().stream()
        .map(MediaType::name)
        .collect(Collectors.toList()));
    put("summary", summary);
    put("doc", doc);
    put("params", params);
    put("response", rsp);
  }

  protected RouteSpecImpl() {
  }

  @Override
  public Optional<String> name() {
    return Optional.ofNullable(get("name"));
  }

  @Override
  public Optional<String> summary() {
    return Optional.ofNullable(get("summary"));
  }

  @Override
  public String method() {
    return get("method");
  }

  @Override
  public String pattern() {
    return get("pattern");
  }

  @Override
  public Optional<String> doc() {
    return Optional.ofNullable(get("doc"));
  }

  @Override
  public List<String> consumes() {
    return get("consumes");
  }

  @Override
  public List<String> produces() {
    return get("produces");
  }

  @Override
  public List<RouteParam> params() {
    return get("params");
  }

  @Override
  public RouteResponse response() {
    return get("response");
  }

  @Override
  public String toString() {
    int len = 80;
    Function<String, String> truncate = v -> {
      String s = v;
      if (s.length() > len) {
        s = s.substring(0, len - 3) + "...";
      }
      return s.replace("\n", "\\n");
    };
    StringBuilder buff = new StringBuilder();
    buff.append(method()).append(" ").append(pattern()).append("\n");
    name().ifPresent(v -> buff.append("  name: ").append(v).append("\n"));
    summary().ifPresent(v -> buff.append("  summary: ").append(truncate.apply(v)).append("\n"));
    doc().ifPresent(v -> buff.append("  doc: ").append(truncate.apply(v)).append("\n"));
    buff.append("  consumes: ").append(consumes()).append("\n");
    buff.append("  produces: ").append(produces()).append("\n");
    buff.append("  params: ").append("\n");
    params().forEach(p -> {
      buff.append("    ").append(p.name()).append("\n");
      buff.append("      paramType: ").append(p.paramType()).append("\n");
      buff.append("      type: ").append(p.type().getTypeName()).append("\n");
      Object pval = p.value();
      if (pval != null) {
        buff.append("      value: ").append(pval).append("\n");
      }
      p.doc().ifPresent(v -> buff.append("      doc: ").append(truncate.apply(v)).append("\n"));
    });
    buff.append("  response: ").append("\n");
    buff.append("    type: ").append(response().type().getTypeName()).append("\n");
    response().doc()
        .ifPresent(v -> buff.append("    doc: ").append(truncate.apply(v)).append("\n"));
    buff.setLength(buff.length() - 1);
    return buff.toString();
  }

}
