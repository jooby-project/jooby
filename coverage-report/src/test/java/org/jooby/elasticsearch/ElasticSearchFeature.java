package org.jooby.elasticsearch;

import java.util.UUID;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ElasticSearchFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("elasticsearch.path.data",
            ConfigValueFactory.fromAnyRef("${java.io.tmpdir}/es/"
                + UUID.randomUUID().toString())));

    use(new ElasticSearch("/es"));

  }

  @Test
  public void es() throws Exception {

    request()
        .get("/es")
        .expect(200);

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
