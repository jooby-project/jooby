package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.jooby.internal.LifecycleProcessor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;

public class StopManagedObjectFeature {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(StopManagedObjectFeature.class);

  private static AtomicInteger counter;

  public static class ManagedObject implements Managed {

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

  }

  @Singleton
  public static class SingletonObject implements Managed {

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
      log.info("stopping: {}", getClass().getName());
      counter.incrementAndGet();
    }

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

    assertEquals(counter.get(), 1);
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

    assertEquals(counter.get(), 1);
  }

}
