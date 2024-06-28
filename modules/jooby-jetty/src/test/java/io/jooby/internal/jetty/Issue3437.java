/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.eclipse.jetty.http.HttpHeader.CONTENT_LENGTH;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.channels.FileChannel;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.Invocable;
import org.junit.jupiter.api.Test;

import io.jooby.Router;

public class Issue3437 {

  @Test
  public void shouldNotCloseFileChannel() throws IOException {
    var fileSize = 871L;
    var uri = mock(HttpURI.class);
    var request = mock(Request.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getHttpURI()).thenReturn(uri);

    var headers = mock(HttpFields.Mutable.class);

    var response = mock(Response.class);
    when(response.getHeaders()).thenReturn(headers);
    var callback = mock(Callback.class);
    var router = mock(Router.class);
    var channel = mock(FileChannel.class);
    when(channel.size()).thenReturn(fileSize);

    var context =
        new JettyContext(
            Invocable.InvocationType.BLOCKING, request, response, callback, router, 4000, 4000);

    context.send(channel);
    verify(headers).put(CONTENT_LENGTH, fileSize);
    verify(channel, never()).close();
  }
}
