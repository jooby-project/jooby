/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2968;

import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.annotation.Generated;

@Generated(C2968.class)
public class C2968_ implements Extension {
  private Function<Context, C2968> provider;

  public C2968_() {
    this.provider = ctx -> new C2968();
  }

  @Override
  public void install(@NonNull Jooby app) throws Exception {
    app.get("/hello", this::hello);
  }

  public String hello(Context ctx) {
    return provider.apply(ctx).hello(ctx.query("name").value());
  }
}
