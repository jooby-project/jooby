package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.concurrent.CountDownLatch;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EmbeddedHttpChannel.class, CountDownLatch.class })
public class EmbeddedHttpChannelTest {

  @Test
  public void sendResponse() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          rsp.send(content);

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn(null);
        })
        .run(unit -> {
          new EmbeddedHttpChannel(unit.get(RestRequest.class), unit.get(Response.class), true)
              .sendResponse(unit.get(RestResponse.class));
        });
  }

  @Test
  public void done() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();
          latch.await();

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          rsp.send(content);

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn(null);
        })
        .run(unit -> {
          EmbeddedHttpChannel channel = new EmbeddedHttpChannel(unit.get(RestRequest.class),
              unit.get(Response.class), true);
          channel.sendResponse(unit.get(RestResponse.class));
          channel.done();
        });
  }

  @Test
  public void sendResponseWithOpaqueId() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          expect(rsp.header("X-Opaque-Id", "Opaque-Id")).andReturn(rsp);
          rsp.send(content);

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn("Opaque-Id");
        })
        .run(unit -> {
          new EmbeddedHttpChannel(unit.get(RestRequest.class), unit.get(Response.class), true)
              .sendResponse(unit.get(RestResponse.class));
        });
  }

  @Test
  public void sendResponseFailure() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          rsp.send(content);
          expectLastCall().andThrow(new IllegalStateException("Intentional err"));

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn(null);
        })
        .run(unit -> {
          new EmbeddedHttpChannel(unit.get(RestRequest.class), unit.get(Response.class), true)
              .sendResponse(unit.get(RestResponse.class));
        });
  }

  @Test(expected = IllegalStateException.class)
  public void sendResponseFailureOnCountDown() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();
          expectLastCall().andThrow(new IllegalStateException("Intentional err"));

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          rsp.send(content);

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn(null);
        })
        .run(unit -> {
          new EmbeddedHttpChannel(unit.get(RestRequest.class), unit.get(Response.class), true)
              .sendResponse(unit.get(RestResponse.class));
        });
  }

  @Test(expected = IllegalStateException.class)
  public void doneWithFailure() throws Exception {
    new MockUnit(RestRequest.class, RestResponse.class, Response.class)
        .expect(unit -> {

          CountDownLatch latch = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          latch.countDown();
          latch.await();

          RestStatus status = RestStatus.OK;

          BytesReference content = unit.mock(BytesReference.class);
          expect(content.length()).andReturn(10);

          RestResponse restResponse = unit.get(RestResponse.class);
          expect(restResponse.status()).andReturn(status);
          expect(restResponse.contentType()).andReturn("application/json");
          expect(restResponse.content()).andReturn(content);

          Response rsp = unit.get(Response.class);
          expect(rsp.status(status.getStatus())).andReturn(rsp);

          expect(rsp.type("application/json")).andReturn(rsp);
          expect(rsp.length(10L)).andReturn(rsp);
          rsp.send(content);
          expectLastCall().andThrow(new IllegalStateException("intentional err"));

          RestRequest restRequest = unit.get(RestRequest.class);
          expect(restRequest.header("X-Opaque-Id")).andReturn(null);
        })
        .run(unit -> {
          EmbeddedHttpChannel channel = new EmbeddedHttpChannel(unit.get(RestRequest.class),
              unit.get(Response.class), true);
          channel.sendResponse(unit.get(RestResponse.class));
          channel.done();
        });
  }

}
