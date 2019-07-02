/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-Local request scope implementation useful for save/store request attribute and access
 * to them using a static way.
 *
 * This is part of public API but usage must be keep to minimum.
 *
 * @author edgar
 */
public final class RequestScope {

  private static final ThreadLocal<Map<Object, Object>> CONTEXT_TL = new ThreadLocal<>();

  private RequestScope() {
  }

  /**
   * Check to see if there is already a value associated with the current
   * thread for the given key.
   *
   * @param key The key against which to check for a given value within the current thread.
   * @return True if there is currently a session bound.
   */
  public static boolean hasBind(@Nonnull Object key) {
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
  public static @Nullable <T> T bind(@Nonnull Object key, @Nonnull T value) {
    return (T) threadMap(true).put(key, value);
  }

  /**
   * Unbinds the session (if one) current associated with the context for the
   * given session.
   *
   * @param key The factory for which to unbind the current session.
   * @param <T> Bind type.
   * @return The bound session if one, else null.
   */
  public static @Nullable <T> T unbind(@Nonnull Object key) {
    final Map<Object, Object> sessionMap = threadMap();
    T existing = null;
    if (sessionMap != null) {
      existing = (T) sessionMap.remove(key);
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
  public static @Nullable <T> T get(@Nonnull Object key) {
    final Map<Object, Object> sessionMap = threadMap();
    if (sessionMap == null) {
      return null;
    } else {
      return (T) sessionMap.get(key);
    }
  }

  private static Map<Object, Object> threadMap() {
    return threadMap(false);
  }

  private static Map<Object, Object> threadMap(boolean createMap) {
    Map<Object, Object> sessionMap = CONTEXT_TL.get();
    if (sessionMap == null && createMap) {
      sessionMap = new HashMap<>();
      CONTEXT_TL.set(sessionMap);
    }
    return sessionMap;
  }

  private static void doCleanup() {
    final Map<Object, Object> ctx = threadMap(false);
    if (ctx != null) {
      if (ctx.isEmpty()) {
        CONTEXT_TL.remove();
      }
    }
  }
}
