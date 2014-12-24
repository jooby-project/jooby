package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jooby.Upload;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
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

    post("/form/files", (req, rsp) -> {
      List<Upload> uploads = req.param("uploads").toList(Upload.class);
      StringBuilder buffer = new StringBuilder();
      for (Upload upload : uploads) {
        try (Upload u = upload) {
          buffer.append(u.name()).append(" ");
        }
      }
      rsp.send(buffer);
    });

    post("/form/use/file", (req, rsp) -> {
      Upload upload = req.param("myfile").to(Upload.class);
      File file = upload.file();
      try (Upload u = upload) {
        assertEquals("p=1", Files.readAllLines(file.toPath()).stream()
            .collect(Collectors.joining("\n"))
        );
      }
      rsp.status(200);
    });

    post("/file/header", (req, rsp) -> {
      Upload upload = req.param("myfile").to(Upload.class);
      rsp.send(upload.header("content-type").stringValue());
    });

    post("/form/optional", (req, rsp) -> {
      Optional<Upload> upload = req.param("upload").toOptional(Upload.class);
      if (upload.isPresent()) {
        try (Upload u = upload.get()) {
          rsp.send(u.name());
        }
      } else {
        rsp.send(upload);
      }
    });

    use(Resource.class);
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
  public void useFile() throws Exception {
    assertEquals(
        "",
        Request
            .Post(uri("form", "use", "file").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addBinaryBody("myfile", "p=1".getBytes(), ContentType.DEFAULT_BINARY,
                        "application.conf")
                    .build()).execute().returnContent().asString());

  }

  @Test
  public void fileHeader() throws Exception {
    assertEquals(
        "application/json; charset=UTF-8",
        Request
            .Post(uri("file", "header").build())
            .body(
                MultipartEntityBuilder
                    .create()
                    .addBinaryBody("myfile", "{}".getBytes(), ContentType.APPLICATION_JSON,
                        "f.json")
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

    assertEquals("pom.xml", Request.Post(uri("form", "optional").build())
        .body(MultipartEntityBuilder.create()
            .addBinaryBody("upload", "".getBytes(), ContentType.APPLICATION_XML, "pom.xml")
            .build()).execute().returnContent().asString());

    assertEquals("pom.xml", Request.Post(uri("r", "form", "optional").build())
        .body(MultipartEntityBuilder.create()
            .addBinaryBody("upload", "".getBytes(), ContentType.APPLICATION_XML, "pom.xml")
            .build()).execute().returnContent().asString());
  }

}
