package org.jooby.gson;

import java.net.URISyntaxException;

import org.jooby.json.Gzon;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@SuppressWarnings("unchecked")
public class GsonDoWithFeature extends ServerFeature {

  {

    use(new Gzon().doWith(builder -> {
      builder.setPrettyPrinting();
    }));

    get("/members", req ->
        Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo")));

  }

  @Test
  public void get() throws URISyntaxException, Exception {
    request()
        .get("/members")
        .expect("[\n" +
            "  {\n" +
            "    \"id\": 1,\n" +
            "    \"name\": \"pablo\"\n" +
            "  }\n" +
            "]");
  }

}
