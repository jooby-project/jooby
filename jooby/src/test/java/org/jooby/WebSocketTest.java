package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Map;

import org.jooby.WebSocket.CloseStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WebSocket.class, LoggerFactory.class })
public class WebSocketTest {

  static class WebSocketMock implements WebSocket {

    @Override
    public void close(final CloseStatus status) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void resume() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void pause() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void terminate() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(final Object data, final SuccessCallback success, final ErrCallback err)
        throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onMessage(final Callback<Mutant> callback) throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public String path() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String pattern() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> vars() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MediaType consumes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MediaType produces() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T require(final Key<T> key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onError(final ErrCallback callback) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(final Callback<CloseStatus> callback) throws Exception {
      throw new UnsupportedOperationException();
    }

  }

  @Test
  public void noopSuccess() throws Exception {
    WebSocket.SUCCESS.invoke();
  }

  @Test
  public void err() throws Exception {
    Exception ex = new Exception();
    new MockUnit(Logger.class)
        .expect(unit -> {
          Logger log = unit.get(Logger.class);
          log.error("error while sending data", ex);

          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(WebSocket.class)).andReturn(log);
        })
        .run(unit -> {
          WebSocket.ERR.invoke(ex);
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void tooLowCode() throws Exception {
    CloseStatus.of(200);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tooHighCode() throws Exception {
    CloseStatus.of(5001);
  }

  @Test
  public void closeStatus() throws Exception {
    assertEquals(1000, WebSocket.NORMAL.code());
    assertEquals("Normal", WebSocket.NORMAL.reason());
    assertEquals("1000 (Normal)", WebSocket.NORMAL.toString());
    assertEquals("1000", WebSocket.CloseStatus.of(1000).toString());

    assertEquals(1001, WebSocket.GOING_AWAY.code());
    assertEquals("Going away", WebSocket.GOING_AWAY.reason());

    assertEquals(1002, WebSocket.PROTOCOL_ERROR.code());
    assertEquals("Protocol error", WebSocket.PROTOCOL_ERROR.reason());

    assertEquals(1003, WebSocket.NOT_ACCEPTABLE.code());
    assertEquals("Not acceptable", WebSocket.NOT_ACCEPTABLE.reason());

    assertEquals(1007, WebSocket.BAD_DATA.code());
    assertEquals("Bad data", WebSocket.BAD_DATA.reason());

    assertEquals(1008, WebSocket.POLICY_VIOLATION.code());
    assertEquals("Policy violation", WebSocket.POLICY_VIOLATION.reason());

    assertEquals(1009, WebSocket.TOO_BIG_TO_PROCESS.code());
    assertEquals("Too big to process", WebSocket.TOO_BIG_TO_PROCESS.reason());

    assertEquals(1010, WebSocket.REQUIRED_EXTENSION.code());
    assertEquals("Required extension", WebSocket.REQUIRED_EXTENSION.reason());

    assertEquals(1011, WebSocket.SERVER_ERROR.code());
    assertEquals("Server error", WebSocket.SERVER_ERROR.reason());

    assertEquals(1012, WebSocket.SERVICE_RESTARTED.code());
    assertEquals("Service restarted", WebSocket.SERVICE_RESTARTED.reason());

    assertEquals(1013, WebSocket.SERVICE_OVERLOAD.code());
    assertEquals("Service overload", WebSocket.SERVICE_OVERLOAD.reason());
  }

  @Test
  public void closeCodeAndReason() throws Exception {
    LinkedList<WebSocket.CloseStatus> statusList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void close(final CloseStatus status) {
        assertEquals(1004, status.code());
        assertEquals("My reason", status.reason());
        statusList.add(status);
      }
    };
    ws.close(1004, "My reason");
    assertTrue(statusList.size() > 0);
  }

  @Test
  public void closeStatusCode() throws Exception {
    LinkedList<WebSocket.CloseStatus> statusList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void close(final CloseStatus status) {
        assertEquals(1007, status.code());
        assertEquals(null, status.reason());
        statusList.add(status);
      }
    };
    ws.close(1007);
    assertTrue(statusList.size() > 0);
  }

  @Test
  public void close() throws Exception {

    LinkedList<WebSocket.CloseStatus> statusList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void close(final CloseStatus status) {
        assertEquals(1000, status.code());
        assertEquals("Normal", status.reason());
        statusList.add(status);
      }
    };
    ws.close(WebSocket.NORMAL);
    assertTrue(statusList.size() > 0);
  }

  @Test
  public void closeDefault() throws Exception {

    LinkedList<WebSocket.CloseStatus> statusList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void close(final CloseStatus status) {
        assertEquals(1000, status.code());
        assertEquals("Normal", status.reason());
        statusList.add(status);
      }
    };
    ws.close();
    assertTrue(statusList.size() > 0);
  }

  @SuppressWarnings("resource")
  @Test
  public void send() throws Exception {
    Object data = new Object();
    WebSocket.SuccessCallback SUCCESS_ = WebSocket.SUCCESS;
    WebSocket.ErrCallback ERR_ = WebSocket.ERR;
    LinkedList<Object> dataList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void send(final Object data, final SuccessCallback success, final ErrCallback err) throws Exception {
        dataList.add(data);
        assertEquals(SUCCESS_, success);
        assertEquals(ERR_, err);
      }
    };
    ws.send(data);
    assertTrue(dataList.size() > 0);
    assertEquals(data, dataList.getFirst());
  }

  @SuppressWarnings("resource")
  @Test
  public void sendCustomSuccess() throws Exception {
    Object data = new Object();
    WebSocket.SuccessCallback SUCCESS_ = () -> {};
    WebSocket.ErrCallback ERR_ = WebSocket.ERR;
    LinkedList<Object> dataList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void send(final Object data, final SuccessCallback success, final ErrCallback err) throws Exception {
        dataList.add(data);
        assertEquals(SUCCESS_, success);
        assertEquals(ERR_, err);
      }
    };
    ws.send(data, SUCCESS_);
    assertTrue(dataList.size() > 0);
    assertEquals(data, dataList.getFirst());
  }

  @SuppressWarnings("resource")
  @Test
  public void sendCustomErr() throws Exception {
    Object data = new Object();
    WebSocket.SuccessCallback SUCCESS_ = WebSocket.SUCCESS;
    WebSocket.ErrCallback ERR_ = (ex) -> {};
    LinkedList<Object> dataList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void send(final Object data, final SuccessCallback success, final ErrCallback err) throws Exception {
        dataList.add(data);
        assertEquals(SUCCESS_, success);
        assertEquals(ERR_, err);
      }
    };
    ws.send(data, ERR_);
    assertTrue(dataList.size() > 0);
    assertEquals(data, dataList.getFirst());
  }

  @SuppressWarnings("resource")
  @Test
  public void sendCustomSuccessAndErr() throws Exception {
    Object data = new Object();
    WebSocket.SuccessCallback SUCCESS_ = () -> {};
    WebSocket.ErrCallback ERR_ = (ex) -> {};
    LinkedList<Object> dataList = new LinkedList<>();
    WebSocket ws = new WebSocketMock() {
      @Override
      public void send(final Object data, final SuccessCallback success, final ErrCallback err) throws Exception {
        dataList.add(data);
        assertEquals(SUCCESS_, success);
        assertEquals(ERR_, err);
      }
    };
    ws.send(data, SUCCESS_, ERR_);
    assertTrue(dataList.size() > 0);
    assertEquals(data, dataList.getFirst());
  }

  @SuppressWarnings("resource")
  @Test
  public void getInstance() throws Exception {
    Object instance = new Object();
    WebSocket ws = new WebSocketMock() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> T require(final Key<T> key) {
        return (T) instance;
      }
    };
    assertEquals(instance, ws.require(WebSocket.class));
    assertEquals(instance, ws.require(TypeLiteral.get(String.class)));
  }

}
