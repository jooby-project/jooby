/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3500;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.gson.GsonModule;

public class WidgetService extends Jooby {

  public WidgetService() {
    install(new GsonModule());

    post(
        "/api/widgets1",
        ctx -> {
          Widget widget = ctx.body().to(Widget.class);
          System.out.println("Created " + widget);
          return ctx.send(StatusCode.CREATED);
        });

    mount(new WidgetRouter());
  }
}

class WidgetRouter extends Jooby {

  public WidgetRouter() {

    post(
        "/api/widgets2",
        ctx -> {
          Widget widget = ctx.body().to(Widget.class);
          return ctx.send(StatusCode.CREATED);
        });
  }
}
