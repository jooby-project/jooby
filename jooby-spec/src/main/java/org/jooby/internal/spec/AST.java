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

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class AST {

  public static MethodCallExpr scopeOf(final MethodCallExpr n) {
    MethodCallExpr prev = n;
    Expression it = n.getScope();
    while (it instanceof MethodCallExpr) {
      prev = (MethodCallExpr) it;
      it = prev.getScope();
    }
    return prev;
  }

}
