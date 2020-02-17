package io.jooby.openapi;

import io.jooby.internal.openapi.Operation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RouteIterator {

  final LinkedList<Operation> routes;
  final boolean ignoreArguments;

  public RouteIterator(List<Operation> routes, boolean ignoreArguments) {
    this.routes = new LinkedList<>(routes);
    this.ignoreArguments = ignoreArguments;
  }

  public RouteIterator next(Consumer<Operation> consumer) {
    return next((route, args) -> consumer.accept(route));
  }

  public RouteIterator next(BiConsumer<Operation, RouteArgumentIterator> consumer) {
    if (routes.size() > 0) {
      Operation route = routes.removeFirst();
      RouteArgumentIterator args = new RouteArgumentIterator(route.getArguments());
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
