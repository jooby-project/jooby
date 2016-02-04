package org.jooby.spec;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;

public class RouteSpecTest {

  public static interface Verifier {
    void verify();
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
      if (it.hasNext()) {
        fail(it.next().toString());
      }
    }
  }

  public static class RouteSpecItr implements Verifier {
    Iterator<RouteSpec> it;

    public RouteSpecItr(final List<RouteSpec> source) {
      it = source.iterator();
    }

    public RouteSpecItr next(final Consumer<RouteSpec> callback) {
      callback.accept(it.next());
      return this;
    }

    @Override
    public void verify() {
      if (it.hasNext()) {
        fail(it.next().toString());
      }
    }
  }

  private List<Verifier> routes = new ArrayList<>();

  private List<Verifier> params = new ArrayList<>();

  @After
  public void checkItr() {
    params.forEach(Verifier::verify);
    params.clear();
    //
    routes.forEach(Verifier::verify);
    routes.clear();
  }

  public RouteSpecItr routes(final List<RouteSpec> source) {
    RouteSpecItr itr = new RouteSpecItr(source);
    routes.add(itr);
    return itr;
  }

  public ParamItr params(final List<RouteParam> source) {
    ParamItr itr = new ParamItr(source);
    params.add(itr);
    return itr;
  }

}
