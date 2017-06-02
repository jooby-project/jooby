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

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class DependencyCollector extends VoidVisitorAdapter<Object> {

  private static class NameCollector extends GenericVisitorAdapter<String, Object> {
    @Override
    public String visit(final QualifiedNameExpr n, final Object arg) {
      return n.toStringWithoutComments();
    }

    @Override
    public String visit(final NameExpr n, final Object arg) {
      return n.getName();
    }

    @Override
    public String visit(final ClassOrInterfaceDeclaration n, final Object arg) {
      StringBuilder name = new StringBuilder();
      Node it = n.getParentNode();
      while (it instanceof ClassOrInterfaceDeclaration) {
        ClassOrInterfaceDeclaration superclass = (ClassOrInterfaceDeclaration) it;
        name.append(superclass.getName()).append(".");
        it = it.getParentNode();
      }
      if (it instanceof CompilationUnit) {
        if (name.length() == 0) {
          name.append(((CompilationUnit) it).getPackage().getName().toString()).append(".");
        } else {
          name.insert(0, ((CompilationUnit) it).getPackage().getName().toString() + ".");
        }
      }
      name.append(n.getName());
      return name.toString();
    }
  }

  private Set<String> packages = new LinkedHashSet<>();

  public Set<String> accept(final Node node) {
    packages.add("java.lang");
    if (node != null) {
      root(node).accept(this, null);
    }
    return packages;
  }

  @Override
  public void visit(final PackageDeclaration n, final Object arg) {
    packages.add(n.accept(new NameCollector(), arg));
  }

  @Override
  public void visit(final ImportDeclaration n, final Object arg) {
    packages.add(n.accept(new NameCollector(), arg));
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Object arg) {
    packages.add(n.accept(new NameCollector(), arg));
    super.visit(n, arg);
  }

  private Node root(final Node n) {
    Node prev = n;
    Node it = n;
    while (it != null) {
      prev = it;
      it = it.getParentNode();
    }
    return prev;
  }
}
