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
import java.util.Set;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import org.jooby.Jooby;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.google.common.collect.ImmutableSet;

public class AppCollector extends GenericVisitorAdapter<Node, Context> {

  private static final Set<String> APP = ImmutableSet
      .of(Jooby.class.getSimpleName(), Jooby.class.getName());

  public Node accept(final CompilationUnit unit, final Context ctx) {
    return unit.accept(this, ctx);
  }

  @Override
  public Node visit(final ObjectCreationExpr expr, final Context ctx) {
    /**
     * Inline like:
     *
     * Jooby app = new Jooby() {{
     * ...
     * }};
     */
    ClassOrInterfaceType type = expr.getType();
    if (APP.contains(type.getName())) {
      return expr;
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Node visit(final InitializerDeclaration expr, final Context ctx) {
    /**
     * extends + initializer
     *
     * class App extends Jooby {
     * {
     * ...
     * }
     * }
     */
    Node node = expr.getParentNode();
    if (node instanceof ClassOrInterfaceDeclaration) {
      List<ClassOrInterfaceType> extendList = ((ClassOrInterfaceDeclaration) node).getExtends();
      if (extendList.size() > 0) {
        Class type = (Class) extendList.get(0).accept(new TypeCollector(), ctx);
        while (type != Object.class) {
          if (type.getTypeName().equals("org.jooby.Jooby")) {
            return expr;
          }
          type = type.getSuperclass();
        }
      }
    }
    return null;
  }

  @Override
  public Node visit(ConstructorDeclaration expr, Context ctx) {
    /**
     * extends + initializer
     *
     * class App extends Jooby {
     * App() {
     * ...
     * }
     * }
     */
    Node node = expr.getParentNode();
    if (node instanceof ClassOrInterfaceDeclaration) {
      List<ClassOrInterfaceType> extendList = ((ClassOrInterfaceDeclaration) node).getExtends();
      if (extendList.size() > 0) {
        Class type = (Class) extendList.get(0).accept(new TypeCollector(), ctx);
        while (type != Object.class) {
          if (type.getTypeName().equals("org.jooby.Jooby")) {
            return expr;
          }
          type = type.getSuperclass();
        }
      }
    }
    return null;
  }

}
