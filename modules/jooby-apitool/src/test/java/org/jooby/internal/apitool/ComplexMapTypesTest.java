package org.jooby.internal.apitool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.util.Types;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ComplexMapTypesTest {

    public static class TypeWrapper {
        private Type type;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }
    }

    @Test
    public void shouldDeserializeMapWithListProperly() throws Exception {
        ObjectMapper mapper = BytecodeRouteParser.mapper;
        TypeWrapper expected = new TypeWrapper();
        ParameterizedType type = Types.mapOf(UUID.class, Types.listOf(Integer.class));
        expected.setType(type);
        String json = mapper.writeValueAsString(expected);
        TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
        assertEquals(expected.getType(), actual.getType());
    }

  @Test
  public void shouldDeserializeMapWithMapWithListProperly() throws Exception {
    ObjectMapper mapper = BytecodeRouteParser.mapper;
    TypeWrapper expected = new TypeWrapper();
    ParameterizedType type = Types.mapOf(UUID.class, Types.mapOf(UUID.class, Types.listOf(Integer.class)));
    expected.setType(type);
    String json = mapper.writeValueAsString(expected);
    TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
    assertEquals(expected.getType(), actual.getType());
  }

  @Test
  public void shouldDeserializeListWithMapWithListProperly() throws Exception {
    ObjectMapper mapper = BytecodeRouteParser.mapper;
    TypeWrapper expected = new TypeWrapper();
    ParameterizedType type = Types.listOf(Types.mapOf(UUID.class, Types.mapOf(UUID.class, Types.listOf(Integer.class))));
    expected.setType(type);
    String json = mapper.writeValueAsString(expected);
    TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
    assertEquals(expected.getType(), actual.getType());
  }

}
