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
package org.jooby.internal.raml;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.ImmutableSet;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Route;
import org.jooby.spec.RouteParam;
import org.jooby.spec.RouteParamType;
import org.jooby.spec.RouteResponse;
import org.jooby.spec.RouteSpec;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;

public class RamlBuilder {
  /**
   * The parameter types that should be skipped
   */
  public static final Set<String> PARAM_TYPES_TO_SKIP = ImmutableSet.of(
    Route.Chain.class.getName(), org.jooby.Response.class.getName(), Request.class.getName()
  );

  private static class Resource {

    private Set<Resource> children = new LinkedHashSet<>();

    private String pattern;

    private List<RouteSpec> routes = new ArrayList<>();

    private String mediaType;

    public Resource(final String pattern, final String mediaType) {
      this.pattern = pattern;
      this.mediaType = mediaType;
    }

    List<RouteSpec> routes() {
      List<RouteSpec> routes = new ArrayList<>();
      routes.addAll(this.routes);
      for (Resource resource : children) {
        routes.addAll(resource.routes());
      }
      return routes;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof Resource) {
        return pattern.equals(((Resource) obj).pattern);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return pattern.hashCode();
    }

    @Override
    public String toString() {
      return toString(0);
    }

    private String toString(final int level) {
      List<RouteSpec> routes = this.routes;
      String pattern = this.pattern;
      Set<Resource> children = this.children;
      List<RouteSpec> deep = routes();
      if (deep.size() == 1) {
        routes = deep;
        List<String> snested = Splitter.on("/").trimResults().omitEmptyStrings()
            .splitToList(normalize(deep.get(0).pattern()));
        pattern = normalize("/"
            + snested.subList(level, snested.size()).stream().collect(Collectors.joining("/")));
        children = Collections.emptySet();
      }
      StringBuilder buff = new StringBuilder();
      String fpattern = pattern;
      buff.append(indent(level)).append(fpattern).append(":\n");
      Set<String> uriParamVisited = new HashSet<>();
      Set<String> visitedPaths = new HashSet<>();
      for (RouteSpec route : routes) {
        List<String> consumes = route.consumes().stream()
            .filter(t -> !t.equals("*/*"))
            .collect(Collectors.toList());
        List<String> produces = route.produces().stream()
            .filter(t -> !t.equals("*/*"))
            .collect(Collectors.toList());
        if (produces.isEmpty()) {
          produces.add(mediaType);
        }

        // uri params
        List<RouteParam> uriParams = route.params().stream()
            .filter(p -> p.paramType() == RouteParamType.PATH)
            .filter(p -> !uriParamVisited.contains(p.name()))
            .collect(Collectors.toList());
        if (uriParams.size() > 0) {
          buff.append(indent(level + 1)).append("uriParameters:\n");
          uriParams.forEach(p -> {
            uriParamVisited.add(p.name());
            buff.append(param(p, level + 1));
          });
        }
        // method
        route.summary().ifPresent(doc -> {
          if (visitedPaths.add(fpattern)) {
            buff.append(indent(level + 1)).append("description: ")
                .append(Doc.parse(doc, level + 3)).append("\n");
          }
        });
        buff.append(indent(level + 1)).append(route.method().toLowerCase()).append(":\n");
        route.doc().ifPresent(
            doc -> buff.append(indent(level + 2)).append("description: ")
                .append(Doc.parse(doc, level + 4)).append("\n"));

        // headers
        List<RouteParam> headers = route.params().stream()
            .filter(p -> p.paramType() == RouteParamType.HEADER)
            .collect(Collectors.toList());
        if (headers.size() > 0) {
          buff.append(indent(level + 2)).append("headers:\n");
          headers.forEach(p -> {
            buff.append(param(p, level + 2));
          });
        }

        // query params
        List<RouteParam> queryParams = route.params().stream()
            .filter(p -> p.paramType() == RouteParamType.QUERY)
            .filter(p -> !PARAM_TYPES_TO_SKIP.contains(p.type().getTypeName()) )
            .collect(Collectors.toList());
        if (queryParams.size() > 0) {
          buff.append(indent(level + 2)).append("queryParameters:\n");
          queryParams.forEach(p -> {
            buff.append(param(p, level + 2));
          });
        }

        // body params
        boolean hasFiles = route.params().stream()
            .filter(p -> p.paramType() == RouteParamType.FILE)
            .findFirst()
            .isPresent();
        List<RouteParam> bodyParams = route.params().stream()
            .filter(
                p -> p.paramType() == RouteParamType.BODY || p.paramType() == RouteParamType.FILE)
            .collect(Collectors.toList());
        if (bodyParams.size() > 0) {
          buff.append(indent(level + 2)).append("body:\n");
          bodyParams.forEach(p -> {
            List<String> cmtypes = consumes;
            if (hasFiles) {
              cmtypes = ImmutableList.of(MediaType.multipart.name());
            } else if (cmtypes.isEmpty()) {
              cmtypes = ImmutableList.of(mediaType);
            }
            cmtypes.forEach(t -> buff.append(indent(level + 3)).append(t).append(":\n"));
            int foffset = 0;
            if (cmtypes.contains(MediaType.form.name())
                || cmtypes.contains(MediaType.multipart.name())) {
              buff.append(indent(level + 4)).append("formParameters:\n");
              foffset = 1;
            }
            buff.append(param(p, level + foffset + 3));
          });
        }
        // responses
        RouteResponse rsp = route.response();
        if (rsp.type() != Object.class) {
          buff.append(indent(level + 2)).append("responses:\n");
          buff.append(indent(level + 3)).append(rsp.statusCode()).append(":\n");
          rsp.doc().ifPresent(doc -> buff.append(indent(level + 4)).append("description: ")
              .append(Doc.parse(doc, level + 4)).append("\n"));
          if (rsp.type() != void.class) {
            RamlType rspType = RamlType.parse(rsp.type());
            buff.append(indent(level + 4)).append("body").append(":\n");
            produces.stream().forEach(t -> buff.append(indent(level + 5)).append(t).append(":\n"));
            buff.append(indent(level + 6)).append("type: ").append(rspType.type()).append("\n");
          }
          Map<Integer, String> statusCodes = Maps.newLinkedHashMap(rsp.statusCodes());
          statusCodes.remove(rsp.statusCode());
          statusCodes.forEach((sc, msg) -> {
            buff.append(indent(level + 3)).append(sc).append(":\n");
            buff.append(indent(level + 4)).append("description: ").append(Doc.parse(msg, level + 8)).append("\n");
          });
        }
      }
      for (Resource child : children) {
        buff.append(child.toString(level + 1));
      }
      return buff.toString();
    }

    private CharSequence param(final RouteParam p, final int level) {
      StringBuilder buff = new StringBuilder();
      int offset;
      boolean body = p.paramType() == RouteParamType.BODY;
      if (!body) {
        buff.append(indent(level + 1)).append(p.name()).append(":\n");
        offset = 1;
      } else {
        offset = 0;
      }
      RamlType type = RamlType.parse(p.type());
      buff.append(indent(level + offset + 1)).append("type: ")
          .append(type.type())
          .append("\n");
      p.doc().ifPresent(doc -> buff.append(indent(level + offset + 1))
          .append("description: ")
          .append(Doc.parse(doc, level + offset + 2)).append("\n"));
      if (!body) {
        buff.append(indent(level + offset + 1)).append("required: ").append(!p.optional())
            .append("\n");
      }
      if (p.value() != null) {
        buff.append(indent(level + offset + 1)).append("default: ").append(p.value()).append("\n");
      }
      return buff;
    }

  }

  private static final Pattern VAR = Pattern.compile("\\:((?:[^/]+)+?)");

  private Config conf;

  @Inject
  public RamlBuilder(@Named("raml") final Config conf) {
    this.conf = conf;
  }

  public String build(final List<RouteSpec> routes) {
    StringBuilder buff = new StringBuilder();
    buff.append("#%RAML 1.0\n");
    conf.root()
        .forEach((n, v) -> buff.append(n).append(": ").append(v.unwrapped()).append("\n"));

    // types
    Set<RamlType> types = new LinkedHashSet<>();
    Consumer<Type> typeCollector = type -> {
      if (type != Object.class && type != void.class) {
        RamlType.parseAll(type).stream()
            .filter(t -> t.isObject() || t.isEnum())
            .forEach(types::add);
      }
    };
    routes.forEach(route -> {
      route.params()
        .stream()
        .filter(p -> !PARAM_TYPES_TO_SKIP.contains(p.type().getTypeName()) )
        .forEach(p -> {
        typeCollector.accept(p.type());
      });
      typeCollector.accept(route.response().type());
    });
    if (types.size() > 0) {
      buff.append("types:\n");
      types.forEach(t -> buff.append(t.toString(2)).append("\n"));
    }

    // resources
    tree(routes, conf.getString("mediaType")).forEach(buff::append);
    return buff.toString().trim();
  }

  private List<Resource> tree(final List<RouteSpec> routes, final String mediaType) {
    List<Resource> result = new ArrayList<>();
    Map<String, Resource> hash = new HashMap<>();
    for (RouteSpec route : routes) {
      String pattern = normalize(route.pattern());
      List<String> segments = Splitter.on('/')
          .trimResults()
          .omitEmptyStrings()
          .splitToList(pattern);
      String prev = "/";
      Resource root = null;
      for (int i = 0; i < segments.size(); i++) {
        String segment = segments.get(i);
        String it = prev + segment;
        Resource resource = hash.get(it);
        if (resource == null) {
          resource = new Resource("/" + segment, mediaType);
          if (i == 0) {
            root = resource;
            result.add(resource);
          } else {
            root.children.add(resource);
          }
          hash.put(it, resource);
        }
        root = resource;
        prev = it + "/";
      }
      hash.get(pattern).routes.add(route);
    }
    return result;
  }

  private static String indent(final int level) {
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < level * 2; i++) {
      buff.append(" ");
    }
    return buff.toString();
  }

  private static String normalize(final String pattern) {
    Matcher matcher = VAR.matcher(pattern);
    StringBuilder result = new StringBuilder();
    int end = 0;
    while (matcher.find()) {
      result.append(pattern, end, matcher.start());
      result.append("{").append(matcher.group(1)).append("}");
      end = matcher.end();
    }
    result.append(pattern, end, pattern.length());
    return result.toString();
  }

}
