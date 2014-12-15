package org.jooby.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.net.URISyntaxException;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@SuppressWarnings("unchecked")
public class JsonCustomMapperFeature extends ServerFeature {

  {

    ObjectMapper mapper = new ObjectMapper();
    use(new Json(mapper).doWith(m -> {
      assertSame(mapper, m);
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }));

    get("/members", req ->
      Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo"))
    );

  }

  @Test
  public void get() throws URISyntaxException, Exception {
    assertEquals("[ {\n" +
        "  \"id\" : 1,\n" +
        "  \"name\" : \"pablo\"\n" +
        "} ]", Request.Get(uri("members").build()).execute()
        .returnContent().asString());
  }

}
