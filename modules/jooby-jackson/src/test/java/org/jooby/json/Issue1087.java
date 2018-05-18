package org.jooby.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.jooby.MediaType;
import org.jooby.Renderer.Context;
import org.jooby.test.MockUnit;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class Issue1087 {

  public static class Item {
    @JsonView(Views.Public.class)
    public int id = 1;

    @JsonView(Views.Public.class)
    public String itemName = "name";

    @JsonView(Views.Internal.class)
    public String ownerName = "owner";
  }

  public static class Views {
    public static class Public {
    }

    public static class Internal extends Public {
    }
  }

  @Test
  public void rendererNoView() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"id\":1,\"itemName\":\"name\",\"ownerName\":\"owner\"}";
    new MockUnit(Context.class, MediaType.class)
        .expect(json(json))
        .run(unit -> {
          new JacksonRenderer(mapper, MediaType.json)
              .render(new Item(), unit.get(Context.class));
        });
  }

  @Test
  public void rendererPublicView() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"id\":1,\"itemName\":\"name\"}";
    new MockUnit(Context.class, MediaType.class)
        .expect(json(json))
        .run(unit -> {
          new JacksonRenderer(mapper, MediaType.json)
              .render(new JacksonView<>(Views.Public.class, new Item()), unit.get(Context.class));
        });
  }

  @Test
  public void rendererInternalView() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"id\":1,\"itemName\":\"name\",\"ownerName\":\"owner\"}";
    new MockUnit(Context.class, MediaType.class)
        .expect(json(json))
        .run(unit -> {
          new JacksonRenderer(mapper, MediaType.json)
              .render(new JacksonView<>(Views.Internal.class, new Item()), unit.get(Context.class));
        });
  }

  private MockUnit.Block json(String json) {
    return unit-> {
      Context ctx = unit.get(Context.class);
      expect(ctx.accepts(MediaType.json)).andReturn(true);
      expect(ctx.type(MediaType.json)).andReturn(ctx);
      expect(ctx.length(json.length())).andReturn(ctx);
      ctx.send(EasyMock.aryEq(json.getBytes(StandardCharsets.UTF_8)));
    };
  }
}
