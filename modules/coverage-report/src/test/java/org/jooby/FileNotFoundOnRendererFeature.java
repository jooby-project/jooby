package org.jooby;

import java.io.FileNotFoundException;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class FileNotFoundOnRendererFeature extends ServerFeature {

  {

    renderer((value, ctx) -> {
      if (value.equals("noengine")) {
        throw new FileNotFoundException("f");
      }
    });

    get("/fnf", () -> "noengine");
  }

  @Test
  public void shouldThrowFNFOnNormalRenderer() throws Exception {
    request().get("/fnf").expect(404);
  }
}
