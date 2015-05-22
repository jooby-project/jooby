package org.jooby.json;

import org.jooby.MediaType;
import org.jooby.Renderer;

import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonRenderer implements Renderer {

  private ObjectMapper mapper;

  private MediaType type;

  public JacksonRenderer(final ObjectMapper mapper, final MediaType type) {
    this.mapper = mapper;
    this.type = type;
  }

  @Override
  public void render(final Object value, final Context ctx) throws Exception {
    if (ctx.accepts(type) && mapper.canSerialize(value.getClass())) {
      ctx.type(type);
      // use UTF-8 and get byte version
      byte[] bytes = mapper.writer().writeValueAsBytes(value);
      ctx.length(bytes.length)
          .send(bytes);
    }
  }

  @Override
  public String toString() {
    return "json";
  }

}
