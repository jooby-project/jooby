package org.jooby.integration.ws;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketPauseResumeFeature extends ServerFeature {

  static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  {
    ws("/ws", ws -> {
      System.out.println("connet server");
      ws.onMessage(message -> {
        System.out.println("on message");

        ws.send("=" + message.value(), () -> {
          System.out.println("client close");
          ws.close();
        });

        ws.pause();

        executor.schedule(() -> {
          System.out.println("on resume");
          ws.resume();
        }, 1, TimeUnit.SECONDS);
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
              public void onFragment(final String fragment, final boolean last) {
              }

              @Override
              public void onMessage(final String message) {
                System.out.println("on client message");
                messages.add(message);
                latch.countDown();
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                System.out.println("on open");
                websocket.sendTextMessage("hey!");
              }

              @Override
              public void onClose(final WebSocket websocket) {
                System.out.println("on close");
                latch.countDown();
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();
    latch.await();
    assertEquals(Sets.newHashSet("=hey!"), messages);
  }
}
