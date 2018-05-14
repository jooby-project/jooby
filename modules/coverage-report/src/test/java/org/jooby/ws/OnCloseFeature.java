package org.jooby.ws;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;


public class OnCloseFeature extends ServerFeature {

  private static volatile CountDownLatch closeLatch;

  {
    ws("/onClose", ws -> {

      ws.onClose(status -> {
        assertNotNull(status);
        closeLatch.countDown();
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
  public void sendClose() throws Exception {
    closeLatch = new CountDownLatch(1);
    AtomicBoolean closed = new AtomicBoolean();

    client.prepareGet(ws("onClose").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketListener() {

              @Override
              public void onOpen(final WebSocket websocket) {
                if (!closed.getAndSet(true)) {
                  websocket.close();
                }
              }

              @Override
              public void onClose(final WebSocket websocket) {
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();

    closeLatch.await(1L, TimeUnit.SECONDS);
  }

}
