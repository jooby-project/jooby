package org.jooby.internal.mapper;

import static org.easymock.EasyMock.expect;

import java.util.concurrent.Callable;

import org.jooby.Deferred;
import org.jooby.Deferred.Initializer0;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CallableMapper.class, Deferred.class })
public class CallableMapperTest {

  private Block deferred = unit -> {
    Deferred deferred = unit.constructor(Deferred.class)
        .args(Deferred.Initializer0.class)
        .build(unit.capture(Deferred.Initializer0.class));
    unit.registerMock(Deferred.class, deferred);
  };

  private Block init0 = unit -> {
    Initializer0 next = unit.captured(Deferred.Initializer0.class).iterator().next();
    next.run(unit.get(Deferred.class));
  };

  @SuppressWarnings("rawtypes")
  @Test
  public void resolve() throws Exception {
    Object value = new Object();
    new MockUnit(Callable.class)
        .expect(deferred)
        .expect(unit -> {
          Callable callable = unit.get(Callable.class);
          expect(callable.call()).andReturn(value);
        })
        .expect(unit -> {
          Deferred deferred = unit.get(Deferred.class);
          deferred.resolve(value);
        })
        .run(unit -> {
          new CallableMapper()
              .map(unit.get(Callable.class));
        }, init0);
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void reject() throws Exception {
    Exception value = new Exception();
    new MockUnit(Callable.class)
        .expect(deferred)
        .expect(unit -> {
          Callable callable = unit.get(Callable.class);
          expect(callable.call()).andThrow(value);
        })
        .expect(unit -> {
          Deferred deferred = unit.get(Deferred.class);
          deferred.reject(value);
        })
        .run(unit -> {
          new CallableMapper()
              .map(unit.get(Callable.class));
        }, init0);
  }
}
