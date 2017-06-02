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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralMinValueExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralMinValueExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.Sets;

public class LocalVariableCollector extends VoidVisitorAdapter<Context> {

  @SuppressWarnings("serial")
  private static class WorkDone extends RuntimeException {
  };

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Set<Node> visited;

  private Node node;

  private LocalStack vars;

  private LocalVariableCollector(final LocalStack parent, final Set<Node> visited) {
    vars = new LocalStack(parent);
    this.visited = visited;
  }

  public LocalVariableCollector() {
    this(null, Sets.newIdentityHashSet());
  }

  public LocalStack accept(final Node node, final Context ctx) {
    this.node = node;
    try {
      root(node).accept(this, ctx);
    } catch (WorkDone ex) {
    }
    return vars;
  }

  @Override
  public void visit(final VariableDeclarationExpr n, final Context ctx) {
    if (visited.add(n)) {
      try {
        Type type = n.getType().accept(new TypeCollector(), ctx);
        n.getVars().forEach(v -> {
          VariableDeclaratorId id = v.getId();
          vars.put(id.getName(), type);
        });
      } catch (IllegalArgumentException ex) {
        log.warn("Type not found {}", n, ex);
      }
    }
    super.visit(n, ctx);
  }

  protected void visitNode(final Node n, final Context ctx) {
    if (this.node == n) {
      throw new WorkDone();
    }
    if (visited.add(n) && n instanceof BodyDeclaration) {
      LocalVariableCollector collector = new LocalVariableCollector(vars, visited);
      n.accept(collector, ctx);
      this.vars = collector.vars;
    }
    visited.add(n);
  }

  @Override
  public void visit(final AnnotationDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final AnnotationMemberDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final ArrayAccessExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final ArrayCreationExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final ArrayInitializerExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final AssertStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final AssignExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final BinaryExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final BlockStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final BooleanLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final BreakStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final CastExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final CatchClause n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final CharLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ClassExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ClassOrInterfaceType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ConditionalExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ConstructorDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ContinueStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final DoStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final DoubleLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EmptyMemberDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EmptyStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EmptyTypeDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EnclosedExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EnumConstantDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final EnumDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ExplicitConstructorInvocationStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ExpressionStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final FieldAccessExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final FieldDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ForeachStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ForStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final IfStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ImportDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final InitializerDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final InstanceOfExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final IntegerLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final IntegerLiteralMinValueExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final LabeledStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final LambdaExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final LongLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final LongLiteralMinValueExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MarkerAnnotationExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MemberValuePair n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MethodCallExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MethodDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MethodReferenceExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final MultiTypeParameter n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final NameExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final NormalAnnotationExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final NullLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ObjectCreationExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final PackageDeclaration n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final Parameter n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final PrimitiveType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final QualifiedNameExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ReferenceType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ReturnStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final SingleMemberAnnotationExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final StringLiteralExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final SuperExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final SwitchEntryStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final SwitchStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final SynchronizedStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ThisExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final ThrowStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final TryStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final TypeDeclarationStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final TypeExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final TypeParameter n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final UnaryExpr n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final VariableDeclarator n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final VariableDeclaratorId n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final VoidType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final WhileStmt n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);

  }

  @Override
  public void visit(final WildcardType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final CompilationUnit n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final LineComment n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final BlockComment n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final JavadocComment n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
  }

  @Override
  public void visit(final UnknownType n, final Context ctx) {
    visitNode(n, ctx);
    super.visit(n, ctx);
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
