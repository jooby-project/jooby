package io.jooby.openapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class AssertIterator<T> {
  final LinkedList<T> items;

  final List<T> processed = new ArrayList<>();

  public AssertIterator(Collection<T> items) {
    this.items = new LinkedList<>(items);
  }

  public AssertIterator<T> next(Consumer<T> consumer) {
    if (items.size() > 0) {
      T item = items.removeFirst();
      processed.add(item);
      consumer.accept(item);
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
    if (!items.isEmpty()) {
      String message = "Ignored argument(s): " + items;
      throw new IllegalStateException(message);
    }
  }
}
