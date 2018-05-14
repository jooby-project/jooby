package org.jooby.ws;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import org.jooby.test.ServerFeature;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketTerminateFeature extends ServerFeature {

  private static AtomicInteger state = new AtomicInteger(0);
  {
    ws("/ws", ws -> {

      ws.onClose(status -> {
        state.set(status.code());
      });

      ws.terminate();
    });

  }

  private AsyncHttpClient client;

  @Before
  public void before() {
    state.set(0);
    client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
  }

  @After
  public void after() {
    client.close();
  }

  @Test
  public void terminate() throws Exception {

    assertEquals(0, state.get());

    client.prepareGet(ws("ws").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onMessage(final String message) {
              }

              @Override
              public void onOpen(final WebSocket websocket) {
              }

              @Override
              public void onClose(final WebSocket websocket) {
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build())
        .get();
    while (1006 != state.get()) {
      Thread.sleep(300L);
    }
    assertEquals(1006, state.get());
  }
}
