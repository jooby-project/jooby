package io.jooby.openapi;

import io.jooby.internal.openapi.OperationExt;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RouteIterator {

  final LinkedList<OperationExt> routes;
  final boolean ignoreArguments;

  public RouteIterator(List<OperationExt> routes, boolean ignoreArguments) {
    this.routes = new LinkedList<>(routes);
    this.ignoreArguments = ignoreArguments;
  }

  public RouteIterator next(Consumer<OperationExt> consumer) {
    return next((route, args) -> consumer.accept(route));
  }

  public RouteIterator next(BiConsumer<OperationExt, RouteArgumentIterator> consumer) {
    if (routes.size() > 0) {
      OperationExt route = routes.removeFirst();
      List params = Optional.ofNullable(route.getParameters()).orElse(Collections.emptyList());
      RouteArgumentIterator args = new RouteArgumentIterator(params);
      consumer.accept(route, args);
      if (!ignoreArguments) {
        args.verify();
      }
    } else {
      throw new NoSuchElementException("No more routes");
    }
    return this;
  }

  public void verify() {
    if (!routes.isEmpty()) {
      String message = "Ignored routes: " + routes;
      routes.clear();
      IllegalStateException x = new IllegalStateException(message);
      x.setStackTrace(Arrays.copyOfRange(x.getStackTrace(), 1, x.getStackTrace().length));
      throw x;
    }
  }
}
