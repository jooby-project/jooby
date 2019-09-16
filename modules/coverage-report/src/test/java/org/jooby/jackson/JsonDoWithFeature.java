package org.jooby.jackson;

import java.net.URISyntaxException;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class JsonDoWithFeature extends ServerFeature {

  {

    use(new Jackson().doWith(mapper ->
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        ));

    get("/members", req ->
        Lists.newArrayList(ImmutableMap.<String, Object> of("id", 1, "name", "pablo")));

  }

  @Test
  public void get() throws URISyntaxException, Exception {
    request().get("/members").expect(value ->
            assertEquals("[ {\n" +
                    "  \"id\" : 1,\n" +
                    "  \"name\" : \"pablo\"\n" +
                    "} ]", value.replaceAll("\r\n", "\n"))
    );
  }

}
