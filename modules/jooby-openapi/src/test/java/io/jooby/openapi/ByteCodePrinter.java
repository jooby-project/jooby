package io.jooby.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteCodePrinter {

//  @Test
//  public void appA() throws Exception {
//    ASMifier.main(new String[]{RouteIdioms.class.getName()});
//  }

  static class JsonType {

    public JavaType type;
  }

  @Test
  public void encodeJavaType() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();

    JsonType value = new JsonType();
    value.type = mapper.getTypeFactory().constructType(String[].class);

    String json = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(value);
    assertEquals("{\n"
        + "  \"type\" : \"[Ljava.lang.String;\"\n"
        + "}", json);
    JsonType readIt = mapper.readValue(json, JsonType.class);
    assertEquals(String[].class, readIt.type.getRawClass());
  }
}

