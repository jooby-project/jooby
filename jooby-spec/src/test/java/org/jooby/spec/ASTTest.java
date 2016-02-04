package org.jooby.spec;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.internal.spec.Context;
import org.jooby.internal.spec.ContextImpl;
import org.jooby.internal.spec.SourceResolver;
import org.jooby.internal.spec.SourceResolverImpl;
import org.jooby.internal.spec.TypeResolverImpl;
import org.junit.After;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.common.base.Joiner;

public class ASTTest {

  public static interface Verifier {
    void verify();
  }

  public static class Itr<K, V> implements Verifier {
    Iterator<Entry<K, V>> it;

    public Itr(final Map<K, V> source) {
      it = source.entrySet().iterator();
    }

    public Itr<K, V> script(final BiConsumer<MethodCallExpr, LambdaExpr> callback) {
      Entry<K, V> next = it.next();
      callback.accept((MethodCallExpr) next.getKey(), (LambdaExpr) next.getValue());
      return this;
    }

    public Itr<K, V> mvc(final BiConsumer<MethodDeclaration, BlockStmt> callback) {
      Entry<K, V> next = it.next();
      callback.accept((MethodDeclaration) next.getKey(), (BlockStmt) next.getValue());
      return this;
    }

    @Override
    public void verify() {
      assertFalse(it.hasNext());
    }
  }

  public static class ParamItr implements Verifier {
    Iterator<RouteParam> it;

    public ParamItr(final List<RouteParam> source) {
      it = source.iterator();
    }

    public ParamItr next(final Consumer<RouteParam> callback) {
      callback.accept(it.next());
      return this;
    }

    @Override
    public void verify() {
      assertFalse(it.hasNext());
    }
  }

  private List<Verifier> itrList = new ArrayList<>();

  @After
  public void checkItr() {
    itrList.forEach(Verifier::verify);
    itrList.clear();
  }

  public CompilationUnit source(final String... source) throws ParseException {
    StringReader reader = new StringReader(Joiner.on("\n").join(source));
    return JavaParser.parse(reader, true);
  }

  public Expression expr(final String... source) throws ParseException {
    return JavaParser.parseExpression(Joiner.on("\n").join(source));
  }

  public Context ctx() {
    SourceResolver sourceResolver = new SourceResolverImpl(
        new File(System.getProperty("user.dir")).toPath());
    return new ContextImpl(new TypeResolverImpl(getClass().getClassLoader()), sourceResolver);
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  public Itr<MethodCallExpr, LambdaExpr> routes(final Map source) {
    Itr itr = new Itr<>(source);
    itrList.add(itr);
    return itr;
  }

  public ParamItr params(final List<RouteParam> source) {
    ParamItr itr = new ParamItr(source);
    itrList.add(itr);
    return itr;
  }

}
