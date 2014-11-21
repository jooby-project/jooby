package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketBootstrapFeature extends ServerFeature {

  {
    ws("/socket", (ws) -> {

      ws.onMessage(message -> {
        ws.send("=" + message.stringValue());
        ws.close();
      });

      ws.send("connected!");
    });
  }

  @Test
  public void connect() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("socket").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onFragment(final String fragment, final boolean last) {
              }

              @Override
              public void onMessage(final String message) {
                messages.add(message);
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendTextMessage("hey!");
              }

              @Override
              public void onClose(final WebSocket websocket) {
                latch.countDown();
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();
    latch.await();
    assertEquals(Arrays.asList("connected!", "=hey!"), messages);
    c.close();
  }
}
