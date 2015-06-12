/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

public class RequestScope implements Scope {

  private final ThreadLocal<Map<Object, Object>> scope = new ThreadLocal<>();

  public void enter(final Map<Object, Object> locals) {
    checkState(scope.get() == null, "A scoping block is already in progress");
    scope.set(locals);
  }

  public void exit() {
    checkState(scope.get() != null, "No scoping block in progress");
    scope.remove();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
    return () -> {
      Map<Object, Object> scopedObjects = getScopedObjectMap(key);

      T current = (T) scopedObjects.get(key);
      if (current == null && !scopedObjects.containsKey(key)) {
        current = unscoped.get();

        // don't remember proxies; these exist only to serve circular dependencies
        if (Scopes.isCircularProxy(current)) {
          return current;
        }

        scopedObjects.put(key, current);
      }
      if (current instanceof javax.inject.Provider) {
        if (!javax.inject.Provider.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
          return (T) ((javax.inject.Provider) current).get();
        }
      }
      return current;
    };
  }

  private <T> Map<Object, Object> getScopedObjectMap(final Key<T> key) {
    Map<Object, Object> scopedObjects = scope.get();
    if (scopedObjects == null) {
      throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block");
    }
    return scopedObjects;
  }

}
