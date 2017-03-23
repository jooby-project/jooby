package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue128 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("server.http.Method", ConfigValueFactory.fromAnyRef("_method")));
    put("/fake/put", req -> req.method());
  }

  @Test
  public void fakePutViaParamUrl() throws Exception {
    request().post("/fake/put?_method=PUT")
        .expect("PUT");
  }

  @Test
  public void fakePutViaFormParam() throws Exception {
    request().post("/fake/put")
        .form().add("_method", "PUT")
        .expect("PUT");
  }

  @Test
  public void fakePostViaHeader() throws Exception {
    request().post("/fake/put")
        .header("_method", "PUT")
        .expect("PUT");
  }

}
