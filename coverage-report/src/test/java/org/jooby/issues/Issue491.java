package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;

public class Issue491 extends ServerFeature {

  @Inject
  @Named("application.port")
  private String applicationPort;

  {

    get("/491", () -> applicationPort);

  }

  @Test
  public void injectableFields() throws Exception {
    request()
        .get("/491")
        .expect(port + "");
  }

}
