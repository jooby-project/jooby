package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue572c extends ServerFeature {

  {
    get("/572c", () -> require(Config.class).getString("contextPath"));
  }

  @Test
  public void shouldGetEmptyContextPath() throws Exception {
    request()
        .get("/572c")
        .expect("");
  }

}
