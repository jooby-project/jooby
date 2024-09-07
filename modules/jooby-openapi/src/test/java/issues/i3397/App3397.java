/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3397;

import io.avaje.inject.BeanScope;
import io.jooby.Jooby;
import io.jooby.OpenAPIModule;

public class App3397 extends Jooby {
  {
    install(new OpenAPIModule());

    BeanScope beanScope = BeanScope.builder().build();

    mvc(beanScope.get(Controller3397.class));
  }
}
