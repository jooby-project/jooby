package io.jooby.internal;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NonBlockingType {

  static final Set<String> types = new HashSet<>();

  static {
    types.add(CompletableFuture.class.getName());
  }

  public static boolean isNonBlocking(Type type) {
    return types.contains(type.getTypeName());
  }
}
