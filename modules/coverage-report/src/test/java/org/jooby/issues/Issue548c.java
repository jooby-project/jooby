package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

public class Issue548c extends ServerFeature {

  @Path("/548c/annotated-path")
  public static class MySocket implements org.jooby.WebSocket.OnMessage<String> {

    private org.jooby.WebSocket ws;

    @Inject
    public MySocket(final org.jooby.WebSocket ws) {
      this.ws = ws;
    }

    @Override
    public void onMessage(final String message) throws Exception {
      ws.send("=" + message);
    }

  }

  {
    ws(MySocket.class);
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
  public void sendText() throws Exception {
    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    client.prepareGet(ws("548c/annotated-path").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onMessage(final String message) {
                messages.add(message);
                latch.countDown();
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendMessage("mvc");
              }

              @Override
              public void onClose(final WebSocket websocket) {
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build())
        .get();
    if (latch.await(1L, TimeUnit.SECONDS)) {
      assertEquals(Arrays.asList("=mvc"), messages);
    }
  }

}
