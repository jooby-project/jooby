package org.jooby.internal.apitool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.internal.MoreTypes;
import com.google.inject.util.Types;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

public class WildcardTypesTest {

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
    public void shouldDeserializeSubtypeProperly() throws Exception {
        ObjectMapper mapper = BytecodeRouteParser.mapper;
        TypeWrapper expected = new TypeWrapper();
        ParameterizedType type = Types.listOf(Types.subtypeOf(Integer.class));
        expected.setType(type);
        String json = mapper.writeValueAsString(expected);
        TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    public void shouldDeserializeSuperTypeProperly() throws Exception {
        ObjectMapper mapper = BytecodeRouteParser.mapper;
        TypeWrapper expected = new TypeWrapper();
        ParameterizedType type = Types.listOf(Types.supertypeOf(Integer.class));
        expected.setType(type);
        String json = mapper.writeValueAsString(expected);
        TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    public void shouldDeserializeUnknownTypeProperly() throws Exception {
        ObjectMapper mapper = BytecodeRouteParser.mapper;
        TypeWrapper expected = new TypeWrapper();
        ParameterizedType type = Types.listOf(new MoreTypes.WildcardTypeImpl(new Type[]{Object.class}, MoreTypes.EMPTY_TYPE_ARRAY));
        expected.setType(type);
        String json = mapper.writeValueAsString(expected);
        TypeWrapper actual = mapper.readValue(json, TypeWrapper.class);
        assertEquals(expected.getType(), actual.getType());
    }
}
