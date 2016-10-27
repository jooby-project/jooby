package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

public class Issue6 extends ServerFeature {

  {
    ws("/connect", (req, ws) -> {
      ws.send(req.param("ws").value(), () -> {
        Thread.sleep(300L);
        ws.close();
      });
    });

  }

  private AsyncHttpClient client;

  @Before
  public void before() {
    client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
  }

  @After
  public void after() {
    client.close();
  }

  @Test
  public void connectWithRequest() throws Exception {

    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(2);

    client.prepareGet(ws("connect?ws=006").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onMessage(final String message) {
                messages.add(message);
                latch.countDown();
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
            }).build())
        .get();

    if (latch.await(1L, TimeUnit.SECONDS)) {
      assertEquals(Arrays.asList("006"), messages);
    }
  }

}
