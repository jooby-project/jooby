package org.jooby.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

public class WebSocketFeature extends ServerFeature {

  private static final CountDownLatch closeLatch = new CountDownLatch(1);

  {
    ws("/connect", (ws) -> {
      ws.send("connected!");
      ws.close();
    });

    ws("/onTextMessage", (ws) -> {

      ws.onMessage(message -> {
        ws.send("=" + message.stringValue());
        ws.close();
      });
    });

    ws("/onBinaryMessage", (ws) -> {

      ws.onMessage(message -> {
        String bytes = "=" + new String(message.to(byte[].class));
        ws.send(bytes.getBytes());
        ws.close();
      });
    });

    ws("/onBinaryMessage/buffer", (ws) -> {

      ws.onMessage(message -> {
        String bytes = "=" + new String(message.to(byte[].class));
        ws.send(ByteBuffer.wrap(bytes.getBytes()));
        ws.close();
      });
    });

    ws("/onBinaryMessage/buffer/direct", (ws) -> {

      ws.onMessage(message -> {
        String bytes = "=" + new String(message.to(byte[].class));
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.getBytes().length);
        buffer.put(bytes.getBytes());
        ws.send(buffer);
        ws.close();
      });
    });

    ws("/onClose", ws -> {

      ws.onClose(status -> {
        assertNotNull(status);
        assertEquals(1000, status.code());
        assertNull(status.reason());
        closeLatch.countDown();
      });
    });

    ws("/runtime-err", ws -> {

      ws.getInstance(ScheduledExecutorService.class);
    });

    ws("/checked-err", ws -> {

      throw new IllegalStateException("intentionl err");
    });

    ws("/iaex", ws -> {

      throw new IllegalArgumentException("intentionl err");
    });

    ws("/nseex", ws -> {

      throw new NoSuchElementException("intentionl err");
    });

    ws("/bad-data", ws -> {

      throw new Err(Status.BAD_REQUEST);
    });

    ws("/err", ws -> {

      throw new Err(Status.FORBIDDEN);
    });
  }

  @Test
  public void connect() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("connect").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onFragment(final String fragment, final boolean last) {
              }

              @Override
              public void onMessage(final String message) {
                messages.add(message);
              }

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
    latch.await();
    assertEquals(Arrays.asList("connected!"), messages);
    c.close();
  }

  @Test
  public void sendText() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<String> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("onTextMessage").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketTextListener() {

              @Override
              public void onFragment(final String fragment, final boolean last) {
              }

              @Override
              public void onMessage(final String message) {
                messages.add(message);
              }

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.sendTextMessage("hey!");
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
    assertEquals(Arrays.asList("=hey!"), messages);
    c.close();
  }

  @Test
  public void sendBinaryMessage() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<Object> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("onBinaryMessage").toString())
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
    c.close();
  }

  @Test
  public void sendBinaryMessageBuffer() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<Object> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("onBinaryMessage", "buffer").toString())
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
    c.close();
  }

  @Test
  public void sendBinaryMessageBufferDirect() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    LinkedList<Object> messages = new LinkedList<>();

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("onBinaryMessage", "buffer", "direct").toString())
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
    c.close();
  }

  @Test
  public void sendClose() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    c.prepareGet(ws("onClose").toString())
        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
            new WebSocketListener() {

              @Override
              public void onOpen(final WebSocket websocket) {
                websocket.close();
              }

              @Override
              public void onClose(final WebSocket websocket) {
              }

              @Override
              public void onError(final Throwable t) {
              }
            }).build()).get();

    closeLatch.await();

    c.close();
  }

  @Test
  public void onRuntimeErrCloseSocket() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("runtime-err").toString())
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
                System.out.println(t);
              }
            }).build()).get();

    latch.await();

    c.close();

  }

  @Test
  public void onCheckedExceptionCloseSocket() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("checked-err").toString())
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
                System.out.println(t);
              }
            }).build()).get();

    latch.await();

    c.close();

  }

  @Test
  public void onIllegalArgumentException() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("iaex").toString())
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
                System.out.println(t);
              }
            }).build()).get();

    latch.await();

    c.close();

  }

  @Test
  public void onNoSuchElementException() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("nseex").toString())
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

    latch.await();

    c.close();

  }

  @Test
  public void onBadRequestErr() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("bad-data").toString())
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

    latch.await();

    c.close();

  }

  @Test
  public void onAnyErr() throws Exception {
    AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().build();
    AsyncHttpClient c = new AsyncHttpClient(cf);

    CountDownLatch latch = new CountDownLatch(1);

    c.prepareGet(ws("err").toString())
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

    latch.await();

    c.close();

  }

}
