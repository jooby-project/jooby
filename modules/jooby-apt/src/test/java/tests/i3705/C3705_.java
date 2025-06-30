/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3705;

@io.jooby.annotation.Generated(C3705.class)
public class C3705_ implements io.jooby.MvcExtension {
  protected java.util.function.Function<io.jooby.Context, C3705> factory;

  public C3705_() {
    this(new C3705());
  }

  public C3705_(C3705 instance) {
    setup(ctx -> instance);
  }

  public C3705_(Class<C3705> type) {
    setup(ctx -> ctx.require(type));
  }

  public C3705_(java.util.function.Supplier<C3705> provider) {
    setup(ctx -> provider.get());
  }

  public C3705_(java.util.function.Function<Class<C3705>, C3705> factory) {
    setup(ctx -> factory.apply(C3705.class));
  }

  private void setup(java.util.function.Function<io.jooby.Context, C3705> factory) {
    this.factory = factory;
  }

  public void install(io.jooby.Jooby app) throws Exception {}
}
