package io.jooby;

import java.util.HashMap;
import java.util.Map;

public class ThreadScope {

  private static final ThreadLocal<Map<Object, Object>> CONTEXT_TL = new ThreadLocal<>();

  /**
   * Check to see if there is already a value associated with the current
   * thread for the given key.
   *
   * @param key The key against which to check for a given value within the current thread.
   * @return True if there is currently a session bound.
   */
  public static boolean hasBind(Object key) {
    return get(key) != null;
  }

  /**
   * Binds the given value to the current context for its key.
   *
   * @param key The key to be bound.
   * @param value The value to be bound.
   * @return Any previously bound session (should be null in most cases).
   */
  public static <T> T bind(Object key, T value) {
    return (T) sessionMap(true).put(key, value);
  }

  /**
   * Unbinds the session (if one) current associated with the context for the
   * given session.
   *
   * @param key The factory for which to unbind the current session.
   * @return The bound session if one, else null.
   */
  public static <T> T unbind(Object key) {
    final Map<Object, Object> sessionMap = sessionMap();
    T existing = null;
    if (sessionMap != null) {
      existing = (T) sessionMap.remove(key);
      doCleanup();
    }
    return existing;
  }

  public static <T> T get(Object key) {
    final Map<Object, Object> sessionMap = sessionMap();
    if (sessionMap == null) {
      return null;
    } else {
      return (T) sessionMap.get(key);
    }
  }

  private static Map<Object, Object> sessionMap() {
    return sessionMap(false);
  }

  private static Map<Object, Object> sessionMap(boolean createMap) {
    Map<Object, Object> sessionMap = CONTEXT_TL.get();
    if (sessionMap == null && createMap) {
      sessionMap = new HashMap<>();
      CONTEXT_TL.set(sessionMap);
    }
    return sessionMap;
  }

  private static void doCleanup() {
    final Map<Object, Object> ctx = sessionMap(false);
    if (ctx != null) {
      if (ctx.isEmpty()) {
        CONTEXT_TL.remove();
      }
    }
  }
}
