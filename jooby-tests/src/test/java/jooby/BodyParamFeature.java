package jooby;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import jooby.mvc.POST;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSink;
import com.google.common.io.CharStreams;
import com.google.inject.TypeLiteral;

public class BodyParamFeature extends ServerFeature {

  private static class Bean {

  }

  @Path("/r")
  public static class Resource {

    @Path("/body")
    @POST
    public Bean body(final Bean param) throws IOException {
      return param;
    }

  }

  {

    use(new BodyConverter() {

      @Override
      public void write(final Object body, final BodyWriter writer) throws Exception {
        writer.text(out -> new CharSink() {
          @Override
          public Writer openStream() throws IOException {
            return out;
          }
        }.write("{}"));
      }

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.json);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
        String body = reader.text(in ->CharStreams.toString(in));
        assertEquals("{}", body);
        return (T) new Bean();
      }

      @Override
      public boolean canWrite(final Class<?> type) {
        return type == Bean.class;
      }

      @Override
      public boolean canRead(final TypeLiteral<?> type) {
        return type.getRawType() == Bean.class;
      }
    });

    post("/body", (req, resp) -> {
      Bean bean = req.param("param").to(Bean.class);
      resp.send(bean);
    });

    use(Resource.class);
  }

  @Test
  public void multipart() throws Exception {
    assertEquals("{}",
        Request
            .Post(uri("body").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addTextBody("param", "{}", ContentType.APPLICATION_JSON)
                    .build()).execute().returnContent().asString());

    assertEquals("{}",
        Request.Post(uri("r", "body").build())
            .body(MultipartEntityBuilder.create()
                .addTextBody("param", "{}", ContentType.APPLICATION_JSON)
                .build()).execute().returnContent().asString());

  }

}
