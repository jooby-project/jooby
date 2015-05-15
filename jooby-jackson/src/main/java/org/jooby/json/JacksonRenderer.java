package org.jooby.json;

import java.util.List;

import org.jooby.MediaType;
import org.jooby.Renderer;

import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonRenderer implements Renderer {

  private ObjectMapper mapper;

  private List<MediaType> types;

  public JacksonRenderer(final ObjectMapper mapper, final List<MediaType> types) {
    this.mapper = mapper;
    this.types = types;
  }

  @Override
  public void render(final Object object, final Context ctx) throws Exception {
    if (ctx.accepts(types) && mapper.canSerialize(object.getClass())) {
      ctx.type(types.get(0));
      // use UTF-8 and get byte version
      byte[] bytes = mapper.writer().writeValueAsBytes(object);
      ctx.length(bytes.length)
          .send(bytes);
    }
  }

  @Override
  public String toString() {
    return "json";
  }

}
