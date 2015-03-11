package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.jooby.Managed;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LifecycleProcessor.class, Scopes.class })
public class PreDestroyTest {

  public static class PreDestroyObject {

    private int calls;

    @PreDestroy
    public void destroy() {
      calls += 1;
    }
  }

  public static class ThrowablePreDestroy {

    @PreDestroy
    public void destroy() throws IOException {
      throw new IOException("intentional err");
    }
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void callStopMethod() throws Exception {
    new MockUnit(Injector.class, Managed.class)
        .expect(unit -> {
          Managed managed = unit.get(Managed.class);
          managed.stop();
        })
        .expect(unit -> {
          unit.mockStatic(Scopes.class);

          Provider provider = unit.mock(Provider.class);
          expect(provider.get()).andReturn(unit.get(Managed.class));

          Key key = Key.get(Managed.class);

          Binding binding = unit.mock(Binding.class);
          expect(Scopes.isSingleton(binding)).andReturn(true);
          expect(binding.getProvider()).andReturn(provider);
          expect(binding.getKey()).andReturn(key);

          Collection<Binding<?>> values = Arrays.asList(binding);

          Map<Key<?>, Binding<?>> bindings = unit.mock(Map.class);
          expect(bindings.values()).andReturn(values);

          Injector injector = unit.get(Injector.class);
          expect(injector.getAllBindings()).andReturn(bindings);
        })
        .run(unit -> {
          LifecycleProcessor.onPreDestroy(unit.get(Injector.class), log);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void callPreDestroy() throws Exception {
    PreDestroyObject managed = new PreDestroyObject();
    new MockUnit(Injector.class)
        .expect(unit -> {
          unit.mockStatic(Scopes.class);

          Provider provider = unit.mock(Provider.class);
          expect(provider.get()).andReturn(managed);

          Key key = Key.get(PreDestroyObject.class);

          Binding binding = unit.mock(Binding.class);
          expect(Scopes.isSingleton(binding)).andReturn(true);
          expect(binding.getProvider()).andReturn(provider);
          expect(binding.getKey()).andReturn(key);

          Collection<Binding<?>> values = Arrays.asList(binding);

          Map<Key<?>, Binding<?>> bindings = unit.mock(Map.class);
          expect(bindings.values()).andReturn(values);

          Injector injector = unit.get(Injector.class);
          expect(injector.getAllBindings()).andReturn(bindings);
        })
        .run(unit -> {
          LifecycleProcessor.onPreDestroy(unit.get(Injector.class), log);
          assertEquals(1, managed.calls);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void ignoreNoSingletonObject() throws Exception {
    new MockUnit(Injector.class)
        .expect(unit -> {
          unit.mockStatic(Scopes.class);

          Binding binding = unit.mock(Binding.class);
          expect(Scopes.isSingleton(binding)).andReturn(false);

          Collection<Binding<?>> values = Arrays.asList(binding);

          Map<Key<?>, Binding<?>> bindings = unit.mock(Map.class);
          expect(bindings.values()).andReturn(values);

          Injector injector = unit.get(Injector.class);
          expect(injector.getAllBindings()).andReturn(bindings);
        })
        .run(unit -> {
          LifecycleProcessor.onPreDestroy(unit.get(Injector.class), log);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void shouldNOTFailOnException() throws Exception {
    new MockUnit(Injector.class, Managed.class)
        .expect(unit -> {
          Managed managed = unit.get(Managed.class);
          managed.stop();
          expectLastCall().andThrow(new IOException());
        })
        .expect(unit -> {
          unit.mockStatic(Scopes.class);

          Provider provider = unit.mock(Provider.class);
          expect(provider.get()).andReturn(unit.get(Managed.class));

          Key key = Key.get(Managed.class);

          Binding binding = unit.mock(Binding.class);
          expect(Scopes.isSingleton(binding)).andReturn(true);
          expect(binding.getProvider()).andReturn(provider);
          expect(binding.getKey()).andReturn(key);

          Collection<Binding<?>> values = Arrays.asList(binding);

          Map<Key<?>, Binding<?>> bindings = unit.mock(Map.class);
          expect(bindings.values()).andReturn(values);

          Injector injector = unit.get(Injector.class);
          expect(injector.getAllBindings()).andReturn(bindings);
        })
        .run(unit -> {
          LifecycleProcessor.onPreDestroy(unit.get(Injector.class), log);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void callPreDestroyShouldNOTFailOnException() throws Exception {
    ThrowablePreDestroy managed = new ThrowablePreDestroy();
    new MockUnit(Injector.class)
        .expect(unit -> {
          unit.mockStatic(Scopes.class);

          Provider provider = unit.mock(Provider.class);
          expect(provider.get()).andReturn(managed);

          Key key = Key.get(ThrowablePreDestroy.class);

          Binding binding = unit.mock(Binding.class);
          expect(Scopes.isSingleton(binding)).andReturn(true);
          expect(binding.getProvider()).andReturn(provider);
          expect(binding.getKey()).andReturn(key);

          Collection<Binding<?>> values = Arrays.asList(binding);

          Map<Key<?>, Binding<?>> bindings = unit.mock(Map.class);
          expect(bindings.values()).andReturn(values);

          Injector injector = unit.get(Injector.class);
          expect(injector.getAllBindings()).andReturn(bindings);
        })
        .run(unit -> {
          LifecycleProcessor.onPreDestroy(unit.get(Injector.class), log);
        });
  }
}
