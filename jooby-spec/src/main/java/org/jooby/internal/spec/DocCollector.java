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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooby.Status;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Splitter;

public class DocCollector extends VoidVisitorAdapter<Context> {

  private static final Pattern SPLITTER = Pattern.compile("\\s+\\*");

  private static final Pattern PARAM = Pattern.compile("(@param)\\s+([^\\s]+)([^@]+)");

  private static final String RETURNS = "@return";

  private static final Pattern CODE = Pattern
      .compile("<code>\\s*(\\d+)\\s*(=\\s*([^<]+))?\\s*</code>");

  private static final Pattern TYPE = Pattern.compile("\\{@link\\s+([^\\}]+)\\}");

  private Map<String, Object> doc = new HashMap<>();

  public Map<String, Object> accept(final Node node, final String method, final Context ctx) {
    node.accept(this, ctx);
    if (!doc.containsKey("@statusCodes")) {
      Map<Object, Object> codes = new LinkedHashMap<>();
      Status status = Status.OK;
      if ("DELETE".equals(method)) {
        status = Status.NO_CONTENT;
      }
      codes.put(status.value(), status.reason());
      doc.put("@statusCodes", codes);
    }
    return doc;
  }

  @Override
  public void visit(final MethodCallExpr n, final Context ctx) {
    Map<String, Object> doc = doc(n, ctx);
    if (doc != null) {
      this.doc.putAll(doc);
      this.doc.put("@summary", summary(n, ctx));
    }
  }

  @Override
  public void visit(final MethodDeclaration m, final Context ctx) {
    ClassOrInterfaceDeclaration clazz = clazz(m);
    Map<String, Object> doc = doc(m, ctx);
    if (doc != null) {
      this.doc.putAll(doc);
      this.doc.put("@summary", doc(clazz, ctx).get("@text"));
    }
  }

  private ClassOrInterfaceDeclaration clazz(final MethodDeclaration method) {
    Node node = method.getParentNode();
    while (!(node instanceof ClassOrInterfaceDeclaration)) {
      node = node.getParentNode();
    }
    return (ClassOrInterfaceDeclaration) node;
  }

  private Map<String, Object> doc(final MethodCallExpr expr, final Context ctx) {
    if (expr.getScope() == null) {
      return doc(expr.getParentNode(), ctx);
    } else {
      List<Expression> args = expr.getArgs();
      if (args.size() > 0) {
        return doc(args.get(0), ctx);
      }
    }
    return null;
  }

  private Map<String, Object> doc(final Node node, final Context ctx) {
    Map<String, Object> hash = new HashMap<>();
    Comment comment = node.getComment();
    if (comment != null) {
      String doc = comment.getContent().trim();
      String clean = Splitter.on(SPLITTER)
          .trimResults()
          .omitEmptyStrings()
          .splitToList(doc)
          .stream()
          .map(l -> l.charAt(0) == '*' ? l.substring(1).trim() : l)
          .collect(Collectors.joining("\n"));
      int at = clean.indexOf('@');
      String text = at == 0 ? null : (at > 0 ? clean.substring(0, at) : clean).trim();
      Map<Integer, String> codes = Collections.emptyMap();
      String tail = clean.substring(Math.max(0, at));
      // params
      Matcher pmatcher = PARAM.matcher(tail);
      while (pmatcher.find()) {
        hash.put(pmatcher.group(2).trim(), pmatcher.group(3).trim());
      }
      // returns
      String returnText = returnText(tail);
      codes = new LinkedHashMap<>();
      if (returnText != null) {
        hash.put("@return", returnText);
        Matcher cmatcher = CODE.matcher(returnText);
        while (cmatcher.find()) {
          Status status = Status.valueOf(Integer.parseInt(cmatcher.group(1).trim()));
          String message = Optional.ofNullable(cmatcher.group(3)).orElse(status.reason()).trim();
          codes.put(status.value(), message);
        }

        Matcher tmatcher = TYPE.matcher(returnText);
        if (tmatcher.find()) {
          ctx.resolveType(node, tmatcher.group(1).trim()).ifPresent(t -> hash.put("@type", t));
        }
      }
      hash.put("@statusCodes", codes);
      hash.put("@text", text);
    }
    return hash;
  }

  private String returnText(final String doc) {
    int retIdx = doc.indexOf(RETURNS);
    if (retIdx >= 0) {
      String ret = doc.substring(retIdx + RETURNS.length()).trim();
      ret = Splitter.on(Pattern.compile("[^\\{]@[a-zA-Z]"))
          .trimResults().omitEmptyStrings()
          .splitToList(ret)
          .stream()
          .findFirst()
          .get();

      return ret;
    }
    return null;
  }

  private String summary(final MethodCallExpr it, final Context ctx) {
    return usePath(it)
        .map(use -> {
          Node node = use;
          while (!(node instanceof ExpressionStmt)) {
            node = node.getParentNode();
          }
          return node == null ? null : (String) doc(node, ctx).get("@text");
        }).orElse(null);
  }

  private Optional<Node> usePath(final MethodCallExpr it) {
    MethodCallExpr expr = AST.scopeOf(it);
    String name = expr.getName();
    List<Expression> args = expr.getArgs();
    if (name.equals("use") && args.size() == 1 && args.get(0) instanceof StringLiteralExpr) {
      return Optional.of(expr);
    }
    return Optional.empty();
  }

}
