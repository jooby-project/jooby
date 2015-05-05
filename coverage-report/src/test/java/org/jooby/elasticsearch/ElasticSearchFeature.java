package org.jooby.elasticsearch;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ElasticSearchFeature extends ServerFeature {

  {

    use(new ElasticSearch("/es"));

  }

  @Test
  public void es() throws Exception {
    // either 200 or 400
    request()
        .delete("/es/customer")
        .expect(rsp -> {
        });

    request()
        .put("/es/customer")
        .expect(200);

    request()
        .get("/es/customer")
        .expect(200);

    request()
        .put("/es/customer/external/1")
        .body("{\"name\": \"John Doe\"}", "application/json")
        .expect(201);

    request()
        .get("/es/customer/external/1")
        .expect(
            "{\"_index\":\"customer\",\"_type\":\"external\",\"_id\":\"1\",\"_version\":1,\"found\":true,\"_source\":{\"name\": \"John Doe\"}}");
  }

}
