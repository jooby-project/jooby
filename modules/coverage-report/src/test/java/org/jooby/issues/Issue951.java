package org.jooby.issues;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import org.jooby.Request;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Issue951 extends ServerFeature {

  public static class User {
    public String name;

    @Override
    public String toString() {
      return name;
    }
  }

  public static class MySocket implements org.jooby.WebSocket.Handler<User> {

    private org.jooby.WebSocket ws;

    @Inject
    public MySocket(final org.jooby.WebSocket ws) {
      this.ws = ws;
    }

    @Override
    public void onMessage(final User message) throws Exception {
      ws.send("=" + message);
    }

    @Override public void onOpen(Request req, org.jooby.WebSocket ws) throws Exception {

    }

    @Override public void onClose(org.jooby.WebSocket.CloseStatus status) throws Exception {

    }

    @Override public void onError(Throwable err) {

    }
  }

  {
    use(new Jackson());

    ws("/951", MySocket.class)
        .consumes("json");
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
  public void genericTypeOnHandlerObject() throws Exception {
    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    client.prepareGet(ws("951").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onMessage(final String message) {
                messages.add(message);
                latch.countDown();
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendMessage("{\"name\":\"pablo marmol\"}");
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
      assertEquals(Arrays.asList("=pablo marmol"), messages);
    }
  }

}
