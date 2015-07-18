package org.jooby.ws;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class OnCheckedErrFeature extends ServerFeature {

  {
    ws("/checked-err", ws -> {

      throw new IllegalStateException("intentionl err");
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
  public void onCheckedExceptionCloseSocket() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    client.prepareGet(ws("checked-err").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketListener() {

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

    latch.await(1L, TimeUnit.SECONDS);
  }

}
