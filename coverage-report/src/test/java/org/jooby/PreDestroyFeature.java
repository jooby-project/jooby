package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.internal.LifecycleProcessor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

public class PreDestroyFeature {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(PreDestroyFeature.class);

  private static AtomicInteger counter;

  public static class ManagedObject {

    @PreDestroy
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

  }

  @Singleton
  public static class SingletonObject {

    @PreDestroy
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

  }

  @Singleton
  public static class FailSilently {

    @PreDestroy
    public void stop() throws Exception {
      counter.incrementAndGet();
      throw new NullPointerException();
    }

  }

  public static class SingletonProvider<T> implements Provider<T>, Managed {

    private T value;

    public SingletonProvider(final T value) {
      this.value = value;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

    @Override
    public T get() {
      return value;
    }

  }

  @Singleton
  public static class SingletonInjectedProvider implements Provider<String>, Managed {

    private String value;

    @Inject
    public SingletonInjectedProvider(@Named("x") final String value) {
      this.value = value;
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

    @Override
    public String get() {
      return value;
    }

  }

  @Test
  public void stopShouldWorkOnInjectedProviderOfSingleton() throws Exception {
    counter = new AtomicInteger(0);

    LifecycleProcessor processor = new LifecycleProcessor();
    Injector injector = Guice.createInjector((Module) binder -> {
      binder.bindListener(Matchers.any(), processor);
      binder.bind(String.class).annotatedWith(Names.named("x")).toInstance("X");
      binder.bind(String.class).toProvider(SingletonInjectedProvider.class).asEagerSingleton();
    });

    injector.getInstance(SingletonInjectedProvider.class);

    processor.destroy();

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldWorkOnSingletonObjects() throws Exception {
    counter = new AtomicInteger(0);

    LifecycleProcessor processor = new LifecycleProcessor();
    Injector injector = Guice.createInjector((Module) binder -> {
      binder.bindListener(Matchers.any(), processor);
    });

    injector.getInstance(SingletonObject.class);

    processor.destroy();

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldWorkOnProviderOfSingleton() throws Exception {
    counter = new AtomicInteger(0);

    LifecycleProcessor processor = new LifecycleProcessor();
    Injector injector = Guice.createInjector((Module) binder -> {
      binder.bindListener(Matchers.any(), processor);
      binder.bind(Object.class).toProvider(new SingletonProvider<>(new Object()))
          .in(Singleton.class);
    });

    injector.getInstance(Object.class);

    processor.destroy();

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldBeExecutedOnlyOnce() throws Exception {
    counter = new AtomicInteger(0);

    LifecycleProcessor processor = new LifecycleProcessor();
    Injector injector = Guice.createInjector((Module) binder -> {
      binder.bindListener(Matchers.any(), processor);
    });

    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);

    processor.destroy();

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldCatchException() throws Exception {
    counter = new AtomicInteger(0);

    LifecycleProcessor processor = new LifecycleProcessor();
    Injector injector = Guice.createInjector((Module) binder -> {
      binder.bindListener(Matchers.any(), processor);
    });

    injector.getInstance(FailSilently.class);

    processor.destroy();

    assertEquals(1, counter.get());
  }

}
