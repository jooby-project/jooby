package io.jooby.openapi;

import io.jooby.internal.openapi.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class RouteArgumentIterator {

  final LinkedList<Parameter> arguments;

  public RouteArgumentIterator(List<Parameter> arguments) {
    this.arguments = new LinkedList<>(arguments);
  }

  public RouteArgumentIterator next(Consumer<Parameter> consumer) {
    if (arguments.size() > 0) {
      consumer.accept(arguments.removeFirst());
    } else {
      throw new NoSuchElementException("No more arguments");
    }
    return this;
  }

  public void verify() {
    if (!arguments.isEmpty()) {
      String message = "Ignored argument(s): " + arguments;
      throw new IllegalStateException(message);
    }
  }
}
