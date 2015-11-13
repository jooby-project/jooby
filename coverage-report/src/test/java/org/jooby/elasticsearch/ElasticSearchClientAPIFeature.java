package org.jooby.elasticsearch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ElasticSearchClientAPIFeature extends ServerFeature {

  {

    use(new Jackson());

    use(new ElasticSearch());

    post("/:id", req -> {
      Client client = req.require(Client.class);
      Map<String, Object> json = new HashMap<>();
      json.put("user", "kimchy");
      json.put("message", "trying out Elasticsearch");
      return client.prepareIndex("twitter", "tweet", req.param("id").value())
          .setSource(json)
          .execute()
          .actionGet();
    });

    get("/:id", req -> {
      Client client = req.require(Client.class);
      return client.prepareGet("twitter", "tweet", req.param("id").value())
          .execute()
          .actionGet().getSource();
    });

    delete("/:id", req -> {
      Client client = req.require(Client.class);
      return client.prepareDelete("twitter", "tweet", req.param("id").value())
          .execute()
          .actionGet();
    });
  }

  @Test
  public void es() throws Exception {
    UUID id = UUID.randomUUID();
    request()
        .post("/" + id)
        .expect(
            "{\"context\":{\"empty\":true},\"headers\":[],\"index\":\"twitter\",\"id\":\"" + id
                + "\",\"type\":\"tweet\",\"version\":1,\"created\":true,\"contextEmpty\":true}");

    request()
        .get("/" + id)
        .expect("{\"message\":\"trying out Elasticsearch\",\"user\":\"kimchy\"}");

    request()
        .delete("/" + id)
        .expect(
            "{\"context\":{\"empty\":true},\"headers\":[],\"index\":\"twitter\",\"id\":\"" + id
                + "\",\"type\":\"tweet\",\"version\":2,\"found\":true,\"contextEmpty\":true}");
  }

}
