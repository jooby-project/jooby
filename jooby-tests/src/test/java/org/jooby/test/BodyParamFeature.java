package org.jooby.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
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

    use(new Body.Parser() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> T parse(final TypeLiteral<T> type, final Body.Reader reader) throws Exception {
        String body = reader.text(in -> CharStreams.toString(in));
        assertEquals("{}", body);
        return (T) new Bean();
      }

      @Override
      public boolean canParse(final TypeLiteral<?> type) {
        return type.getRawType() == Bean.class;
      }

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.json);
      }
    });

    use(new Body.Formatter() {

      @Override
      public void format(final Object body, final Body.Writer writer) throws Exception {
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

      @Override
      public boolean canFormat(final Class<?> type) {
        return type == Bean.class;
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
