package org.jooby.json;

import java.util.List;

import org.jooby.MediaType;
import org.jooby.MediaType.Matcher;
import org.jooby.Parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.TypeLiteral;

class JacksonParser implements Parser {

  private ObjectMapper mapper;

  private Matcher matcher;

  public JacksonParser(final ObjectMapper mapper, final List<MediaType> types) {
    this.mapper = mapper;
    this.matcher = MediaType.matcher(types);
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Exception {
    JavaType javaType = mapper.constructType(type.getType());
    if (matcher.matches(ctx.type()) && mapper.canDeserialize(javaType)) {
      return ctx.body(body -> mapper.readValue(body.bytes(), javaType));
    }
    return ctx.next();
  }

  @Override
  public String toString() {
    return "json";
  }

}
