package org.jooby.ws;

import com.google.common.collect.Sets;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketPauseResumeFeature extends ServerFeature {

  static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  {
    ws("/ws", ws -> {
      ws.pause();
      // 2nd ignored
      ws.pause();

      ws.resume();
      // 2nd call ignored
      ws.resume();

      ws.onMessage(message -> {

        ws.send("=" + message.value(), () -> {
          ws.close();
        });

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
  public void pauseAndResume() throws Exception {
    Set<String> messages = new HashSet<>();

    CountDownLatch latch = new CountDownLatch(2);

    client.prepareGet(ws("ws").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onMessage(final String message) {
                messages.add(message);
                latch.countDown();
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendMessage("hey!");
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
      assertEquals(Sets.newHashSet("=hey!"), messages);
    }
  }
}
