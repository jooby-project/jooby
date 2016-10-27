package org.jooby.ws;

import static org.junit.Assert.assertArrayEquals;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jooby.MediaType;
import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketByteListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;


public class OnByteArrayMessageFeature extends ServerFeature {

  {
    ws("/onBinaryMessage", (ws) -> {

      ws.onMessage(message -> {
        String bytes = "=" + new String(message.to(byte[].class));
        ws.send(bytes.getBytes(), () -> {
          ws.close();
        });
      });
    }).produces(MediaType.octetstream);

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
  public void sendBinaryMessage() throws Exception {
    LinkedList<Object> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(3);

    client.prepareGet(ws("onBinaryMessage").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketByteListener() {

              @Override
              public void onMessage(final byte[] message) {
                messages.add(message);
                latch.countDown();
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendMessage("hey!".getBytes());
                latch.countDown();
              }

              @Override
              public void onClose(final WebSocket websocket) {
                latch.countDown();
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();
    if (latch.await(1L, TimeUnit.SECONDS)) {
      assertArrayEquals("=hey!".getBytes(), (byte[]) messages.get(0));
    }
  }
}
