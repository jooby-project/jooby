package org.jooby.internal.elasticsearch;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CountDownLatch;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.jooby.Response;

public class EmbeddedHttpChannel extends HttpChannel {

  private Response rsp;

  private final CountDownLatch latch = new CountDownLatch(1);

  private Exception failure;

  public EmbeddedHttpChannel(final RestRequest request, final Response rsp,
      final boolean detailedErrorsEnabled) {
    super(request, detailedErrorsEnabled);
    this.rsp = requireNonNull(rsp, "Response is required.");
  }

  @Override
  public void sendResponse(final RestResponse response) {
    try {
      String opaque = request.header("X-Opaque-Id");
      if (opaque != null) {
        rsp.header("X-Opaque-Id", opaque);
      }

      BytesReference content = response.content();
      rsp.status(response.status().getStatus())
          .length(content.length())
          .type(response.contentType())
          .send(content);
    } catch (Exception ex) {
      failure = ex;
    } finally {
      latch.countDown();
    }

  }

  public void done() throws Exception {
    latch.await();

    if (failure != null) {
      throw failure;
    }
  }

}
