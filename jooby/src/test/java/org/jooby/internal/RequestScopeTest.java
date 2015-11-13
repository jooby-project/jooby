package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.internal.CircularDependencyProxy;

public class RequestScopeTest {

  @Test
  public void enter() {
    RequestScope requestScope = new RequestScope();
    requestScope.enter(Collections.emptyMap());
    requestScope.exit();
  }

  @Test(expected = IllegalStateException.class)
  public void enterTwice() {
    RequestScope requestScope = new RequestScope();
    try {
      requestScope.enter(Collections.emptyMap());
      requestScope.enter(Collections.emptyMap());
    } finally {
      requestScope.exit();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void exitTwice() {
    RequestScope requestScope = new RequestScope();
    requestScope.enter(Collections.emptyMap());
    requestScope.exit();
    requestScope.exit();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void scopedValue() throws Exception {
    RequestScope requestScope = new RequestScope();
    Key<Object> key = Key.get(Object.class);
    Object value = new Object();
    try {
      new MockUnit(Provider.class, Map.class)
          .expect(unit -> {
            Map scopedObjects = unit.get(Map.class);
            requestScope.enter(scopedObjects);
            expect(scopedObjects.get(key)).andReturn(null);
            expect(scopedObjects.containsKey(key)).andReturn(false);

            expect(scopedObjects.put(key, value)).andReturn(null);
          })
          .expect(unit -> {
            Provider provider = unit.get(Provider.class);
            expect(provider.get()).andReturn(value);
          })
          .run(unit -> {
            Object result = requestScope.<Object> scope(key, unit.get(Provider.class)).get();
            assertEquals(value, result);
          });
    } finally {
      requestScope.exit();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void scopedNullValue() throws Exception {
    RequestScope requestScope = new RequestScope();
    Key<Object> key = Key.get(Object.class);
    try {
      new MockUnit(Provider.class, Map.class)
          .expect(unit -> {
            Map scopedObjects = unit.get(Map.class);
            requestScope.enter(scopedObjects);
            expect(scopedObjects.get(key)).andReturn(null);
            expect(scopedObjects.containsKey(key)).andReturn(true);
          })
          .run(unit -> {
            Object result = requestScope.<Object> scope(key, unit.get(Provider.class)).get();
            assertEquals(null, result);
          });
    } finally {
      requestScope.exit();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void scopeExistingValue() throws Exception {
    RequestScope requestScope = new RequestScope();
    Key<Object> key = Key.get(Object.class);
    Object value = new Object();
    try {
      new MockUnit(Provider.class, Map.class)
          .expect(unit -> {
            Map scopedObjects = unit.get(Map.class);
            requestScope.enter(scopedObjects);
            expect(scopedObjects.get(key)).andReturn(value);
          })
          .run(unit -> {
            Object result = requestScope.<Object> scope(key, unit.get(Provider.class)).get();
            assertEquals(value, result);
          });
    } finally {
      requestScope.exit();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void circularScopedValue() throws Exception {
    RequestScope requestScope = new RequestScope();
    Key<Object> key = Key.get(Object.class);
    try {
      new MockUnit(Provider.class, Map.class, CircularDependencyProxy.class)
          .expect(unit -> {
            Map scopedObjects = unit.get(Map.class);
            requestScope.enter(scopedObjects);
            expect(scopedObjects.get(key)).andReturn(null);
            expect(scopedObjects.containsKey(key)).andReturn(false);
          })
          .expect(unit -> {
            Provider provider = unit.get(Provider.class);
            expect(provider.get()).andReturn(unit.get(CircularDependencyProxy.class));
          })
          .run(unit -> {
            Object result = requestScope.<Object> scope(key, unit.get(Provider.class)).get();
            assertEquals(unit.get(CircularDependencyProxy.class), result);
          });
    } finally {
      requestScope.exit();
    }
  }

  @SuppressWarnings({"unchecked" })
  @Test(expected = OutOfScopeException.class)
  public void outOfScope() throws Exception {
    RequestScope requestScope = new RequestScope();
    Key<Object> key = Key.get(Object.class);
    Object value = new Object();
    new MockUnit(Provider.class, Map.class)
        .run(unit -> {
          Object result = requestScope.<Object> scope(key, unit.get(Provider.class)).get();
          assertEquals(value, result);
        });
  }
}
