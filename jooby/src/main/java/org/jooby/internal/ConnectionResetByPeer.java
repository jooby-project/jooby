package org.jooby.internal;

import static javaslang.Predicates.instanceOf;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class ConnectionResetByPeer {

  public static boolean test(final Throwable cause) {
    return Optional.ofNullable(cause)
        .filter(instanceOf(IOException.class))
        .map(x -> x.getMessage())
        .filter(Objects::nonNull)
        .map(message -> message.toLowerCase().contains("connection reset by peer"))
        .orElse(false);
  }
}
