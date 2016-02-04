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
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.google.inject.util.Types;

public class TypeCollector extends GenericVisitorAdapter<Type, Context> {

  @Override
  public Type visit(final ClassOrInterfaceType n, final Context ctx) {
    String name = name(n);
    Type type = ctx.resolveType(n, name)
        .orElseThrow(() -> new IllegalArgumentException("Type not found: " + name));
    List<Type> args = n.getTypeArgs().stream()
        .map(it -> it.accept(new TypeCollector(), ctx))
        .filter(it -> it != null)
        .collect(Collectors.toList());
    if (args.size() > 0) {
      type = Types.newParameterizedType(type, args.toArray(new Type[args.size()]));
    }
    return type;
  }

  private String name(ClassOrInterfaceType n) {
    LinkedList<String> name = new LinkedList<>();
    while (n != null) {
      name.addFirst(n.getName());
      n = n.getScope();
    }
    return name.stream().collect(Collectors.joining("."));
  }

  @Override
  public Type visit(final PrimitiveType n, final Context ctx) {
    Primitive type = n.getType();
    switch (type) {
      case Byte:
        return Byte.class;
      case Boolean:
        return boolean.class;
      case Char:
        return char.class;
      case Short:
        return short.class;
      case Int:
        return int.class;
      case Long:
        return long.class;
      case Float:
        return float.class;
      default:
        return double.class;
    }
  }

  @Override
  public Type visit(final BooleanLiteralExpr n, final Context arg) {
    return boolean.class;
  }

  @Override
  public Type visit(final StringLiteralExpr n, final Context arg) {
    return String.class;
  }

  @Override
  public Type visit(final CharLiteralExpr n, final Context arg) {
    return char.class;
  }

  @Override
  public Type visit(final DoubleLiteralExpr n, final Context arg) {
    return double.class;
  }

  @Override
  public Type visit(final IntegerLiteralExpr n, final Context arg) {
    return int.class;
  }

  @Override
  public Type visit(final LongLiteralExpr n, final Context arg) {
    return long.class;
  }

  @Override
  public Type visit(final ClassExpr n, final Context ctx) {
    return n.getType().accept(new TypeCollector(), ctx);
  }

  @Override
  public Type visit(final ReferenceType n, final Context ctx) {
    return n.getType().accept(new TypeCollector(), ctx);
  }

  @Override
  public Type visit(final VoidType n, final Context ctx) {
    return void.class;
  }

  @Override
  public Type visit(final WildcardType n, final Context ctx) {
    return null;
  }

}
