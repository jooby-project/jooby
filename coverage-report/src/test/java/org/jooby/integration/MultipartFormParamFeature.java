package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        assertEquals(true, myfile.file().exists());
        return name + " " + age + " " + upload.name() + " " + myfile.type().name();
      } finally {
        assertEquals(false, myfile.file().exists());
      }
    }

    @Path("/form/files")
    @POST
    public String multiplesFiles(final List<Upload> uploads) throws IOException {
      StringBuilder buffer = new StringBuilder();
      for (Upload upload : uploads) {
        try (Upload u = upload) {
          assertEquals(true, upload.file().exists());
          buffer.append(u.name()).append(" ");
        } finally {
          assertEquals(false, upload.file().exists());
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
      String name = req.param("name").value();
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
      rsp.send(upload.header("content-type").value());
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
    request()
        .post("/form")
        .multipart()
        .add("name", "edgar")
        .add("age", 34)
        .file("myfile", "<xml></xml>".getBytes(), "application/xml", "pom.xml")
        .expect("edgar 34 pom.xml application/xml");

    request()
        .post("/r/form")
        .multipart()
        .add("name", "edgar")
        .add("age", 34)
        .file("myfile", "<xml></xml>".getBytes(), "application/xml", "pom.xml")
        .expect("edgar 34 pom.xml application/xml");

  }

  @Test
  public void multipleFiles() throws Exception {
    request()
        .post("/form/files")
        .multipart()
        .file("uploads", "p=1".getBytes(), "application/octet-stream", "application.conf")
        .file("uploads", "p=2".getBytes(), "application/octet-stream", "m1.conf")
        .expect("application.conf m1.conf ");

    request()
        .post("/r/form/files")
        .multipart()
        .file("uploads", "p=1".getBytes(), "application/octet-stream", "application.conf")
        .file("uploads", "p=2".getBytes(), "application/octet-stream", "m1.conf")
        .expect("application.conf m1.conf ");
  }

  @Test
  public void useFile() throws Exception {
    request()
        .post("/form/use/file")
        .multipart()
        .file("myfile", "p=1".getBytes(), "application/octet-stream", "application.conf")
        .expect(200)
        .empty();
  }

  @Test
  public void fileHeader() throws Exception {
    request()
        .post("/file/header")
        .multipart()
        .file("myfile", "{}".getBytes(), "application/json;charset=UTF-8", "f.json")
        .expect(200)
        .expect("application/json; charset=UTF-8");
  }

  @Test
  public void optionalFile() throws Exception {
    request()
        .post("/form/optional")
        .multipart()
        .file("upload", "<xml></xml>".getBytes(), "application/xml", "pom.xml")
        .expect(200)
        .expect("pom.xml");

    request()
        .post("/r/form/optional")
        .multipart()
        .file("upload", "<xml></xml>".getBytes(), "application/xml", "pom.xml")
        .expect(200)
        .expect("pom.xml");

    request()
        .post("/form/optional")
        .multipart()
        .file("upload", "".getBytes(), "application/xml", "pom.xml")
        .expect(200)
        .expect("pom.xml");

    request()
        .post("/r/form/optional")
        .multipart()
        .file("upload", "".getBytes(), "application/xml", "pom.xml")
        .expect(200)
        .expect("pom.xml");
  }

}
