package org.jooby.hazelcast;

import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HazelcastFeature extends ServerFeature {

  {

    use(new Hcast());

    get("/:map/:key/", req -> {
      HazelcastInstance hcast = req.require(HazelcastInstance.class);
      IMap<String, Object> map = hcast.getMap(req.param("map").value());
      return Optional.ofNullable(map.get(req.param("key").value()));
    });

    put("/:map/:key/:value", req -> {
      HazelcastInstance hcast = req.require(HazelcastInstance.class);
      IMap<String, Object> map = hcast.getMap(req.param("map").value());
      return Optional.ofNullable(map.put(req.param("key").value(), req.param("value").value()));
    });

  }

  @Test
  public void basic() throws Exception {
    request()
        .get("/basic/foo")
        .expect("Optional.empty");

    request()
        .put("/basic/foo/bar")
        .expect("Optional.empty");

    request()
        .get("/basic/foo")
        .expect("Optional[bar]");
  }
}
