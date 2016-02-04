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
import java.util.List;
import java.util.Map;

import org.jooby.spec.RouteResponse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ResponseTypeCollector extends VoidVisitorAdapter<Context> {

  private Type type;

  private List<String> args = new ArrayList<>();

  public RouteResponse accept(final Node node, final Context ctx, final Type retType,
      final String doc, final Map<Integer, String> codes) {
    this.type = retType;
    boolean lambda = node instanceof LambdaExpr;
    if (lambda) {
      ((LambdaExpr) node).getParameters()
          .forEach(p -> args.add(p.getId().toStringWithoutComments()));
    }
    if (this.type == null) {
      node.accept(this, ctx);
      if (type == null && lambda) {
        LambdaExpr expr = (LambdaExpr) node;
        if (expr.getChildrenNodes().size() == 1) {
          type = expr.getChildrenNodes().get(0).accept(new TypeCollector(), ctx);
        }
      }
    }
    return new RouteResponseImpl(this.type == null ? Object.class : this.type, doc, codes);
  }

  @Override
  public void visit(final MethodCallExpr n, final Context ctx) {
    if (args.size() > 1) {
      // req, rsp
      String var = scope(n);
      if (args.get(1).equals(var) && n.getName().equals("send")) {
        Type type = type(n.getArgs().get(0), ctx);
        this.type = type;
      }
    }
  }

  @Override
  public void visit(final ReturnStmt n, final Context ctx) {
    this.type = type(n.getExpr(), ctx);
  }

  private String scope(final MethodCallExpr n) {
    Expression scope = n.getScope();
    while (scope != null && scope instanceof MethodCallExpr) {
      scope = ((MethodCallExpr) scope).getScope();
    }
    return ((NameExpr) scope).getName();
  }

  private Type type(final Expression expr, final Context ctx) {
    LocalStack vars = new LocalVariableCollector().accept(expr, ctx);
    Type type = vars.get(expr.toStringWithoutComments());
    if (type == null) {
      type = expr.accept(new TypeCollector(), ctx);
    }
    return type;
  }

}
