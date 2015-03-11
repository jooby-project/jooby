package org.jooby.internal;

import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.jooby.Managed;
import org.jooby.MockUnit;
import org.junit.Test;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;

public class PostConstructTest {

  public static class ValidPostConstruct {

    private int calls;

    @PostConstruct
    public void start() {
      calls += 1;
    }
  }

  public static class ThrowablePostConstruct {

    private Exception ex;

    public ThrowablePostConstruct(final Exception ex) {
      this.ex = ex;
    }

    public ThrowablePostConstruct() {
    }

    @PostConstruct
    public void start() throws Exception {
      throw ex;
    }
  }

  public static class StaticPostConstruct {

    @PostConstruct
    public static void start() {
    }
  }

  public static class PrivatePostConstruct {

    @PostConstruct
    private void start() {
    }
  }

  public static class PostConstructWithArgs {

    @PostConstruct
    public void start(final int something) {
    }
  }

  public static class PostConstructWithReturnType {

    @PostConstruct
    public Object start() {
      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void callStartMethod() throws Exception {
    TypeLiteral<Managed> type = new TypeLiteral<Managed>() {
    };
    new MockUnit(TypeEncounter.class, Managed.class)
        .expect(unit -> {
          Managed managed = unit.get(Managed.class);
          managed.start();
        })
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<Managed> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          listener.afterInjection(unit.get(Managed.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void postConstruct() throws Exception {
    TypeLiteral<ValidPostConstruct> type = new TypeLiteral<ValidPostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<ValidPostConstruct> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          ValidPostConstruct postConstruct = new ValidPostConstruct();
          listener.afterInjection(postConstruct);
          assertEquals(1, postConstruct.calls);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = NullPointerException.class)
  public void postConstructRuntimeException() throws Exception {
    TypeLiteral<ThrowablePostConstruct> type = new TypeLiteral<ThrowablePostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<ThrowablePostConstruct> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          listener.afterInjection(null);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = IllegalStateException.class)
  public void postConstructAnnotationCheckedException() throws Exception {
    TypeLiteral<ThrowablePostConstruct> type = new TypeLiteral<ThrowablePostConstruct>() {
    };
    new MockUnit(TypeEncounter.class, ThrowablePostConstruct.class)
        .expect(unit -> {
          ThrowablePostConstruct postConstruct = unit.get(ThrowablePostConstruct.class);
          postConstruct.start();
          expectLastCall().andThrow(new IOException("Intentional err"));
        })
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<ThrowablePostConstruct> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          listener.afterInjection(unit.get(ThrowablePostConstruct.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = IllegalStateException.class)
  public void postConstructInvocationException() throws Exception {
    TypeLiteral<ThrowablePostConstruct> type = new TypeLiteral<ThrowablePostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<ThrowablePostConstruct> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          listener.afterInjection(new ThrowablePostConstruct(new IOException()));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = IllegalStateException.class)
  public void postConstructInvocationRuntimeException() throws Exception {
    TypeLiteral<ThrowablePostConstruct> type = new TypeLiteral<ThrowablePostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .expect(unit -> {
          TypeEncounter encounter = unit.get(TypeEncounter.class);
          encounter.register(unit.capture(InjectionListener.class));
        })
        .run(unit -> {
          new LifecycleProcessor().<ThrowablePostConstruct> hear(type, unit.get(TypeEncounter.class));
        }, unit -> {
          InjectionListener listener = unit.captured(InjectionListener.class).iterator().next();
          listener.afterInjection(new ThrowablePostConstruct(new IOException()));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void postConstructShouldNOTBeStatic() throws Exception {
    TypeLiteral<StaticPostConstruct> type = new TypeLiteral<StaticPostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .run(unit -> {
          new LifecycleProcessor().<StaticPostConstruct> hear(type, unit.get(TypeEncounter.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void postConstructShouldNOTBePrivate() throws Exception {
    TypeLiteral<PrivatePostConstruct> type = new TypeLiteral<PrivatePostConstruct>() {
    };
    new MockUnit(TypeEncounter.class)
        .run(unit -> {
          new LifecycleProcessor().<PrivatePostConstruct> hear(type, unit.get(TypeEncounter.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void postConstructShouldNOTHaveArgs() throws Exception {
    TypeLiteral<PostConstructWithArgs> type = new TypeLiteral<PostConstructWithArgs>() {
    };
    new MockUnit(TypeEncounter.class)
        .run(unit -> {
          new LifecycleProcessor().<PostConstructWithArgs> hear(type, unit.get(TypeEncounter.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void postConstructShouldNOTReturnType() throws Exception {
    TypeLiteral<PostConstructWithReturnType> type = new TypeLiteral<PostConstructWithReturnType>() {
    };
    new MockUnit(TypeEncounter.class)
        .run(unit -> {
          new LifecycleProcessor().<PostConstructWithReturnType> hear(type, unit.get(TypeEncounter.class));
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void dontCallStartMethod() throws Exception {
    TypeLiteral<Runnable> type = new TypeLiteral<Runnable>() {
    };
    new MockUnit(TypeEncounter.class, Runnable.class)
        .run(unit -> {
          new LifecycleProcessor().<Runnable> hear(type, unit.get(TypeEncounter.class));
        });
  }
}
