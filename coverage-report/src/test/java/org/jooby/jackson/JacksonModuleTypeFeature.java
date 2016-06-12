package org.jooby.jackson;

import java.io.IOException;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonModuleTypeFeature extends ServerFeature {

  @SuppressWarnings("serial")
  public static class Mod extends SimpleModule {

    public Mod() {
      addSerializer(Integer.class, new JsonSerializer<Integer>() {

        @Override
        public void serialize(final Integer value, final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException, JsonProcessingException {
          gen.writeRawValue(Integer.toString(value * 2));
        }

      });
    }
  }

  {
    use(new Jackson().module(Mod.class));

    get("/jackson-mod", () -> 2);
  }

  @Test
  public void shouldUseCustomModule() throws Exception {
    request().get("/jackson-mod").expect("4");
  }
}
