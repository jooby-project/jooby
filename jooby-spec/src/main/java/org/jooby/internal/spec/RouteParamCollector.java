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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.spec.RouteParam;
import org.jooby.spec.RouteParamType;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Maps;
import com.google.inject.util.Types;

public class RouteParamCollector extends VoidVisitorAdapter<Context> {

  private static final String BODY = "<body>";

  private List<RouteParam> params = new ArrayList<>();

  private Set<String> names = new LinkedHashSet<>();

  private Map<String, Object> doc;

  private String method;

  private String pattern;

  public RouteParamCollector(final Map<String, Object> doc, final String method,
      final String pattern) {
    this.doc = doc;
    this.method = method;
    this.pattern = pattern;
  }

  public RouteParamCollector() {
    this(Collections.emptyMap(), "", "");
  }

  public List<RouteParam> accept(final Node node, final Context ctx) {
    if (node instanceof LambdaExpr) {
      ((LambdaExpr) node).getParameters().forEach(p -> {
        names.add(p.getId().toStringWithoutComments());
      });
    }
    node.accept(this, ctx);
    return params;
  }

  @Override
  public void visit(final MethodCallExpr n, final Context ctx) {
    List<MethodCallExpr> call = call(n);
    if (call.size() > 0) {
      MethodCallExpr cparam = call.get(0);
      String name = cparam.getArgs().stream()
          .findFirst()
          .map(it -> ((StringLiteralExpr) it).getValue())
          .orElse(BODY);
      Entry<Type, Object> typeDef = type(call.get(1), ctx);
      String doc = (String) this.doc.get(name.equals(BODY) ? "body" : name);
      params.add(new RouteParamImpl(name, typeDef.getKey(), type(name), typeDef.getValue(), doc));
    }
  }

  private RouteParamType type(final String name) {
    if (name.equals("<body>")) {
      return RouteParamType.BODY;
    } else if (pattern.indexOf(":" + name) >= 0 || pattern.indexOf("{" + name + "}") >= 0) {
      return RouteParamType.PATH;
    } else {
      if (method.equals("POST")) {
        return RouteParamType.FORM;
      } else {
        return RouteParamType.QUERY;
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private Entry<Type, Object> type(final MethodCallExpr expr, final Context ctx) {
    String name = expr.getName();
    Type type = null;
    switch (name) {
      case "charValue": {
        type = char.class;
      }
        break;
      case "byteValue": {
        type = byte.class;
      }
        break;
      case "booleanValue": {
        type = boolean.class;
      }
        break;
      case "shortValue": {
        type = short.class;
      }
        break;
      case "intValue": {
        type = int.class;
      }
        break;
      case "longValue": {
        type = long.class;
      }
        break;
      case "floatValue": {
        type = float.class;
      }
        break;
      case "doubleValue": {
        type = double.class;
      }
        break;
      case "value": {
        type = String.class;
      }
        break;
      case "toList": {
        type = List.class;
      }
        break;
      case "toSet": {
        type = Set.class;
      }
        break;
      case "toSortedSet": {
        type = SortedSet.class;
      }
        break;
      case "toOptional": {
        type = Optional.class;
      }
        break;
      case "toUpload": {
        type = ctx.resolveType(expr, "org.jooby.Upload").get();
      }
        break;
    }
    Object defaultValue = null;
    List<Expression> args = expr.getArgs();
    if (args.size() > 0) {
      Expression arg = args.get(0);
      Object result = arg.accept(new LiteralCollector(), ctx);
      if (result instanceof Type) {
        if (type == null) {
          type = (Type) result;
        } else {
          type = Types.newParameterizedType(type, (Type) result);
        }
      } else if (result instanceof Enum) {
        Enum e = (Enum) result;
        type = e.getDeclaringClass();
        defaultValue = e.name();
      } else {
        if (result != null) {
          defaultValue = result;
        } else {
          Map<String, Object> vals = new StaticValueCollector().accept(expr, ctx);
          defaultValue = arg.toStringWithoutComments();
          defaultValue = vals.getOrDefault(defaultValue, defaultValue);
        }
      }
    } else if (name.startsWith("to") && name.length() > 2 && !name.equals("toUpload")) {
      type = Types.newParameterizedType(type, String.class);
    }
    return Maps.immutableEntry(type, defaultValue);
  }

  private List<MethodCallExpr> call(final MethodCallExpr n) {
    LinkedList<MethodCallExpr> call = new LinkedList<>();
    Expression it = n;
    Expression prev = it;
    while (it instanceof MethodCallExpr) {
      MethodCallExpr local = (MethodCallExpr) it;
      call.addFirst(local);
      prev = it;
      it = local.getScope();
    }
    // req.
    it = it == null ? prev : it;
    if (names.contains(it.toStringWithoutComments())) {
      // param(id).value
      if (call.size() == 2) {
        if ("param".equals(call.get(0).getName())) {
          List<Expression> args = call.get(0).getArgs();
          if (args.size() == 1 && args.get(0) instanceof StringLiteralExpr) {
            return call;
          }
        } else if ("body".equals(call.get(0).getName())) {
          List<Expression> args = call.get(0).getArgs();
          if (args.size() == 0) {
            return call;
          }
        }
      }
    }
    return Collections.emptyList();
  }

}
