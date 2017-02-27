package org.jooby.issues;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jooby.Err;
import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

public class Issue636 extends ServerFeature {

  private static CountDownLatch closeLatch;

  {
    ws("/636", ws -> {

      ws.onClose(status -> {
        try {
          ws.send("636");
        } catch (Err x) {
          closeLatch.countDown();
        }
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

    WebSocket ws = client.prepareGet(ws("636").toString())
        .execute(new WebSocketUpgradeHandler.Builder().build())
        .get();

    ws.sendMessage("foo");
    Thread.sleep(100L);
    ws.close();
    Thread.sleep(100L);
    closeLatch.await(1L, TimeUnit.SECONDS);
  }

}
