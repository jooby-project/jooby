/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.MvcFactory;
import io.jooby.Route;
import jakarta.inject.Provider;

public class Expected2629b implements MvcFactory {

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
                C2629b controller = (C2629b) provider.get();
                var s = ctx.query("s").value();
                var i = ctx.query("i").intValue();
                var d = ctx.query("d").doubleValue();
                var j = ctx.query("j").longValue();
                var f = ctx.query("f").floatValue();
                var b = ctx.query("b").booleanValue();
                return controller.mix(s, i, d, ctx, j, f, b);
              });
      route.setMvcMethod(
          C2629b.class.getMethod(
              "mix",
              String.class,
              Integer.class,
              double.class,
              Context.class,
              long.class,
              Float.class,
              boolean.class));
    };
  }
}
