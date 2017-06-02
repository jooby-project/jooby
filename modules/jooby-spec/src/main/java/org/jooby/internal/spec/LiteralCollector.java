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
import java.util.LinkedList;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralMinValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralMinValueExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

public class LiteralCollector extends GenericVisitorAdapter<Object, Context> {

  @Override
  public Object visit(final ClassExpr n, final Context ctx) {
    return n.accept(new TypeCollector(), ctx);
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public Object visit(final FieldAccessExpr n, final Context ctx) {
    String name = name(n);
    Type type = ctx.resolveType(n, name)
        .orElseThrow(() -> new IllegalArgumentException("Type not found " + name));
    if (type instanceof Class) {
      Class etype = (Class) type;
      if (etype.isEnum()) {
        return Enum.valueOf(etype, n.getField());
      }
    }
    return super.visit(n, ctx);
  }

  @Override
  public Object visit(final BooleanLiteralExpr n, final Context arg) {
    return n.getValue();
  }

  @Override
  public Object visit(final StringLiteralExpr n, final Context arg) {
    return n.getValue();
  }

  @Override
  public Object visit(final CharLiteralExpr n, final Context arg) {
    return n.getValue().charAt(0);
  }

  @Override
  public Object visit(final DoubleLiteralExpr n, final Context arg) {
    return Double.parseDouble(n.getValue());
  }

  @Override
  public Object visit(final IntegerLiteralExpr n, final Context arg) {
    return Integer.parseInt(n.getValue());
  }

  @Override
  public Object visit(final IntegerLiteralMinValueExpr n, final Context arg) {
    return Integer.MIN_VALUE;
  }

  @Override
  public Object visit(final LongLiteralExpr n, final Context arg) {
    return Long.parseLong(n.getValue());
  }

  @Override
  public Object visit(final LongLiteralMinValueExpr n, final Context arg) {
    return Long.MIN_VALUE;
  }

  private String name(final FieldAccessExpr n) {
    LinkedList<String> name = new LinkedList<>();
    Expression it = n.getScope();
    while (it instanceof FieldAccessExpr) {
      name.addFirst(((FieldAccessExpr) it).getField());
      it = ((FieldAccessExpr) it).getScope();
    }
    name.addFirst(it.toStringWithoutComments());
    return name.stream().collect(Collectors.joining("."));
  }

}
