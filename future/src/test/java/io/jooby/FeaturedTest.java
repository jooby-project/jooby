package io.jooby;

import io.jooby.jackson.Jackson;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.test.JoobyRunner;
import io.jooby.utow.Utow;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jooby.funzy.Throwing;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeaturedTest {

  private static String _19kb = readText(userdir("src", "test", "resources", "files", "19kb.txt"));

  private static MediaType json = MediaType.parse("application/json");

  private static MediaType textplain = MediaType.parse("text/plain");

  @Test
  public void sayHiFromIO() {
    new JoobyRunner(app -> {
      app.mode(Mode.IO);

      app.get("/", ctx -> "Hello World!");

      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(0, rsp.body().contentLength());
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>\n"
            + "Not Found (404) Not Found\n"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr><h2>message: Not Found (404)</h2>\n"
            + "<h2>status: Not Found (404)</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(609, rsp.body().contentLength());
      });
    }, new Netty(), new Utow(), new Jetty());

  }

  @Test
  public void sayHiFromWorker() {
    new JoobyRunner(app -> {
      app.mode(Mode.WORKER);

      app.get("/", ctx -> "Hello World!");

      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).ready(client -> {
      client.get("/?foo=bar", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(0, rsp.body().contentLength());
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>\n"
            + "Not Found (404) Not Found\n"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr><h2>message: Not Found (404)</h2>\n"
            + "<h2>status: Not Found (404)</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(609, rsp.body().contentLength());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void rawPath() {
    new JoobyRunner(app -> {

      app.get("/{code}", ctx -> ctx.path());

    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });

      client.get("/a+b", rsp -> {
        assertEquals("/a+b", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });

      client.get("/%2F", rsp -> {
        assertEquals("/%2F", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void httpMethods() {
    new JoobyRunner(app -> {

      app.get("/", ctx -> ctx.method());

      app.post("/", ctx -> ctx.method());

      app.put("/", ctx -> ctx.method());

      app.delete("/", ctx -> ctx.method());

      app.patch("/", ctx -> ctx.method());

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("GET", rsp.body().string());
      });

      client.post("/", rsp -> {
        assertEquals("POST", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.delete("/", rsp -> {
        assertEquals("DELETE", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.patch("/", rsp -> {
        assertEquals("PATCH", rsp.body().string());
      });

    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void gzip() throws IOException {
    String text = "Praesent blandit, justo a luctus elementum, ante sapien pellentesque tortor, "
        + "vitae maximus nulla augue sed nulla. Phasellus quis turpis ac mi tristique aliquam. "
        + "Suspendisse tellus sem, sollicitudin ac elit id, laoreet viverra erat. Proin libero "
        + "nulla, efficitur at facilisis a, placerat in ligula. Quisque sem dolor, efficitur ac "
        + "nisl ut, porttitor iaculis leo. Nullam auctor neque augue, quis malesuada nisl rhoncus "
        + "id. Quisque ut odio erat. Mauris pellentesque suscipit libero et laoreet. Nulla ipsum "
        + "enim, sodales in risus egestas, tempus pulvinar erat. Sed elementum, leo eget vulputate "
        + "commodo, ligula eros ullamcorper risus, at feugiat neque ipsum quis lectus. Nulla sit "
        + "amet lectus lacinia, congue sapien ac, vehicula lacus. Vestibulum vitae vestibulum enim. "
        + "Proin vulputate, quam ut commodo pellentesque, enim tortor ornare neque, a aliquam massa "
        + "felis a ligula. Pellentesque lorem erat, fringilla at ipsum a, scelerisque hendrerit "
        + "lorem. Sed interdum nibh at ante consequat, vitae fermentum augue luctus.";
    new JoobyRunner(app -> {

      app.get("/top", ctx -> text);

      app.gzip(() -> {
        app.get("/gzip", ctx -> text);
      });

      app.get("/bottom", ctx -> text);
    }).ready(client -> {

      Throwing.Consumer<Response> raw = rsp -> {
        assertEquals(1013, rsp.body().contentLength());
        assertEquals(null, rsp.header("content-encoding"));
        assertEquals(text, rsp.body().string());
      };

      client.get("/top", raw);

      client.get("/gzip").prepare(req -> {
        req.addHeader("Accept-Encoding", "gzip");
      }).execute(rsp -> {
        int min = 525;
        int max = 532;
        assertTrue(rsp.body().contentLength() == min || rsp.body().contentLength() == max);
        assertEquals("gzip", rsp.header("content-encoding"));
        assertEquals(text, ungzip(rsp.body().bytes()));
      });

      client.get("/bottom", raw);
    }, new Utow(), new Jetty());
  }

  private String ungzip(byte[] buff) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buff));
    Scanner scanner = new Scanner(gzip);
    StringBuilder str = new StringBuilder();
    while (scanner.hasNext()) {
      str.append(scanner.nextLine());
    }
    return str.toString();
  }

  @Test
  public void pathVarible() {
    new JoobyRunner(app -> {
      app.get("/articles/{id}", ctx -> ctx.param("id").intValue());

      app.get("/articles/*", ctx -> ctx.param("*").value());

      app.get("/file/*path", ctx -> ctx.param("path").value());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.param("nid").intValue());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.param("zid").intValue());

      app.get("/file/{file}.json", ctx -> ctx.param("file").value() + ".JSON");

      app.get("/file/{file}.{ext}",
          ctx -> ctx.param("file").value() + "." + ctx.param("ext").value());

      app.get("/profile/{pid}", ctx -> ctx.param("pid").value());

      app.get("/profile/me", ctx -> "me!");

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("tail/match", rsp.body().string());
      });

      client.get("/file/js/index.js", rsp -> {
        assertEquals("js/index.js", rsp.body().string());
      });

      client.get("/regex/678", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678/edit", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678d", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/file/foo.js", rsp -> {
        assertEquals("foo.js", rsp.body().string());
      });

      client.get("/file/foo.json", rsp -> {
        assertEquals("foo.JSON", rsp.body().string());
      });

      client.get("/profile/me", rsp -> {
        assertEquals("me!", rsp.body().string());
      });

      client.get("/profile/edgar", rsp -> {
        assertEquals("edgar", rsp.body().string());
      });
    }, new Utow());
  }

  @Test
  public void pathEncoding() {
    new JoobyRunner(app -> {
      app.get("/{value}", ctx -> ctx.path() + "@" + ctx.param("value").value());
    }).ready(client -> {
      client.get("/a+b", rsp -> {
        assertEquals("/a+b@a+b", rsp.body().string());
      });
      client.get("/a b", rsp -> {
        assertEquals("/a%20b@a b", rsp.body().string());
      });
      client.get("/%2F%2B", rsp -> {
        assertEquals("/%2F%2B@/+", rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void queryString() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> ctx.queryString() + "@" + ctx.query("q").value(""));

      app.get("/value", ctx -> ctx.query("user"));
    }).ready(client -> {
      client.get("/?q=a+b", rsp -> {
        assertEquals("?q=a+b@a b", rsp.body().string());
      });
      client.get("/", rsp -> {
        assertEquals("@", rsp.body().string());
      });

      client.get("/value?user.name=user&user.pass=pwd", rsp -> {
        assertEquals("{name=user, pass=pwd}", rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void form() {
    new JoobyRunner(app -> {
      app.post("/", ctx -> ctx.form());
    }).mode(Mode.IO, Mode.WORKER).ready(client -> {
      client.post("/", new FormBody.Builder()
          .add("q", "a b")
          .add("user.name", "user")
          .build(), rsp -> {
        assertEquals("{q=a b, user={name=user}}", rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void multipartFromWorker() {
    new JoobyRunner(app -> {
      app.post("/f", ctx -> {
        Value.Upload f = ctx.file("f");
        return f.filename() + "(type=" + f.contentType() + ";exists=" + Files.exists(f.path())
            + ")";
      });

      app.post("/files", ctx -> {
        List<Value.Upload> files = ctx.files("f");
        return files.stream().map(f -> f.filename() + "=" + f.filesize())
            .collect(Collectors.toList());
      });
    }).ready(client -> {
      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "fileupload.js",
              create(MediaType.parse("application/javascript"),
                  userdir("src", "test", "resources", "files", "fileupload.js").toFile()))
          .build(), rsp -> {
        assertEquals("fileupload.js(type=application/javascript;exists=true)", rsp.body().string());
      });

      client.post("/files", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("f", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("[f1.txt=5, f2.txt=5]", rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void multipartFromIO() {
    new JoobyRunner(app -> {
      app.post("/f", ctx -> ctx.multipart());

      app.dispatch(() ->
          app.post("/w", ctx -> ctx.multipart()));

      app.error(IllegalStateException.class, (ctx, x, statusCode) -> {
        ctx.send(x.getMessage());
      });
    }).mode(Mode.IO).ready(client -> {
      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("foo", "bar")
          .build(), rsp -> {
        assertEquals(
            "Attempted to do blocking IO from the IO thread. This is prohibited as it may result in deadlocks",
            rsp.body().string());
      });

      client.post("/w", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("foo", "bar")
          .build(), rsp -> {
        assertEquals("{foo=bar}", rsp.body().string());
      });
    }, new Netty(), new Utow());
  }

  @Test
  public void filter() {
    new JoobyRunner(app -> {

      app.before(ctx -> {
        StringBuilder buff = new StringBuilder();
        buff.append("before1:" + ctx.isInIoThread()).append(";");
        ctx.set("buff", buff);
      });

      app.after((ctx, value) -> {
        StringBuilder buff = (StringBuilder) value;
        buff.append("after1:" + ctx.isInIoThread()).append(";");
        return buff;
      });

      app.dispatch(() -> {
        app.before(ctx -> {
          StringBuilder buff = ctx.get("buff");
          buff.append("before2:" + ctx.isInIoThread()).append(";");
        });

        app.after((ctx, value) -> {
          StringBuilder buff = ctx.get("buff");
          buff.append(value).append(";");
          buff.append("after2:" + ctx.isInIoThread()).append(";");
          return buff;
        });

        app.get("/", ctx -> "result:" + ctx.isInIoThread());
      });

    }).mode(Mode.IO).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("before1:true;before2:false;result:false;after2:false;after1:false;",
            rsp.body().string());
      });
    }, new Netty(), new Utow()/* No Jetty bc always use a worker thread */);
  }

  @Test
  public void parser() {
    new JoobyRunner(app -> {
      app.converter(new Jackson());

      app.post("/map", ctx -> ctx.body(Map.class));

      app.post("/ints", ctx -> {
        List<Integer> ints = ctx.body(Reified.list(Integer.class));
        return ints;
      });

      app.post("/str", ctx -> ctx.body().string());
    }).ready(client -> {
      client.header("Content-Type", "application/json");
      client.post("/map", create(json, "{\"foo\": \"bar\"}"), rsp -> {
        assertEquals("{\"foo\":\"bar\"}", rsp.body().string());
      });

      client.header("Content-Type", "application/json");
      client.post("/ints", create(json, "[3, 4, 1]"), rsp -> {
        assertEquals("[3,4,1]", rsp.body().string());
      });

      client.post("/str", create(textplain, _19kb), rsp -> {
        assertEquals(_19kb, rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  private static String readText(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }
}
