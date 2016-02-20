package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.elasticsearch.common.collect.Sets;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Splitter;

public class RequestLocalsFeature extends ServerFeature {

  {

    get("/locals", (req) -> {
      assertFalse(req.isSet("l1"));
      req.set("l1", "v1");
      assertTrue(req.isSet("l1"));
      return req.ifGet("l1");
    });

    get("/locals/unset", (req) -> {
      req.set("l1", "v1");
      Optional<Object> val = req.unset("l1");
      assertFalse(req.isSet("l1"));
      return val;
    });

    get("/locals/attributes", (req) -> {
      req.set("l1", "v1");
      return req.attributes();
    });

  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/locals")
        .expect("Optional[v1]");
  }

  @Test
  public void unset() throws Exception {
    request()
        .get("/locals/unset")
        .expect("Optional[v1]");
  }

  @Test
  public void attributes() throws Exception {
    request()
        .get("/locals/attributes")
        .expect(c -> {
          String s = c.substring(1, c.length() - 1).trim();
          Set<String> values = Sets.newHashSet(Splitter.on(",").trimResults().split(s));
          assertEquals(Sets.newHashSet("contextPath=", "path=/locals/attributes", "l1=v1"), values);
        });

  }

}
