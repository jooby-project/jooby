package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.internal.LifecycleProcessor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

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

  @Test
  public void noStopForProto() throws Exception {
    counter = new AtomicInteger(0);

    Injector injector = Guice.createInjector();

    injector.getInstance(ManagedObject.class);
    injector.getInstance(ManagedObject.class);
    injector.getInstance(ManagedObject.class);

    LifecycleProcessor.onPreDestroy(injector, log);

    assertEquals(0, counter.get());
  }

  @Test
  public void stopShouldWorkOnSingletonObjects() throws Exception {
    counter = new AtomicInteger(0);

    Injector injector = Guice.createInjector();

    injector.getInstance(SingletonObject.class);

    LifecycleProcessor.onPreDestroy(injector, log);

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldWorkOnProviderOfSingleton() throws Exception {
    counter = new AtomicInteger(0);

    Injector injector = Guice.createInjector(binder -> {
      binder.bind(Object.class).toProvider(new SingletonProvider<>(new Object())).in(Singleton.class);
    });

    injector.getInstance(Object.class);

    LifecycleProcessor.onPreDestroy(injector, log);

    assertEquals(1, counter.get());
  }

  @Test
  public void stopShouldBeExecutedOnlyOnce() throws Exception {
    counter = new AtomicInteger(0);

    Injector injector = Guice.createInjector();

    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);
    injector.getInstance(SingletonObject.class);

    LifecycleProcessor.onPreDestroy(injector, log);

    assertEquals(1, counter.get());
  }

}
