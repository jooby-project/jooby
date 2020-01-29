package io.jooby.openapi;

import io.jooby.internal.openapi.RouteDescriptor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class RouteIterator {

  final LinkedList<RouteDescriptor> routes;

  public RouteIterator(List<RouteDescriptor> routes) {
    this.routes = new LinkedList<>(routes);
  }

  public RouteIterator next(Consumer<RouteDescriptor> consumer) {
    if (routes.size() > 0) {
      consumer.accept(routes.removeFirst());
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
      x.setStackTrace(Arrays.copyOfRange(x.getStackTrace(), 1 , x.getStackTrace().length));
      throw x;
    }
  }
}
