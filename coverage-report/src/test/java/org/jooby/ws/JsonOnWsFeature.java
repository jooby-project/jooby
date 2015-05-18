package org.jooby.ws;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class JsonOnWsFeature extends ServerFeature {

  {
    use(new Jackson());

    ws("/ws/json", (ws) -> {

      ws.send(ImmutableMap.builder().put("k", "v").build());
    }).produces("json");

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
  public void json() throws Exception {
    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    client.prepareGet(ws("ws", "json").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onFragment(final String fragment, final boolean last) {
              }

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
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();
    latch.await();
    assertEquals("{\"k\":\"v\"}", messages.get(0));
  }

}
