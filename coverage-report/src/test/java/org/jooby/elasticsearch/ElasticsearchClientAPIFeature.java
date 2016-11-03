package org.jooby.elasticsearch;

import org.elasticsearch.client.RestClient;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ElasticsearchClientAPIFeature extends ServerFeature {

  {

    use(new Elasticsearch());

    get("/", req -> req.require(RestClient.class).getClass().getName());

  }

  @Test
  public void boot() throws Exception {
    request()
        .get("/")
        .expect("org.elasticsearch.client.RestClient");
  }

}
