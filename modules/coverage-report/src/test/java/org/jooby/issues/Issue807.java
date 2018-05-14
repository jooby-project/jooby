package org.jooby.issues;

import org.jooby.Err;
import org.jooby.Upload;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.Optional;

public class Issue807 extends ServerFeature {

  {
    get("/807", () -> {
      throw new Err(444);
    });
  }

  @Test
  public void shouldThrowCustomStatusCode() throws Exception {

    request()
        .get("/807")
        .expect(444);
  }

}
