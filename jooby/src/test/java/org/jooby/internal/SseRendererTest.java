package org.jooby.internal;

import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import org.jooby.MediaType;
import org.junit.Test;

public class SseRendererTest {

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedSendFile() throws Exception {
    FileChannel filechannel = null;
    new SseRenderer(Collections.emptyList(), MediaType.ALL, StandardCharsets.UTF_8, Locale.US,
        Collections.emptyMap())
            ._send(filechannel);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedStream() throws Exception {
    InputStream stream = null;
    new SseRenderer(Collections.emptyList(), MediaType.ALL, StandardCharsets.UTF_8, Locale.US,
        Collections.emptyMap())
            ._send(stream);
  }
}
