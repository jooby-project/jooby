/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.Route;
import jakarta.inject.Provider;

public class Expected2629 implements MvcFactory {

  @Override
  public boolean supports(@NonNull Class type) {
    return false;
  }

  @NonNull @Override
  public Extension create(@NonNull Provider provider) {
    return application -> {
      Route route =
          application.get(
              "/2629",
              ctx -> {
                C2629 controller = (C2629) provider.get();
                String type = ctx.query("type").value();
                var number = ctx.query("number").toList(Integer.class);
                var bool = ctx.query("bool").booleanValue();
                return controller.queryUsers(type, number, bool);
              });
      route.setMvcMethod(
          C2629.class.getMethod("queryUsers", String.class, List.class, boolean.class));
    };
  }
}
