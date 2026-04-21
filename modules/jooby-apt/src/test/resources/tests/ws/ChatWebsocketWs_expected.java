package tests.ws;

@io.jooby.annotation.Generated(ChatWebsocket.class)
public class ChatWebsocketWs_ implements io.jooby.Extension {
    protected java.util.function.Function<io.jooby.Context, ChatWebsocket> factory;

    public ChatWebsocketWs_() {
      this(io.jooby.SneakyThrows.singleton(ChatWebsocket::new));
    }

    public ChatWebsocketWs_(ChatWebsocket instance) {
      setup(ctx -> instance);
    }

    public ChatWebsocketWs_(io.jooby.SneakyThrows.Supplier<ChatWebsocket> provider) {
      setup(ctx -> provider.get());
    }

    public ChatWebsocketWs_(io.jooby.SneakyThrows.Function<Class<ChatWebsocket>, ChatWebsocket> provider) {
      setup(ctx -> provider.apply(ChatWebsocket.class));
    }

    private void setup(java.util.function.Function<io.jooby.Context, ChatWebsocket> factory) {
      this.factory = factory;
    }

    public void install(io.jooby.Jooby app) throws Exception {
      app.ws("/chat/{username}", this::wsInit);
    }

    private void wsInit(io.jooby.Context ctx, io.jooby.WebSocketConfigurer configurer) {
      /** See {@link ChatWebsocket#onConnect(io.jooby.WebSocket, io.jooby.Context)} */
      configurer.onConnect(ws -> {
        var c = this.factory.apply(ctx);
        var __wsReturn = c.onConnect(ws, ctx);
        ws.send(__wsReturn);
      });

      /** See {@link ChatWebsocket#onMessage(io.jooby.WebSocket, io.jooby.Context, io.jooby.WebSocketMessage)} */
      configurer.onMessage((ws, message) -> {
        var c = this.factory.apply(ctx);
        var __wsReturn = c.onMessage(ws, ctx, message);
        ws.render(__wsReturn);
      });

      /** See {@link ChatWebsocket#onClose(io.jooby.WebSocket, io.jooby.Context, io.jooby.WebSocketCloseStatus)} */
      configurer.onClose((ws, status) -> {
        var c = this.factory.apply(ctx);
        c.onClose(ws, ctx, status);
      });

      /** See {@link ChatWebsocket#onError(io.jooby.WebSocket, io.jooby.Context, Throwable)} */
      configurer.onError((ws, cause) -> {
        var c = this.factory.apply(ctx);
        c.onError(ws, ctx, cause);
      });

    }
}
