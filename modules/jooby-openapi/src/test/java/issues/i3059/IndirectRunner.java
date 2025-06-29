/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3059;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import java.util.ArrayList;
import java.util.List;

import io.jooby.Jooby;

public class IndirectRunner {

  private final List<Object> resourcesToBind;

  public static IndirectRunner create() {
    return new IndirectRunner();
  }

  IndirectRunner() {
    this.resourcesToBind = new ArrayList<>();
  }

  public IndirectRunner bindResource(Object resource) {
    resourcesToBind.add(resource);
    return this;
  }

  public void run() {
    Jooby jooby = new Jooby();
    bindResources(jooby);
    Jooby.runApp(new String[] {}, () -> jooby);
  }

  private void bindResources(Jooby jooby) {
    for (Object resource : resourcesToBind) {
      jooby.mvc(toMvcExtension(resource));
    }
  }
}
