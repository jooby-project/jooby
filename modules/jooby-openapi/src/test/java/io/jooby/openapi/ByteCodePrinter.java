package io.jooby.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import examples.RouteIdioms;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.ASMifier;

import java.util.HashMap;
import java.util.Map;

public class ByteCodePrinter {

  @Test
  public void appA() throws Exception {
    ASMifier.main(new String[]{RouteIdioms.class.getName()});
  }

  static class JsonType {

    public JavaType type;
  }

  @Test
  public void encodeJavaType() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();

    JsonType value = new JsonType();
    value.type = mapper.getTypeFactory().constructType(int.class);

    String json = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(value);
    System.out.println(json);
    JsonType readIt = mapper.readValue(json, JsonType.class);
    System.out.println(readIt.type.isPrimitive());
  }
}

