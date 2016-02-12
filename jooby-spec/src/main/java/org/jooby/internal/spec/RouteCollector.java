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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.spec;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooby.Route;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Maps;

public class RouteCollector extends VoidVisitorAdapter<Context> {

  private List<Map.Entry<Object, Node>> nodes = new ArrayList<>();

  private boolean script;

  private Consumer<String> owners;

  private Map<String, Node> vars = new HashMap<>();

  private RouteCollector(final boolean script, final Consumer<String> owners) {
    this.script = script;
    this.owners = owners;
  }

  public RouteCollector(final Consumer<String> owners) {
    this(true, owners);
  }

  public RouteCollector() {
    this(true, owner -> {
    });
  }

  public List<Entry<Object, Node>> accept(final Node node, final Context ctx) {
    node.accept(this, ctx);
    return nodes;
  }

  @Override
  public void visit(final VariableDeclarator n, final Context ctx) {
    vars.put(n.getId().getName(), n.getInit());
  }

  @Override
  public void visit(final MethodDeclaration m, final Context ctx) {
    if (!script) {
      boolean mvc = m.getAnnotations().stream()
          .map(it -> it.getName().getName())
          .filter(Route.METHODS::contains)
          .findFirst()
          .isPresent();
      if (mvc) {
        nodes.add(Maps.immutableEntry(m, m.getBody()));
      }
    }
  }

  @Override
  public void visit(final MethodCallExpr n, final Context ctx) {
    if (script) {
      Type mvcClass = useMvc(n, ctx);
      if (mvcClass != null) {
        mvcRoutes(n, mvcClass, ctx);
      } else {
        Type appType = importApp(n, ctx);
        if (appType != null) {
          importRoutes(appType, ctx);
        } else {
          List<MethodCallExpr> routes = routes(n, ctx);
          for (MethodCallExpr route : routes) {
            Optional<LambdaExpr> lambda = route.getArgs().stream()
                .map(it -> handler(it, ctx))
                .filter(it -> it != null)
                .findFirst();
            this.nodes.add(Maps.immutableEntry(route, lambda.get()));
          }
        }
      }
    }
  }

  private void importRoutes(final Type type, final Context ctx) {
    // compiled or parse?
    List<Map.Entry<Object, Node>> result = ctx.parseSpec(type).map(specs -> {
      List<Map.Entry<Object, Node>> nodes = new ArrayList<>();
      specs.forEach(spec -> nodes.add(Maps.immutableEntry(spec, null)));
      return nodes;
    }).orElseGet(() -> ctx.parse(type)
        .map(unit -> new RouteCollector(true, owners).accept(unit, ctx))
        .orElse(Collections.emptyList()));
    owners.accept(type.getTypeName());
    this.nodes.addAll(result);
  }

  private void mvcRoutes(final Node n, final Type type, final Context ctx) {
    List<Map.Entry<Object, Node>> result = ctx.parse(type)
        .map(unit -> new RouteCollector(false, owners).accept(unit, ctx))
        .orElse(Collections.emptyList());
    owners.accept(type.getTypeName());
    nodes.addAll(result);
  }

  private Type useMvc(final MethodCallExpr n, final Context ctx) {
    if ("use".equals(n.getName())) {
      List<Expression> args = n.getArgs();
      if (args.size() == 1) {
        Expression arg = args.get(0);
        if (arg instanceof ClassExpr) {
          return arg.accept(new TypeCollector(), ctx);
        }
      }
    }
    return null;
  }

  private Type importApp(final MethodCallExpr n, final Context ctx) {
    Function<Expression, Type> type = expr -> {
      if (expr instanceof ObjectCreationExpr) {
        ClassOrInterfaceType t = ((ObjectCreationExpr) expr).getType();
        Optional<java.lang.reflect.Type> resolved = ctx.resolveType(n, t.toStringWithoutComments());
        if (resolved.isPresent()) {
          Type c = resolved.get();
          if (isJooby(c)) {
            return c;
          }
        }
      }
      return null;
    };

    if ("use".equals(n.getName())) {
      List<Expression> args = n.getArgs();
      if (args.size() == 2) {
        return type.apply(args.get(1));
      }
      if (args.size() == 1) {
        return type.apply(args.get(0));

      }
    }
    return null;
  }

  private boolean isJooby(final Type type) {
    Type t = type;
    while (t instanceof Class) {
      @SuppressWarnings("rawtypes")
      Class c = (Class) t;
      if (c.getTypeName().equals("org.jooby.Jooby")) {
        return true;
      }
      t = c.getSuperclass();
    }
    return false;
  }

  private List<MethodCallExpr> routes(final MethodCallExpr expr, final Context ctx) {
    LinkedList<MethodCallExpr> expressions = new LinkedList<>();

    Expression it = expr;
    while (it instanceof MethodCallExpr) {
      MethodCallExpr local = (MethodCallExpr) it;
      String name = local.getName();
      int n = 0;
      if (Route.METHODS.contains(name.toUpperCase())) {
        n = route(local, ctx);
      } else if (name.equals("use")) {
        n = route(local, ctx);
      } else if (name.equals("all") && AST.scopeOf(local).getName().equals("use")) {
        n = route(local, ctx);
      }
      while (n > 0) {
        expressions.addFirst(local);
        n -= 1;
      }
      it = local.getScope();
    }
    return expressions;
  }

  private int route(final MethodCallExpr expr, final Context ctx) {
    List<Expression> args = expr.getArgs();
    if (args.size() == 1) {
      if (handler(args.get(0), ctx) != null) {
        return 1;
      }
    }
    // method(path, [path1, path2], handler());
    if (args.size() < 5) {
      // method(path, lambda)
      Set<Type> types = new LinkedHashSet<>();
      for (int i = 0; i < args.size() - 1; i++) {
        types.add(args.get(i).accept(new TypeCollector(), ctx));
      }
      boolean str = types.size() == 1 && types.contains(String.class);
      if (str && handler(args.get(args.size() - 1), ctx) != null) {
        return args.size() - 1;
      }
    }
    return 0;
  }

  private LambdaExpr handler(final Expression expr, final Context ctx) {
    Node node = vars.getOrDefault(expr.toStringWithoutComments(), expr);
    return node.accept(new GenericVisitorAdapter<LambdaExpr, Context>() {
      @Override
      public LambdaExpr visit(final LambdaExpr n, final Context ctx) {
        return n;
      }
    }, ctx);
  }

}
