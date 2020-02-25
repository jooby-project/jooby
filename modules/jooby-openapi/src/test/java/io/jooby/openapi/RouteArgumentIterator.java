package io.jooby.openapi;

import io.jooby.internal.openapi.ParameterExt;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class RouteArgumentIterator {

  final LinkedList<ParameterExt> arguments;

  public RouteArgumentIterator(List<ParameterExt> arguments) {
    this.arguments = new LinkedList<>(arguments);
  }

  public RouteArgumentIterator next(Consumer<ParameterExt> consumer) {
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
