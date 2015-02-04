package org.jooby.internal;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

public class RequestScope implements Scope {

  private final ThreadLocal<Map<Key<?>, Object>> scope = new ThreadLocal<>();

  public void enter() {
    checkState(scope.get() == null, "A scoping block is already in progress");
    scope.set(new HashMap<>());
  }

  public void exit() {
    checkState(scope.get() != null, "No scoping block in progress");
    scope.remove();
  }

  public <T> void seed(final Key<T> key, final T value) {
    Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
    checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
        "already seeded in this scope. Old value: %s New value: %s", key,
        scopedObjects.get(key), value);
    scopedObjects.put(key, value);
  }

  public <T> void seed(final Class<T> clazz, final T value) {
    seed(Key.get(clazz), value);
  }

  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    return () -> {
      Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

      @SuppressWarnings("unchecked")
      T current = (T) scopedObjects.get(key);
      if (current == null && !scopedObjects.containsKey(key)) {
        current = unscoped.get();

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return current;
        }

        scopedObjects.put(key, current);
      }
      return current;
    };
  }

  private <T> Map<Key<?>, Object> getScopedObjectMap(final Key<T> key) {
    Map<Key<?>, Object> scopedObjects = scope.get();
    if (scopedObjects == null) {
      throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block");
    }
    return scopedObjects;
  }

}
