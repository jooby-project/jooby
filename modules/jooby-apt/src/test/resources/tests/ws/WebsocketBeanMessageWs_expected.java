package tests.ws;

@io.jooby.annotation.Generated(WebsocketBeanMessage.class)
public class WebsocketBeanMessageWs_ implements io.jooby.Extension {
    protected java.util.function.Function<io.jooby.Context, WebsocketBeanMessage> factory;

    public WebsocketBeanMessageWs_() {
      this(io.jooby.SneakyThrows.singleton(WebsocketBeanMessage::new));
    }

    public WebsocketBeanMessageWs_(WebsocketBeanMessage instance) {
      setup(ctx -> instance);
    }

    public WebsocketBeanMessageWs_(io.jooby.SneakyThrows.Supplier<WebsocketBeanMessage> provider) {
      setup(ctx -> provider.get());
    }

    public WebsocketBeanMessageWs_(io.jooby.SneakyThrows.Function<Class<WebsocketBeanMessage>, WebsocketBeanMessage> provider) {
      setup(ctx -> provider.apply(WebsocketBeanMessage.class));
    }

    private void setup(java.util.function.Function<io.jooby.Context, WebsocketBeanMessage> factory) {
      this.factory = factory;
    }

    public void install(io.jooby.Jooby app) throws Exception {
      app.ws("/", this::wsInit);
    }

    private void wsInit(io.jooby.Context ctx, io.jooby.WebSocketConfigurer configurer) {
      /** See {@link WebsocketBeanMessage#onMessage(io.jooby.WebSocket, io.jooby.Context, tests.ws.WebsocketBeanMessage.Incoming)} */
      configurer.onMessage((ws, message) -> {
        var c = this.factory.apply(ctx);
        var __wsReturn = c.onMessage(ws, ctx, message.to(tests.ws.WebsocketBeanMessage.Incoming.class));
        ws.render(__wsReturn);
      });

    }
}
