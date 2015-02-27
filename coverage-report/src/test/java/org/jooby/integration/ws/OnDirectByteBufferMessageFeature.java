package org.jooby.integration.ws;

import static org.junit.Assert.assertArrayEquals;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import org.jooby.test.ServerFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class OnDirectByteBufferMessageFeature extends ServerFeature {

  {
    ws("/onBinaryMessage/buffer/direct", (ws) -> {

      ws.onMessage(message -> {
        String bytes = "=" + new String(message.to(byte[].class));
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.getBytes().length);
        buffer.put(bytes.getBytes());
        ws.send(buffer, () -> {
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
  public void sendBinaryMessageBufferDirect() throws Exception {
    LinkedList<Object> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    client.prepareGet(ws("onBinaryMessage", "buffer", "direct").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketByteListener() {

              @Override
              public void onFragment(final byte[] fragment, final boolean last) {
              }

              @Override
              public void onMessage(final byte[] message) {
                messages.add(message);
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendMessage("hey!".getBytes());
              }

              @Override
              public void onClose(final WebSocket websocket) {
                latch.countDown();
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();
    latch.await();
    assertArrayEquals("=hey!".getBytes(), (byte[]) messages.get(0));
  }

}
