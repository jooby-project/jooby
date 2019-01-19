package org.jooby.internal.apitool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.util.Types;
import org.jooby.apitool.ApiToolFeature;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

public class Issue1230 extends ApiToolFeature {

  public static class I1230 {

    private Type type;

    public Type getType() {
      return type;
    }

    public void setType(Type type) {
      this.type = type;
    }
  }

  @Test
  public void shouldDeserializeComplexTypePropertly() throws Exception {
    ObjectMapper mapper = BytecodeRouteParser.mapper;
    I1230 expected = new I1230();
    ParameterizedType type = Types.listOf(Types.listOf(Integer.class));
    expected.setType(type);
    String json = mapper.writeValueAsString(expected);
    I1230 actual = mapper.readValue(json, I1230.class);
    assertEquals(expected.getType(), actual.getType());
  }
}
