package org.jooby.test;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.jooby.Jooby;
import org.jooby.MediaType;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

/**
 * Internal use only.
 *
 * @author edgar
 */
@RunWith(JoobySuite.class)
public abstract class SseFeature extends Jooby {

  private int port;

  private AsyncHttpClient client;

  @Before
  public void before() {
    client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().build());
  }

  @After
  public void after() {
    client.close();
  }

  public String sse(final String path, final int count) throws Exception {
    CountDownLatch latch = new CountDownLatch(count);
    String result = client.prepareGet("http://localhost:" + port + path)
        .addHeader("Content-Type", MediaType.sse.name())
        .addHeader("last-event-id", count + "")
        .execute(new AsyncHandler<String>() {

          StringBuilder sb = new StringBuilder();

          @Override
          public void onThrowable(final Throwable t) {
            t.printStackTrace();
          }

          @Override
          public AsyncHandler.STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart)
              throws Exception {
            sb.append(new String(bodyPart.getBodyPartBytes(), StandardCharsets.UTF_8));
            latch.countDown();
            return AsyncHandler.STATE.CONTINUE;
          }

          @Override
          public AsyncHandler.STATE onStatusReceived(final HttpResponseStatus responseStatus)
              throws Exception {
            assertEquals(200, responseStatus.getStatusCode());
            return AsyncHandler.STATE.CONTINUE;
          }

          @Override
          public AsyncHandler.STATE onHeadersReceived(final HttpResponseHeaders headers)
              throws Exception {
            FluentCaseInsensitiveStringsMap h = headers.getHeaders();
            assertEquals("close", h.get("Connection").get(0).toLowerCase());
            assertEquals("text/event-stream; charset=utf-8",
                h.get("Content-Type").get(0).toLowerCase());
            return AsyncHandler.STATE.CONTINUE;
          }

          @Override
          public String onCompleted() throws Exception {
            return sb.toString();
          }
        }).get();

    latch.await();
    return result;
  }

}
