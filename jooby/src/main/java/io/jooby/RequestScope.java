/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Thread-Local request scope implementation useful for save/store request attribute and access to
 * them using a static way.
 *
 * <p>This is part of public API but usage must be keep to minimum.
 *
 * @author edgar
 */
public final class RequestScope {

  private static final ThreadLocal<Map<Object, Object>> CONTEXT_TL = new ThreadLocal<>();

  private RequestScope() {}

  /**
   * Check to see if there is already a value associated with the current thread for the given key.
   *
   * @param key The key against which to check for a given value within the current thread.
   * @return True if there is currently a session bound.
   */
  public static boolean hasBind(@NonNull Object key) {
    return get(key) != null;
  }

  /**
   * Binds the given value to the current context for its key.
   *
   * @param key The key to be bound.
   * @param value The value to be bound.
   * @param <T> Bind type.
   * @return Any previously bound session (should be null in most cases).
   */
  public static @Nullable <T> T bind(@NonNull Object key, @NonNull T value) {
    return (T) threadMap(true).put(key, value);
  }

  /**
   * Unbinds the session (if one) current associated with the context for the given session.
   *
   * @param key The factory for which to unbind the current session.
   * @param <T> Bind type.
   * @return The bound session if one, else null.
   */
  public static @Nullable <T> T unbind(@NonNull Object key) {
    var contextMap = threadMap();
    T existing = null;
    if (contextMap != null) {
      existing = (T) contextMap.remove(key);
      doCleanup();
    }
    return existing;
  }

  /**
   * Get a previously bind value for the given key or <code>null</code>.
   *
   * @param key Key.
   * @param <T> Object type.
   * @return Binded value or <code>null</code>.
   */
  public static @Nullable <T> T get(@NonNull Object key) {
    var contextMap = threadMap();
    if (contextMap == null) {
      return null;
    } else {
      return (T) contextMap.get(key);
    }
  }

  /**
   * Exposes thread local state. Internal usage only (don't use it).
   *
   * @return Exposes thread local state. Internal usage only (don't use it).
   */
  public static ThreadLocal<Map<Object, Object>> threadLocal() {
    return CONTEXT_TL;
  }

  private static Map<Object, Object> threadMap() {
    return threadMap(false);
  }

  private static Map<Object, Object> threadMap(boolean createMap) {
    var contextMap = CONTEXT_TL.get();
    if (contextMap == null && createMap) {
      contextMap = new HashMap<>();
      CONTEXT_TL.set(contextMap);
    }
    return contextMap;
  }

  private static void doCleanup() {
    var ctx = threadMap(false);
    if (ctx != null) {
      if (ctx.isEmpty()) {
        CONTEXT_TL.remove();
      }
    }
  }
}
