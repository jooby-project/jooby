package jooby;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jooby.mvc.POST;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;

public class MultipartFormParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/form")
    @POST
    public String form(final String name, final int age, final Upload myfile) throws IOException {
      try (Upload upload = myfile) {
        return name + " " + age + " " + upload.name() + " " + myfile.type().name();
      }
    }

    @Path("/form/files")
    @POST
    public String multiplesFiles(final List<Upload> uploads) throws IOException {
      StringBuilder buffer = new StringBuilder();
      for (Upload upload : uploads) {
        try (Upload u = upload) {
          buffer.append(u.name()).append(" ");
        }
      }
      return buffer.toString();
    }

    @Path("/form/optional")
    @POST
    public String optional(final Optional<Upload> upload) throws IOException {
      if (upload.isPresent()) {
        try (Upload u = upload.get()) {
          return u.name();
        }
      }
      return upload.toString();
    }
  }

  {

    post("/form", (req, resp) -> {
      String name = req.param("name").stringValue();
      int age = req.param("age").intValue();
      Upload upload = req.param("myfile").to(Upload.class);
      resp.send(name + " " + age + " " + upload.name() + " " + upload.type());
    });

    post("/form/files", (req, resp) -> {
      List<Upload> uploads = req.param("uploads").toList(Upload.class);
      StringBuilder buffer = new StringBuilder();
      for (Upload upload : uploads) {
        try (Upload u = upload) {
          buffer.append(u.name()).append(" ");
        }
      }
      resp.send(buffer);
    });

    post("/form/optional", (req, resp) -> {
      Optional<Upload> upload = req.param("upload").toOptional(Upload.class);
      if (upload.isPresent()) {
        try (Upload u = upload.get()) {
          resp.send(u.name());
        }
      } else {
        resp.send(upload);
      }
    });

    route(Resource.class);
  }

  @Test
  public void multipart() throws Exception {
    assertEquals("edgar 34 pom.xml application/xml",
        Request
            .Post(uri("form").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addTextBody("name", "edgar")
                    .addTextBody("age", "34")
                    .addBinaryBody("myfile", "<xml></xml>".getBytes(), ContentType.APPLICATION_XML,
                        "pom.xml")
                    .build()).execute().returnContent().asString());

    assertEquals("edgar 34 pom.xml application/xml",
        Request.Post(uri("r", "form").build())
            .body(MultipartEntityBuilder.create()
                .addTextBody("name", "edgar")
                .addTextBody("age", "34")
                .addBinaryBody("myfile", "<xml></xml>".getBytes(), ContentType.APPLICATION_XML,
                    "pom.xml")
                .build()).execute().returnContent().asString());

  }

  @Test
  public void multipleFiles() throws Exception {
    assertEquals(
        "application.conf m1.conf ",
        Request
            .Post(uri("form", "files").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addBinaryBody("uploads", "p=1".getBytes(), ContentType.DEFAULT_BINARY,
                        "application.conf")
                    .addBinaryBody("uploads", "p=2".getBytes(), ContentType.DEFAULT_BINARY,
                        "m1.conf")
                    .build()).execute().returnContent().asString());

    assertEquals(
        "application.conf m1.conf ",
        Request
            .Post(uri("r", "form", "files").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addBinaryBody("uploads", "p=1".getBytes(), ContentType.DEFAULT_BINARY,
                        "application.conf")
                    .addBinaryBody("uploads", "p=2".getBytes(), ContentType.DEFAULT_BINARY,
                        "m1.conf")
                    .build()).execute().returnContent().asString());

  }

  @Test
  public void optionalFile() throws Exception {
    assertEquals(
        "pom.xml",
        Request
            .Post(uri("form", "optional").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addBinaryBody("upload", "<xml></xml>".getBytes(), ContentType.APPLICATION_XML,
                        "pom.xml")
                    .build()).execute().returnContent().asString());

    assertEquals("pom.xml", Request.Post(uri("r", "form", "optional").build())
        .body(MultipartEntityBuilder.create()
            .addBinaryBody("upload", "<xml></xml>".getBytes(), ContentType.APPLICATION_XML,
                "pom.xml")
            .build()).execute().returnContent().asString());

    assertEquals("Optional.empty", Request.Post(uri("form", "optional").build())
        .body(MultipartEntityBuilder.create()
            .build()).execute().returnContent().asString());

    assertEquals("Optional.empty", Request.Post(uri("r", "form", "optional").build())
        .body(MultipartEntityBuilder.create()
            .build()).execute().returnContent().asString());
  }

}
