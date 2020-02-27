package io.jooby.openapi;

import io.jooby.internal.openapi.ParameterExt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class RouteArgumentIterator {

  final LinkedList<ParameterExt> arguments;

  final List<ParameterExt> processed = new ArrayList<>();

  public RouteArgumentIterator(List<ParameterExt> arguments) {
    this.arguments = new LinkedList<>(arguments);
  }

  public RouteArgumentIterator next(Consumer<ParameterExt> consumer) {
    if (arguments.size() > 0) {
      ParameterExt param = arguments.removeFirst();
      processed.add(param);
      consumer.accept(param);
    } else {
      if (processed.isEmpty()) {
        throw new NoSuchElementException("No parameters");
      } else {
        throw new NoSuchElementException("No more parameters, processed: " + processed);
      }
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
