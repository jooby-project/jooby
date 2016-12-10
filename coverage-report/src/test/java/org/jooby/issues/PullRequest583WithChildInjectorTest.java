package org.jooby.issues;

import java.util.concurrent.atomic.AtomicReference;

import org.jooby.test.ServerFeature;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public class PullRequest583WithChildInjectorTest extends ServerFeature {

  {
    AtomicReference<Injector> ref = new AtomicReference<>();

    Injector parentInjector = Guice.createInjector(
        binder -> binder.bind(MyInjectedClass.class).toInstance(new MyInjectedClass()));

    injector((stage, module) -> {
      Injector injector = parentInjector.createChildInjector(module);
      ref.set(injector);
      return injector;
    });

    get("/583", () -> {
      Injector injector = require(Injector.class);
      Assert.assertSame(injector, ref.get());
      Assert.assertSame(parentInjector.getInstance(MyInjectedClass.class),
          injector.getInstance(MyInjectedClass.class));
      return "OK";
    });
  }

  @Test
  public void childInjector() throws Exception {
    request()
        .get("/583")
        .expect("OK");
  }

  private static class MyInjectedClass {
  }

}
