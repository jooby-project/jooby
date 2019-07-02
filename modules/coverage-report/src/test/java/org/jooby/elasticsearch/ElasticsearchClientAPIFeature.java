package org.jooby.elasticsearch;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ElasticsearchClientAPIFeature extends ServerFeature {

  {

    use(new Elasticsearch());

    get("/", req -> req.require(RestClient.class).getClass().getName());
    get("/hlrc", req -> req.require(RestHighLevelClient.class).getClass().getName());

  }

  @Test
  public void boot() throws Exception {
    request()
        .get("/")
        .expect("org.elasticsearch.client.RestClient");
    request()
        .get("/hlrc")
        .expect("org.elasticsearch.client.RestHighLevelClient");
  }

}
