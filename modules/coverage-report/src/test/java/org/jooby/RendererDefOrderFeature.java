package org.jooby;

import java.util.Set;
import java.util.stream.Collectors;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RendererDefOrderFeature extends ServerFeature {

  Key<Set<Renderer>> KEY = Key.get(new TypeLiteral<Set<Renderer>>() {
  });

  {

    get("/renderer/order", req -> req.require(KEY).stream().map(Renderer::name)
        .collect(Collectors.joining(", ")));

  }

  @Test
  public void order() throws Exception {
    request()
        .get("/renderer/order")
        .expect("asset, bytes, byteBuffer, file, charBuffer, stream, reader, fileChannel, defaultErr, text");
  }
}
