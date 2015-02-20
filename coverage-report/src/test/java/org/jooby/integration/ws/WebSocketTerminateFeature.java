package org.jooby.integration.ws;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketTerminateFeature extends ServerFeature {

  private static final List<Integer> statusList = new ArrayList<>();
  {
    ws("/ws", ws -> {

      ws.onClose(status -> {
        statusList.add(status.code());
      });

      ws.terminate();
    });

  }

  @Test
  public void terminate() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("ws").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onFragment(final String fragment, final boolean last) {
              }

              @Override
              public void onMessage(final String message) {
              }

              @Override
              public void onOpen(final WebSocket websocket) {
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

    c.close();

    assertEquals(new Integer(1006), statusList.get(0));
    statusList.clear();
  }
}
