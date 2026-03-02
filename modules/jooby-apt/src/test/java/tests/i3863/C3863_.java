/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

@io.jooby.annotation.Generated(C3863.class)
public class C3863_ implements io.jooby.Extension {
  protected java.util.function.Function<io.jooby.Context, C3863> factory;

  public C3863_() {
    this(io.jooby.SneakyThrows.singleton(C3863::new));
  }

  public C3863_(C3863 instance) {
    setup(ctx -> instance);
  }

  public C3863_(io.jooby.SneakyThrows.Supplier<C3863> provider) {
    setup(ctx -> (C3863) provider.get());
  }

  public C3863_(io.jooby.SneakyThrows.Function<Class<C3863>, C3863> provider) {
    setup(ctx -> provider.apply(C3863.class));
  }

  private void setup(java.util.function.Function<io.jooby.Context, C3863> factory) {
    this.factory = factory;
  }

  public void install(io.jooby.Jooby app) throws Exception {
    /** See {@link C3863#findUser(long) */
    app.get("/trpc/users.findUser", this::trpcFindUser);

    /** See {@link C3863#findUser(long) */
    app.get("/users/{id}", this::findUser);
  }

  public U3863 findUser(io.jooby.Context ctx) {
    var c = this.factory.apply(ctx);
    return c.findUser(ctx.path("id").longValue());
  }

  public io.jooby.trpc.TrpcResponse<U3863> trpcFindUser(io.jooby.Context ctx) {
    var input = ctx.query("input").value();
    var mapper = ctx.require(tools.jackson.databind.ObjectMapper.class);
    var arg0 = mapper.readValue(input, long.class);
    var c = this.factory.apply(ctx);
    var result = c.findUser(arg0);
    return io.jooby.trpc.TrpcResponse.success(result);
  }
}
