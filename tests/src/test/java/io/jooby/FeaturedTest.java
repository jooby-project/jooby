package io.jooby;

import io.jooby.freemarker.FreemarkerModule;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static io.jooby.ExecutionMode.DEFAULT;
import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.ExecutionMode.WORKER;
import static io.jooby.MediaType.text;
import static io.jooby.MediaType.xml;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reactor.core.scheduler.Schedulers.elastic;

public class FeaturedTest {

  private static String _19kb = readText(
      userdir("src", "test", "resources", "files", "19kb.txt"));

  private static String _16kb = _19kb.substring(0, ServerOptions._16KB);

  private static String _8kb = _16kb.substring(0, _16kb.length() / 2);

  private static MediaType json = MediaType.parse("application/json");

  private static MediaType textplain = MediaType.parse("text/plain");

  public static class Datafile {

    public final String name;

    public final FileUpload filename;

    public Datafile(String name, FileUpload filename) {
      this.name = name;
      this.filename = filename;
    }
  }

  public static class Datafiles {

    public final String name;

    public final List<FileUpload> filename;

    public Datafiles(String name, List<FileUpload> filename) {
      this.name = name;
      this.filename = filename;
    }
  }

  @ServerTest
  public void sayHi(ServerTestRunner runner) {
    runner.define(app -> {

      app.get("/", ctx -> "Hello World!");

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });
    });

  }

  @ServerTest(executionMode = EVENT_LOOP)
  public void sayHiFromIO(ServerTestRunner runner) {
    runner.define(app -> {
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
            + "<meta charset=\"utf-8\">\n"
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
            + "<title>Not Found (404)</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr>\n"
            + "<h2>message: /notFound</h2>\n"
            + "<h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(608, rsp.body().contentLength());
      });
    });
  }

  @ServerTest(executionMode = WORKER)
  public void sayHiFromWorker(ServerTestRunner runner) {
    runner.define(app -> {
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
            + "<meta charset=\"utf-8\">\n"
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
            + "<title>Not Found (404)</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr>\n"
            + "<h2>message: /notFound</h2>\n"
            + "<h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(608, rsp.body().contentLength());
      });
    });
  }

  @ServerTest
  public void rawPath(ServerTestRunner runner) {
    runner.define(app -> {

      app.get("/{code}", ctx -> ctx.getRequestPath());

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
    });
  }

  @ServerTest
  public void httpMethods(ServerTestRunner runner) {
    runner.define(app -> {

      app.get("/", ctx -> ctx.getMethod());

      app.post("/", ctx -> ctx.getMethod());

      app.put("/", ctx -> ctx.getMethod());

      app.delete("/", ctx -> ctx.getMethod());

      app.patch("/", ctx -> ctx.getMethod());

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

    });
  }

  @ServerTest
  public void gzip(ServerTestRunner runner) throws IOException {
    String text =
        "Praesent blandit, justo a luctus elementum, ante sapien pellentesque tortor, "
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
    runner.define(app -> {
      app.setServerOptions(new ServerOptions().setGzip(true));
      app.get("/gzip", ctx -> text);
    }).ready(client -> {
      client.get("/gzip").prepare(req -> {
        req.addHeader("Accept-Encoding", "gzip");
      }).execute(rsp -> {
        int min = 525;
        int max = 532;
        ResponseBody body = rsp.body();
        long len = body.contentLength();
        assertTrue(len == min || len == max, "Content-Length:" + len);
        assertEquals("gzip", rsp.header("content-encoding"));
        assertEquals(text, ungzip(body.bytes()));
      });

      // No ContentNegotiation-Encoding
      client.get("/gzip").prepare(req -> {
        // NOTE: required bc okhttp always set ContentNegotiation-Encoding to gzip
        req.addHeader("Accept-Encoding", "");
      }).execute(rsp -> {
        ResponseBody body = rsp.body();
        assertEquals(text, new String(body.bytes()));
        assertEquals(1013, body.contentLength());
        assertEquals(null, rsp.header("content-encoding"));
      });
    });
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

  @ServerTest
  public void pathVariable(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/articles/{id}", ctx -> ctx.path("id").intValue());

      app.get("/articles/*", ctx -> ctx.path("*").value());

      app.get("/file/*path", ctx -> ctx.path("path").value());

      app.get("/catchallWithVarPrefix/{id}/*path",
          ctx -> ctx.path("id").value() + ":" + ctx.path("path").value());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.path("nid").intValue());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.path("zid").intValue());

      app.get("/file/{file}.json", ctx -> ctx.path("file").value() + ".JSON");

      app.get("/file/{file}.{ext}",
          ctx -> ctx.path("file").value() + "." + ctx.path("ext").value());

      app.get("/profile/{pid}", ctx -> ctx.path("pid").value());

      app.get("/profile/me", ctx -> "me!");

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("tail/match", rsp.body().string());
      });

      client.get("/catchallWithVarPrefix/55/js/index.js", rsp -> {
        assertEquals("55:js/index.js", rsp.body().string());
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
    });
  }

  @ServerTest
  public void paramKeys(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/articles/{id}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/articles/*", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/*path", ctx -> ctx.getRoute().getPathKeys());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.getRoute().getPathKeys());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/{file}.json", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/{file}.{ext}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/profile/{pid}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/profile/me", ctx -> ctx.getRoute().getPathKeys());

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("[id]", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("[*]", rsp.body().string());
      });

      client.get("/file/js/index.js", rsp -> {
        assertEquals("[path]", rsp.body().string());
      });

      client.get("/regex/678", rsp -> {
        assertEquals("[nid]", rsp.body().string());
      });

      client.get("/regex/678/edit", rsp -> {
        assertEquals("[zid]", rsp.body().string());
      });

      client.get("/file/foo.js", rsp -> {
        assertEquals("[file, ext]", rsp.body().string());
      });

      client.get("/file/foo.json", rsp -> {
        assertEquals("[file]", rsp.body().string());
      });

      client.get("/profile/me", rsp -> {
        assertEquals("[]", rsp.body().string());
      });

      client.get("/profile/edgar", rsp -> {
        assertEquals("[pid]", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void pathEncoding(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/{value}", ctx -> ctx.getRequestPath() + "@" + ctx.path("value").value());
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
    });
  }

  @ServerTest
  public void queryString(ServerTestRunner runner) {
    runner.define(app -> {
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
    });
  }

  @ServerTest(executionMode = {EVENT_LOOP, WORKER})
  public void form(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/", ctx -> ctx.form());
    }).ready(client -> {
      client.post("/", new FormBody.Builder()
          .add("q", "a b")
          .add("user.name", "user")
          .build(), rsp -> {
        assertEquals("{q=a b, user={name=user}}", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void multipart(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/large", ctx -> {
        FileUpload f = ctx.file("f");
        return new String(f.bytes(), StandardCharsets.UTF_8);
      });

      app.post("/f", ctx -> {
        FileUpload f = ctx.file("f");
        return f.getFileName() + "(type=" + f.getContentType() + ";exists=" + Files
            .exists(f.path())
            + ")";
      });

      app.post("/files", ctx -> {
        List<FileUpload> files = ctx.files("f");
        return files.stream().map(f -> f.getFileName() + "=" + f.getFileSize())
            .collect(Collectors.toList());
      });

      app.post("/datafile", ctx -> {
        Datafile file = ctx.multipart(Datafile.class);
        String result = file.name + "=" + Files.exists(file.filename.path());
        return result;
      });

      app.post("/datafiles", ctx -> {
        Datafiles file = ctx.multipart(Datafiles.class);
        return file.name + "=" + file.filename;
      });

      app.post("/multipart", ctx -> {
        Multipart multipart = ctx.multipart();
        Map<String, List<String>> multimap = multipart.toMultimap();
        Map<String, Object> rsp = new LinkedHashMap<>();
        rsp.putAll(multimap);
        rsp.put("f", multipart.files("f"));
        return rsp;
      });
    }).ready(client -> {
      client.post("/large", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("f", "19kb.txt", create(_19kb, MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals(_19kb, rsp.body().string());
      });

      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "fileupload.js",
              create(userdir("src", "test", "resources", "files", "fileupload.js").toFile(),
                  MediaType.parse("application/javascript")))
          .build(), rsp -> {
        assertEquals("fileupload.js(type=application/javascript;exists=true)",
            rsp.body().string());
      });

      client.post("/files", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt", create("text1", MediaType.parse("text/plain")))
          .addFormDataPart("f", "f2.txt", create("text2", MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals("[f1.txt=5, f2.txt=5]", rsp.body().string());
      });

      client.post("/multipart", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt",
              create("text1", MediaType.parse("text/plain")))
          .addFormDataPart("f", "f2.txt",
              create("text2", MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals("{user.name=[user], f=[f1.txt, f2.txt]}", rsp.body().string());
      });

      client.post("/datafile", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "fileupload.js",
              create(userdir("src", "test", "resources", "files", "fileupload.js").toFile(),
                  MediaType.parse("application/javascript")))
          .build(), rsp -> {
        assertEquals("foo_report=true", rsp.body().string());
      });

      client.post("/datafiles", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "f1.txt",
              create("text1", MediaType.parse("text/plain")))
          .addFormDataPart("filename", "f2.txt",
              create("text2", MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals("foo_report=[f1.txt, f2.txt]", rsp.body().string());
      });
    });
  }

  /* No Jetty bc always use a worker thread */
  @ServerTest(server = {Netty.class, Utow.class}, executionMode = EVENT_LOOP)
  public void beforeAfter(ServerTestRunner runner) {
    runner.define(app -> {

      app.before(ctx -> {
        StringBuilder buff = new StringBuilder();
        buff.append("before1:" + ctx.isInIoThread()).append(";");
        ctx.attribute("buff", buff);
      });

      app.after((ctx, value, failure) -> {
        StringBuilder buff = (StringBuilder) value;
        buff.append("after1:" + ctx.isInIoThread()).append(";");
      });

      app.dispatch(() -> {
        app.before(ctx -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("before2:" + ctx.isInIoThread()).append(";");
        });

        app.after((ctx, value, failure) -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("after2:" + ctx.isInIoThread()).append(";");
        });

        app.get("/", ctx -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("result:").append(ctx.isInIoThread()).append(";");
          return buff;
        });
      });

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("before1:false;before2:false;result:false;after2:false;after1:false;",
            rsp.body().string());
      });
    });
  }

  @ServerTest
  public void decorator(ServerTestRunner runner) {
    runner.define(app -> {

      app.decorator(next -> ctx -> "{" + ctx.attribute("prefix") + next.apply(ctx) + "}");

      app.before(ctx -> {
        ctx.attribute("prefix", "%");
      });

      app.decorator(next -> ctx -> "<" + ctx.attribute("prefix") + next.apply(ctx) + ">");

      app.get("/decorator", ctx -> ctx.attribute("prefix") + "OK" + "%");

    }).ready(client -> {
      client.get("/decorator", rsp -> {
        assertEquals("{%<%%OK%>}",
            rsp.body().string());
      });
    });
  }

  @ServerTest
  public void decoder(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());

      app.post("/map", ctx -> ctx.body(Map.class));

      app.post("/toListInt", ctx -> {
        List<Integer> ints = ctx.body().toList(Integer.class);
        return ints;
      });

      app.post("/intBody", ctx -> {
        return ctx.body().to(Integer.class);
      });

      app.post("/str", ctx -> ctx.body().value());
    }).ready(client -> {
      client.header("Content-Type", "application/json");
      client.post("/map", create("{\"foo\": \"bar\"}", json), rsp -> {
        assertEquals("{\"foo\":\"bar\"}", rsp.body().string());
      });

      client.header("Content-Type", "application/json");
      client.post("/toListInt", create("[3, 4, 1]", json), rsp -> {
        assertEquals("[3,4,1]", rsp.body().string());
      });

      client.post("/intBody", create("678", textplain), rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.post("/str", create(_19kb, textplain), rsp -> {
        String value = rsp.body().string();
        assertEquals(_19kb, value);
      });
    });
  }

  @ServerTest
  public void jsonVsRawOutput(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());

      app.path("/api/pets", () -> {

        app.get("/{id}", ctx -> {
          ctx.setResponseType(io.jooby.MediaType.json);
          return "{\"message\": \"" + ctx.path("id") + "\"}";
        });

        app.get("/raw", ctx -> {
          ctx.setResponseType(io.jooby.MediaType.json);
          return "{\"message\": \"raw\"}".getBytes(StandardCharsets.UTF_8);
        });

        app.get("/", ctx -> {
          return Arrays.asList(mapOf("message", "fooo"));
        });
      });
    }).ready(client -> {
      client.get("/api/pets/fooo", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("{\"message\": \"fooo\"}", rsp.body().string());
      });

      client.get("/api/pets/raw", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("{\"message\": \"raw\"}", rsp.body().string());
      });

      client.get("/api/pets", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("[{\"message\":\"fooo\"}]", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void defaultAndExplicitAcceptableResponses(ServerTestRunner runner) {
    class Message {
      String value = "OK";

      @Override public String toString() {
        return value;
      }
    }

    runner.define(app -> {
      app.encoder(io.jooby.MediaType.json, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("{" + value.toString() + "}").getBytes(StandardCharsets.UTF_8)
      );

      app.encoder(io.jooby.MediaType.xml, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("<" + value.toString() + ">").getBytes(StandardCharsets.UTF_8)
      );

      app.get("/defaults", ctx -> {
        ctx.query("type").toOptional().ifPresent(ctx::setResponseType);
        return new Message();
      });

      app.get("/produces", ctx -> new Message())
          .produces(io.jooby.MediaType.json, io.jooby.MediaType.xml);

    }).ready(client -> {
      client.header("Accept", "application/json");
      client.get("/defaults", rsp -> {
        assertEquals("{OK}", rsp.body().string());
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "application/xml");
      client.get("/defaults", rsp -> {
        assertEquals("<OK>", rsp.body().string());
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "text/plain");
      client.get("/defaults", rsp -> {
        assertEquals("OK", rsp.body().string());
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "*/*");
      client.get("/defaults", rsp -> {
        assertEquals("{OK}", rsp.body().string());
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "text/html");
      client.get("/defaults", rsp -> {
        assertEquals(406, rsp.code());
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "text/html");
      client.get("/defaults?type=text/html", rsp -> {
        assertEquals("OK", rsp.body().string());
        assertEquals("text/html", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "application/json");
      client.get("/produces", rsp -> {
        assertEquals("{OK}", rsp.body().string());
        assertEquals("application/json;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "application/xml");
      client.get("/produces", rsp -> {
        assertEquals("<OK>", rsp.body().string());
        assertEquals("application/xml;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "*/*");
      client.get("/produces", rsp -> {
        assertEquals("{OK}", rsp.body().string());
        assertEquals("application/json;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "text/html");
      client.get("/produces", rsp -> {
        assertEquals(406, rsp.code());
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });

      client.header("Accept", "text/plain");
      client.get("/produces", rsp -> {
        assertEquals(406, rsp.code());
        assertEquals("text/plain;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });
    });
  }

  @ServerTest
  public void consumes(ServerTestRunner runner) {
    runner.define(app -> {
      app.decoder(io.jooby.MediaType.json, new MessageDecoder() {
        @Nonnull @Override public String decode(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return "{" + ctx.body().value() + "}";
        }
      });

      app.decoder(xml, new MessageDecoder() {
        @Nonnull @Override public String decode(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return "<" + ctx.body().value() + ">";
        }
      });

      app.get("/defaults", ctx -> {
        return ctx.body(String.class);
      });

      app.get("/consumes", ctx -> ctx.body(String.class))
          .consumes(io.jooby.MediaType.json, io.jooby.MediaType.xml);

    }).ready(client -> {
      client.header("Content-Type", "application/json");
      client.get("/defaults", rsp -> {
        assertEquals("{}", rsp.body().string());
      });
      client.header("Content-Type", "application/xml");
      client.get("/defaults", rsp -> {
        assertEquals("<>", rsp.body().string());
      });
      client.header("Content-Type", "text/plain");
      client.get("/defaults", rsp -> {
        assertEquals("", rsp.body().string());
      });

      client.header("Content-Type", "application/json");
      client.get("/consumes", rsp -> {
        assertEquals("{}", rsp.body().string());
      });
      client.header("Content-Type", "application/xml");
      client.get("/consumes", rsp -> {
        assertEquals("<>", rsp.body().string());
      });
      client.header("Content-Type", "text/plain");
      client.get("/consumes", rsp -> {
        assertEquals(415, rsp.code());
      });
    });
  }

  @ServerTest
  public void errorHandler(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());

      app.get("/", ctx -> {
        if (ctx.getRequestPath().length() != 0) {
          throw new IllegalArgumentException("Intentional error");
        }
        return "OK";
      });

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
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
            + "<title>Bad Request (400)</title>\n"
            + "<body>\n"
            + "<h1>Bad Request</h1>\n"
            + "<hr>\n"
            + "<h2>message: Intentional error</h2>\n"
            + "<h2>status code: 400</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
      });

      client.header("Accept", "application/json");
      client.get("/", rsp -> {
        assertEquals("application/json;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals(
            "{\"message\":\"Intentional error\",\"statusCode\":400,\"reason\":\"Bad Request\"}",
            rsp.body().string());
      });
    });
  }

  @ServerTest
  public void rx2(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/rx/flowable", ctx ->
          Flowable.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/observable", ctx ->
          Observable.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/single", ctx ->
          Single.fromCallable(() -> "Single")
              .map(s -> "Hello " + s)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/maybe", ctx ->
          Maybe.fromCallable(() -> "Maybe")
              .map(s -> "Hello " + s)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );

      app.get("/rx/nomaybe", ctx ->
          Maybe.empty()
              .subscribeOn(Schedulers.io())
      );

      app.get("/rx/flowable/each", ctx -> {
        Writer writer = ctx.responseWriter();
        return Flowable.range(1, 10)
            .map(i -> i + ",")
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .doOnError(ctx::sendError)
            .doFinally(writer::close)
            .forEach(it -> {
              writer.write(it);
            });
      });
    }).ready(client -> {
      client.get("/rx/flowable", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
      client.get("/rx/observable", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
      client.get("/rx/single", rsp -> {
        assertEquals("Hello Single", rsp.body().string());
      });
      client.get("/rx/maybe", rsp -> {
        assertEquals("Hello Maybe", rsp.body().string());
      });
      client.get("/rx/nomaybe", rsp -> {
        assertEquals(404, rsp.code());
        assertEquals("", rsp.body().string());
      });
      client.get("/rx/flowable/each", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void reactor(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/reactor/mono", ctx ->
          Mono.fromCallable(() -> "Mono")
              .map(s -> "Hello " + s)
              .subscribeOn(elastic())
      );
      app.get("/reactor/flux", ctx ->
          Flux.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(elastic())
      );
    }).ready(client -> {
      client.get("/reactor/mono", rsp -> {
        assertEquals("10", rsp.header("content-length").toLowerCase());
        assertEquals("Hello Mono", rsp.body().string());
      });
      client.get("/reactor/flux", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void completableFuture(ServerTestRunner runner) {
    runner.define(app -> {

      app.get("/completable", ctx ->
          supplyAsync(() -> "Completable Future!")
              .thenApply(v -> "Hello " + v)
      );

    }).ready(client -> {
      client.get("/completable", rsp -> {
        assertEquals("Hello Completable Future!", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void contextPath(ServerTestRunner runner) {
    runner.define(app -> {
      app.setContextPath("/foo");

      app.get("/", ctx -> ctx.getRequestPath());

      app.get("/bar", ctx -> ctx.getRequestPath());

    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });

      client.get("/foo/bar", rsp -> {
        assertEquals("/foo/bar", rsp.body().string());
      });

      client.get("/foo/barx", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/foo/favicon.ico", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  @ServerTest
  public void simpleRouterComposition(ServerTestRunner runner) {
    runner.define(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.get("/foo", Context::pathString);

      app.use(bar);

    }).ready(client -> {
      client.get("/bar", rsp -> {
        assertEquals("/bar", rsp.body().string());
      });

      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void dynamicRoutingComposition(ServerTestRunner runner) {
    runner.define(app -> {

      Jooby v1 = new Jooby();
      v1.get("/api", ctx -> "v1");

      Jooby v2 = new Jooby();
      v2.get("/api", ctx -> "v2");

      app.use(ctx -> ctx.header("version").value("").equals("v1"), v1);
      app.use(ctx -> ctx.header("version").value("").equals("v2"), v2);

      app.get("/api", ctx -> "fallback");
    }).ready(client -> {
      client.header("version", "v2");
      client.get("/api", rsp -> {
        assertEquals("v2", rsp.body().string());
      });

      client.header("version", "v1");
      client.get("/api", rsp -> {
        assertEquals("v1", rsp.body().string());
      });

      client.get("/api", rsp -> {
        assertEquals("fallback", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void prefixPathOnExistingRouter(ServerTestRunner runner) {
    runner.define(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.use("/prefix", bar);

    }).ready(client -> {
      client.get("/prefix/bar", rsp -> {
        assertEquals("/prefix/bar", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void compose(ServerTestRunner runner) {
    runner.define(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.path("/api", () -> {
        app.use(bar);

        app.use("/bar", bar);
      });

    }).ready(client -> {
      client.get("/api/bar", rsp -> {
        assertEquals("/api/bar", rsp.body().string());
      });
      client.get("/api/bar/bar", rsp -> {
        assertEquals("/api/bar/bar", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void routerCaseInsensitive(ServerTestRunner runner) {
    runner.define(app -> {
      app.setRouterOptions(RouterOption.IGNORE_CASE, RouterOption.IGNORE_TRAILING_SLASH);
      app.get("/foo", Context::pathString);

      app.get("/bar", Context::pathString);
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
      client.get("/foo/", rsp -> {
        assertEquals("/foo/", rsp.body().string());
      });
      client.get("/fOo", rsp -> {
        assertEquals("/fOo", rsp.body().string());
      });

      client.get("/bar", rsp -> {
        assertEquals("/bar", rsp.body().string());
      });
      client.get("/BAR", rsp -> {
        assertEquals("/BAR", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void routerCaseSensitive(ServerTestRunner runner) {
    /** Now do it case sensitive: */
    runner.define(app -> {

      app.get("/foo", Context::pathString);

      app.get("/BAR", Context::pathString);
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
      client.get("/fOo", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/bar", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/BAR", rsp -> {
        assertEquals("/BAR", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void maxRequestSize(ServerTestRunner runner) {
    runner.define(app -> {
      app.setServerOptions(new ServerOptions()
          .setBufferSize(ServerOptions._16KB / 2)
          .setMaxRequestSize(ServerOptions._16KB));
      app.post("/request-size", ctx -> ctx.body().value());

      app.get("/request-size", ctx -> ctx.body().value());
    }).ready(client -> {
      // Exceeds
      client.post("/request-size", RequestBody.create(_19kb, MediaType.get("text/plain")), rsp -> {
        assertEquals(413, rsp.code());
      });
      // Chunk by chunk
      client.post("/request-size", RequestBody.create(_16kb, MediaType.get("text/plain")), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals(_16kb, rsp.body().string());
      });
      // Single read
      client.post("/request-size", RequestBody.create(_8kb, MediaType.get("text/plain")), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals(_8kb, rsp.body().string());
      });
      // Empty
      client.post("/request-size", RequestBody.create("", MediaType.get("text/plain")), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("", rsp.body().string());
      });
      // No body
      client.get("/request-size", rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void trailinSlashIsANewRoute(ServerTestRunner runner) {
    runner.define(app -> {
      app.setRouterOptions(RouterOption.IGNORE_TRAILING_SLASH);
      app.get("/foo/", ctx -> "foo/");

      app.get("/foo", ctx -> "new foo");
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("new foo", rsp.body().string());
      });
      client.get("/foo/", rsp -> {
        assertEquals("new foo", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void methodNotAllowed(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/method", Context::pathString);
    }).ready(client -> {
      client.get("/method", rsp -> {
        assertEquals(StatusCode.METHOD_NOT_ALLOWED.value(), rsp.code());
        assertEquals("POST", rsp.header("Allow"));
      });
    });
  }

  @ServerTest
  public void silentFavicon(ServerTestRunner runner) {
    runner.use(Jooby::new).ready(client -> {
      client.get("/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
      client.get("/foo/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
    });
  }

  @ServerTest
  public void customHttpMethod(ServerTestRunner runner) {
    runner.define(app -> {
      app.route("foo", "/bar", Context::getMethod);
    }).ready(client -> {
      client.invoke("foo", "/bar").execute(rsp -> {
        assertEquals("FOO", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void setContentType(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/plain", ctx -> ctx.setResponseType(text).send("Text"));
    }).ready(client -> {
      client.get("/plain", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("Text", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void setContentLen(ServerTestRunner runner) {
    String value = "...";
    runner.define(app -> {
      app.get("/len",
          ctx -> ctx.setResponseType(text).setResponseLength(value.length()).send(value));
    }).ready(client -> {
      client.get("/len", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("...", rsp.body().string());
        assertEquals(3L, rsp.body().contentLength());
      });
    });
  }

  @ServerTest
  public void sendStatusCode(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/statuscode", ctx -> ctx.send(StatusCode.OK));
    }).ready(client -> {
      client.get("/statuscode", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(0L, rsp.body().contentLength());
      });
    });
  }

  @ServerTest(server = {Utow.class, Netty.class}, executionMode = DEFAULT)
  public void mixedMode(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/blocking", ctx -> !ctx.isInIoThread());
      app.get("/reactive", ctx ->
          Single.fromCallable(() -> ctx.isInIoThread())
      );
    }).ready(client -> {
      client.get("/blocking", rsp -> {
        assertEquals("true", rsp.body().string());
      });
      client.get("/reactive", rsp -> {
        assertEquals("true", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void defaultHeaders(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/", Context::pathString);
    }).ready(client -> {
      String serverHeader = String.valueOf(runner.getServer().charAt(0));
      client.get("/", rsp -> {
        assertNotNull(rsp.header("Date"));
        assertEquals(serverHeader, rsp.header("Server"));
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
      });
    });
  }

  @ServerTest
  public void sendStream(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/txt", ctx -> {
        ctx.query("l").toOptional().ifPresent(len -> ctx.setResponseLength(Long.parseLong(len)));
        return new ByteArrayInputStream(_19kb.getBytes(StandardCharsets.UTF_8));
      });
    }).ready(client -> {
      client.get("/txt", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/txt?l=" + _19kb.length(), rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()), rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });
    });
  }

  @ServerTest
  public void sendStreamRange(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/range", ctx -> {
        ctx.setResponseLength(_19kb.length());
        return ctx.send(new ByteArrayInputStream(_19kb.getBytes(StandardCharsets.UTF_8)));
      });
    }).ready(client -> {
      client.header("Range", "bytes=-");
      client.get("/range", rsp -> {
        assertEquals(416, rsp.code());
      });

      client.header("Range", "bytes=0-99");
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-99/" + _19kb.length(), rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, 100), rsp.body().string());
      });

      client.header("Range", "bytes=-100");
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals(
            "bytes " + (_19kb.length() - 100) + "-" + (_19kb.length() - 1) + "/" + _19kb.length(),
            rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - 100), rsp.body().string());
      });

      int last = _16kb.length() + 5;
      client.header("Range", "bytes=-" + last);
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals(
            "bytes " + (_19kb.length() - last) + "-" + (_19kb.length() - 1) + "/" + _19kb.length(),
            rsp.header("content-range"));
        assertEquals(Integer.toString(last), rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - last), rsp.body().string());
      });
      client.header("Range", "bytes=0-" + last);
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-" + last + "/" + _19kb.length(), rsp.header("content-range"));
        assertEquals(Integer.toString(last + 1), rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, last + 1), rsp.body().string());
      });
    });
  }

  @ServerTest
  public void sendFile(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/filechannel", ctx ->
          FileChannel.open(userdir("src", "test", "resources", "files", "19kb.txt"))
      );
      app.get("/path", ctx ->
          userdir("src", "test", "resources", "files", "19kb.txt")
      );
      app.get("/file", ctx ->
          userdir("src", "test", "resources", "files", "19kb.txt").toFile()
      );
      app.get("/filenotfound", ctx ->
          userdir("src", "test", "resources", "files", "notfound.txt")
      );
      app.get("/attachment", ctx -> {
        Path file = userdir("src", "test", "resources", "files", "19kb.txt");
        return new AttachedFile(file, ctx.query("name").value(file.getFileName().toString()));
      });
    }).ready(client -> {
      client.get("/filechannel", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/path", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/file", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/filenotfound", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/attachment", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals("text/plain;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("attachment;filename=\"19kb.txt\"",
            rsp.header("content-disposition").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/attachment?name=foo+bar.txt", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals("text/plain;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("attachment;filename=\"foo bar.txt\";filename*=utf-8''foo%20bar.txt",
            rsp.header("content-disposition").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });
    });
  }

  @ServerTest
  public void sendFileRange(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/file-range", ctx -> {
        ctx.setResponseLength(_19kb.length());
        return ctx
            .send(FileChannel.open(userdir("src", "test", "resources", "files", "19kb.txt")));
      });
    }).ready(client -> {
      client.header("Range", "bytes=0-99");
      client.get("/file-range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-99/" + _19kb.length(), rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, 100), rsp.body().string());
      });

      client.header("Range", "bytes=-100");
      client.get("/file-range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals(
            "bytes " + (_19kb.length() - 100) + "-" + (_19kb.length() - 1) + "/" + _19kb.length(),
            rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - 100), rsp.body().string());
      });
    });
  }

  @ServerTest
  public void writer(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/19kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_19kb)) {
              transfer(reader, writer);
            }
          })
      );

      app.get("/16kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_16kb)) {
              transfer(reader, writer);
            }
          })
      );

      app.get("/8kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_8kb)) {
              transfer(reader, writer);
            }
          })
      );

    }).ready(client -> {
      // Large responses always send as chunk
      client.get("/19kb", rsp -> {
        assertEquals(_19kb, rsp.body().string());
        assertEquals("chunked", rsp.header("Transfer-Encoding").toLowerCase());
        assertEquals(null, rsp.header("Content-Length"));
      });
      // Responses <= 16kb may send content-length or chunks (server specific), but not both.
      client.get("/16kb", rsp -> {
        assertEquals(_16kb, rsp.body().string());
        AtomicInteger i = new AtomicInteger();
        Optional.ofNullable(rsp.header("Content-Length")).ifPresent(value -> {
          assertEquals("16384", value);
          i.incrementAndGet();
        });
        Optional.ofNullable(rsp.header("Transfer-Encoding")).ifPresent(value -> {
          assertEquals("chunked", value.toLowerCase());
          i.incrementAndGet();
        });
        assertEquals(1, i.get());
      });
      client.get("/8kb", rsp -> {
        assertEquals(_8kb, rsp.body().string());
        AtomicInteger i = new AtomicInteger();
        Optional.ofNullable(rsp.header("Content-Length")).ifPresent(value -> {
          assertEquals("8192", value);
          i.incrementAndGet();
        });
        Optional.ofNullable(rsp.header("Transfer-Encoding")).ifPresent(value -> {
          assertEquals("chunked", value.toLowerCase());
          i.incrementAndGet();
        });
        assertEquals(1, i.get());
      });
    });
  }

  @ServerTest
  public void lifeCycle(ServerTestRunner runner) {
    AtomicInteger counter = new AtomicInteger();

    runner.define(app -> {
      app.onStarting(() -> counter.incrementAndGet());

      app.onStarting(() -> counter.incrementAndGet());

      app.onStop(() -> {
        counter.decrementAndGet();
      });

      app.onStop(() -> {
        counter.decrementAndGet();
        throw new IllegalStateException("intentional error");
      });

      app.get("/lifeCycle", ctx -> counter.get());
    }).ready(client -> {
      client.get("/lifeCycle", rsp ->
          assertEquals("2", rsp.body().string())
      );
    });
  }

  @ServerTest
  public void sendRedirect(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/", ctx -> ctx.sendRedirect("/foo"));
      app.get("/foo", Context::pathString);
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void cookies(ServerTestRunner runner) {
    DateTimeFormatter fmt = DateTimeFormatter
        .ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
        .withZone(ZoneId.of("GMT"));

    runner.define(app -> {
      app.get("/cookies", ctx -> ctx.cookieMap());

      app.get("/set-cookie", ctx -> {
        ctx.queryMap().entrySet().stream()
            .map(e -> new Cookie(e.getKey(), e.getValue()))
            .forEach(ctx::setResponseCookie);
        return ctx.send(StatusCode.OK);
      });

      app.get("/max-age", ctx -> {
        Cookie cookie = new Cookie("foo", "bar")
            .setMaxAge(ctx.query("maxAge").longValue(0));
        return ctx
            .setResponseCookie(cookie)
            .send(StatusCode.OK);
      });
    }).ready(client -> {
      client.get("/cookies", response -> {
        assertEquals("{}", response.body().string());
      });
      client.header("Cookie", "foo=bar");
      client.get("/cookies", response -> {
        assertEquals("{foo=bar}", response.body().string());
      });
      client.header("Cookie", "foo=bar; x=y");
      client.get("/cookies", response -> {
        assertEquals("{foo=bar, x=y}", response.body().string());
      });
      client.header("Cookie", "$Version=1; X=x; $Path=/set;");
      client.get("/cookies", response -> {
        assertEquals("{X=x}", response.body().string());
      });
      // response cookies
      client.get("/set-cookie", response -> {
        assertEquals(null, response.header("Set-Cookie"));
      });
      client.get("/set-cookie?foo=bar", response -> {
        assertEquals("foo=bar;Path=/", response.header("Set-Cookie"));
      });
      client.get("/set-cookie?foo=bar&x=y", response -> {
        assertEquals("[foo=bar;Path=/, x=y;Path=/]", response.headers("Set-Cookie").toString());
      });
      // max-age
      client.get("/max-age", response -> {
        // expires
        assertEquals("[foo=bar;Path=/;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT]",
            response.headers("Set-Cookie").toString());
      });
      client.get("/max-age?maxAge=-1", response -> {
        // browser session
        assertEquals("[foo=bar;Path=/]", response.headers("Set-Cookie").toString());
      });
      Instant date = Instant.now();
      client.get("/max-age?maxAge=" + Duration.ofMinutes(30).getSeconds(), response -> {
        // Expire in 30 minutes from now on
        String setCookie = response.header("Set-Cookie");
        String prefix = "foo=bar;Path=/;";
        assertTrue(setCookie.startsWith(prefix));
        setCookie = setCookie.substring(prefix.length());
        String maxAge = "Max-Age=1800;Expires=";
        assertTrue(setCookie.startsWith(maxAge));
        setCookie = setCookie.substring(maxAge.length());
        Instant expires = Instant.from(fmt.parse(setCookie));
        long minutes = Duration.between(date, expires).toMinutes();
        // Give it -/+5
        assertTrue(minutes >= 25 && minutes <= 35);
      });
    });
  }

  @ServerTest
  public void varOnCatchAll(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/*", Context::pathMap);

      app.get("/query/?*x", Context::pathMap);

      app.get("/search/*q", Context::pathMap);
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("{}", rsp.body().string());
      });
      client.get("/x", rsp -> {
        assertEquals("{*=x}", rsp.body().string());
      });
      client.get("/search", rsp -> {
        assertEquals("{*=search}", rsp.body().string());
      });
      client.get("/search/x", rsp -> {
        assertEquals("{q=x}", rsp.body().string());
      });
      client.get("/query/y", rsp -> {
        assertEquals("{x=y}", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void singlePageApp(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/?*",
          new AssetHandler("fallback.html", AssetSource.create(app.getClassLoader(), "/www")));

    }).ready(client -> {
      client.get("/docs", rsp -> {
        assertEquals("fallback.html", rsp.body().string().trim());
      });

      client.get("/docs/index.html", rsp -> {
        assertEquals("fallback.html", rsp.body().string().trim());
      });

      client.get("/docs/v1", rsp -> {
        assertEquals("fallback.html", rsp.body().string().trim());
      });

      client.get("/", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void staticSiteFromCpWithPrefix(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/?*", "/www");

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/about.html", rsp -> {
        assertEquals("about.html", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void staticSiteFromCp(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/?*");

    }).ready(client -> {
      client.get("/www", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/www/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/www/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/www/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/www/about.html", rsp -> {
        assertEquals("about.html", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void staticSiteFromCpWithPrefixPathAndPrefixLocation(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/static/?*", "/www");

    }).ready(client -> {
      client.get("/static", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/static/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/static/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/static/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/static/about.html", rsp -> {
        assertEquals("about.html", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void staticSiteFromFs(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/?*", userdir("src", "test", "resources", "www"));

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/about.html", rsp -> {
        assertEquals("about.html", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void staticSiteFromFsUsingPathPrefix(ServerTestRunner runner) {
    runner.define(app -> {

      app.assets("/static/?*", userdir("src", "test", "resources", "www"));

    }).ready(client -> {
      client.get("/static", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/static/index.html", rsp -> {
        assertEquals("index.html", rsp.body().string().trim());
      });

      client.get("/static/note", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/static/note/index.html", rsp -> {
        assertEquals("note.html", rsp.body().string().trim());
      });

      client.get("/static/about.html", rsp -> {
        assertEquals("about.html", rsp.body().string().trim());
      });
    });
  }

  private static final String VUE = vueVersion();

  private static String vueVersion() {
    try (InputStream vueprops = FeaturedTest.class.getClassLoader()
        .getResourceAsStream("META-INF/maven/org.webjars.npm/vue/pom.properties")) {
      Properties properties = new Properties();
      properties.load(vueprops);
      return properties.getProperty("version");
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @ServerTest
  public void assets(ServerTestRunner runner) throws IOException {

    String cl1 = String.valueOf(userdir("src", "test", "www", "js", "index.js").toFile().length());
    String cl2 = String
        .valueOf(getClass().getResource("/www/foo.js").openConnection().getContentLength());
    String cl4 = String
        .valueOf(userdir("src", "test", "www", "css", "styles.css").toFile().length());
    String cl5 = String.valueOf(userdir("src", "test", "www", "index.html").toFile().length());
    String vueSize = String.valueOf(
        getClass().getResource("/META-INF/resources/webjars/vue/" + VUE + "/dist/vue.js")
            .openConnection().getContentLength());
    runner.define(app -> {
      app.assets("/static/?*", userdir("src", "test", "www"));
      app.assets("/*", userdir("src", "test", "www"));
      app.assets("/cp/*", "/www");
      app.assets("/jar/*", "/META-INF/resources/webjars/vue/" + VUE);
      app.assets("/jar2/*", "/META-INF/resources/webjars/vue/" + VUE + "/dist");

      app.assets("/m/*", AssetSource.create(userdir("src", "test", "www")),
          AssetSource.create(getClass().getClassLoader(), "/www"));

      app.assets("/fsfile.js", userdir("src", "test", "www", "js", "index.js"));
      app.assets("/cpfile.js", "/www/foo.js");

    }).ready(client -> {
      client.get("/cpfile.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl2, rsp.header("Content-Length").toLowerCase());
      });
      // single file
      client.get("/fsfile.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl1, rsp.header("Content-Length").toLowerCase());
      });
      /** Multiple sources on same path: */
      client.get("/m/foo.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl2, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/m/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl1, rsp.header("Content-Length").toLowerCase());
        assertEquals(200, rsp.code());

        String etag = rsp.header("etag");
        client.header("If-None-Match", etag);
        client.get("/m/js/index.js", etagrsp -> {
          assertEquals(null, etagrsp.header("etag"));
          assertEquals(304, etagrsp.code());
        });

        String lastModified = rsp.header("Last-Modified");
        client.header("If-Modified-Since", lastModified);
        client.get("/m/js/index.js", rsp2 -> {
          assertEquals(null, rsp2.header("Last-Modified"));
          assertEquals(304, rsp2.code());
        });
      });
      /** Project classpath: */
      client.get("/cp/foo.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl2, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/cp", rsp -> {
        assertEquals(404, rsp.code());
      });
      /** File system: */

      client.get("/", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl5, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl5, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/index.html", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl5, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/note", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("note.html", rsp.body().string().trim());
      });
      client.get("/static/note/index.html", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("note.html", rsp.body().string().trim());
      });
      client.get("/static/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl1, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/css/styles.css", rsp -> {
        assertEquals("text/css;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl4, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/../resources/fileupload.js", rsp -> {
        assertEquals(404, rsp.code());
      });

      /* ROOT: */
      client.get("/index.html", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl5, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(cl1, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/css/styles.css", rsp -> {
        assertEquals("text/css;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals(cl4, rsp.header("Content-Length").toLowerCase());
      });

      // Inside jar
      client.get("/jar/dist/vue.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals(vueSize, rsp.header("Content-Length").toLowerCase());
      });
      client.get("/jar2/dist/../package.json", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/jar/dist/nope.js", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/jar/dist", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  @ServerTest
  public void assetSingleFile(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/asset/cp", "singleroot");
      app.assets("/asset/fs", userdir("src", "test", "resources", "singleroot"));
    }).ready(client -> {
      client.get("/asset/cp", rsp -> {
        assertEquals("abcd", rsp.body().string().trim());
      });
      client.get("/asset/fs", rsp -> {
        assertEquals("abcd", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void services(ServerTestRunner runner) {
    runner.define(app -> {
      ServiceRegistry services = app.getServices();
      services.put(ServiceKey.key(String.class, "x"), "value");

      app.get("/services", ctx -> app.require(String.class, "x"));
    }).ready(client -> {
      client.get("/services", rsp -> {
        assertEquals("value", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void render(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());
      app.get("/int", ctx -> ctx.render(1));
      app.get("/bytes", ctx -> ctx.render("bytes".getBytes(StandardCharsets.UTF_8)));
      app.get("/stream",
          ctx -> ctx.render(new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8))));
      app.get("/complex", ctx -> ctx.render(mapOf("k", "v")));
    }).ready(client -> {
      client.get("/int", rsp -> {
        assertEquals("1", rsp.body().string());
      });
      client.get("/bytes", rsp -> {
        assertEquals("bytes", rsp.body().string());
      });
      client.get("/stream", rsp -> {
        assertEquals("bytes", rsp.body().string());
      });
      client.get("/complex", rsp -> {
        assertEquals("{\"k\":\"v\"}", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void flashScope(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/flash", req -> req.flash());

      app.post("/flash", ctx -> {
        ctx.flash().put("success", "Thank you!");
        return ctx.sendRedirect("/flash");
      });

      app.post("/untouch", Context::pathString);

      app.get("/error", ctx -> {
        boolean noreset = !ctx.query("noreset").isMissing();
        if (noreset) {
          ctx.setResetHeadersOnError(false);
        }
        ctx.flash().put("error", "error");
        return ctx.query("error").value();
      });

      app.error((ctx, cause, statusCode) -> {
        ctx.setResponseCode(statusCode).send(cause.getMessage());
      });

    }).dontFollowRedirects().ready(client -> {
      client.post("/flash", rsp -> {
        assertEquals(302, rsp.code());
        String setCookie = rsp.header("Set-Cookie");
        assertEquals("jooby.flash=success=Thank+you%21;Path=/;HttpOnly", setCookie);

        client.header("Cookie", setCookie).get("/flash", next -> {
          assertEquals(200, next.code());
          assertEquals("{success=Thank you!}", next.body().string());
          String clearCookie = next.header("Set-Cookie");
          assertTrue(clearCookie.startsWith("jooby.flash=;Path=/;HttpOnly;Max-Age=0;"),
              clearCookie);
        });
      });

      client.get("/flash", rsp -> {
        assertEquals("{}", rsp.body().string());
        assertNull(rsp.header("Set-Cookie"));
      });

      client.get("/untouch", rsp ->
          assertNull(rsp.header("Set-Cookie"))
      );

      client.get("/error", rsp ->
          assertNull(rsp.header("Set-Cookie"))
      );

      client.get("/error?noreset=true", rsp ->
          assertNotNull(rsp.header("Set-Cookie"))
      );
    });
  }

  @ServerTest
  public void customFlashScope(ServerTestRunner runner) {
    runner.define(app -> {
      app.setContextPath("/custom");
      app.setFlashCookie("f");

      app.get("/flash", ctx -> {
        ctx.flash().put("success", "Thank you!");
        return ctx.flash();
      });
    }).ready(client -> {
      client.get("/custom/flash", rsp -> {
        assertEquals(200, rsp.code());
        String setCookie = rsp.header("Set-Cookie");
        assertEquals("f=success=Thank+you%21;Path=/custom;HttpOnly", setCookie);
      });
    });
  }

  @ServerTest
  public void flashScopeKeep(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/flash", ctx -> {
        FlashMap flash = ctx.flash();
        flash.put("foo", "bar");
        return ctx.sendRedirect("/flash/1");
      });

      app.get("/flash/1", ctx -> {
        FlashMap flash = ctx.flash();
        flash.keep();
        return ctx.sendRedirect("/flash/" + flash.get("foo"));
      });

      app.get("/flash/bar", ctx -> {
        return ctx.flash("foo").value() + ctx.flash().size();
      });
      app.error((ctx, cause, statusCode) -> {
        ctx.setResponseCode(statusCode).send(cause.getMessage());
      });

    }).dontFollowRedirects().ready(client -> {
      client.get("/flash", rsp -> {
        assertEquals(302, rsp.code());
        client.header("Cookie", rsp.header("Set-Cookie")).get(rsp.header("Location"), r1 -> {
          assertEquals(302, r1.code());
          assertEquals("/flash/bar", r1.header("Location"));

          client.header("Cookie", r1.header("Set-Cookie")).get(r1.header("Location"), r2 -> {
            assertEquals(200, r2.code());
            assertEquals("bar1", r2.body().string());

            String clearCookie = r2.header("Set-Cookie");
            assertTrue(clearCookie.startsWith("jooby.flash=;Path=/;HttpOnly;Max-Age=0;"),
                clearCookie);
          });
        });
      });
    });
  }

  @ServerTest
  public void templateEngines(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new HandlebarsModule());
      app.install(new FreemarkerModule());

      app.get("/1", ctx -> new ModelAndView("index.hbs").put("name", "Handlebars"));
      app.get("/2", ctx -> new ModelAndView("index.ftl").put("name", "Freemarker"));
    }).ready(client -> {
      client.get("/1", rsp -> {
        assertEquals("Hello Handlebars!", rsp.body().string().trim());
      });
      client.get("/2", rsp -> {
        assertEquals("Hello Freemarker!", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void accessToFlashScopeFromTemplateEngine(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new HandlebarsModule());
      app.get("/home", ctx -> new ModelAndView("flash.hbs"));
    }).dontFollowRedirects().ready(client -> {
      client.header("Cookie", "jooby.flash=name=Flash");
      client.get("/home", rsp -> {
        assertEquals("Hello Flash!", rsp.body().string().trim());
      });
    });
  }

  // TODO: We might add this back but using a different implementation:
  //  @ServerTest(executionMode = EVENT_LOOP)
  @DisplayName("Context detaches when running in event-loop and returns a Context")
  public void detachOnEventLoop(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/detach", ctx -> {
        CompletableFuture.runAsync(() -> {
          ctx.send(ctx.getRequestPath());
        });
        return ctx;
      });
    }).ready(client -> {
      client.get("/detach", rsp -> {
        assertEquals("/detach", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void rawValue(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/body", ctx -> {
        return ctx.body().value();
      });

      app.post("/form", ctx -> {
        return ctx.form().value();
      });

      app.post("/multipart", ctx -> {
        return ctx.multipart().value();
      });
    }).ready(client -> {
      client.post("/body", rsp -> {
        assertEquals("", rsp.body().string());
      });

      client.post("/body", create("{\"foo\": \"bar\"}", json), rsp -> {
        assertEquals("{\"foo\": \"bar\"}", rsp.body().string());
      });

      client
          .post("/form",
              new FormBody.Builder().add("a", "a b").add("c", "d").add("c", "e").build(),
              rsp -> {
                assertEquals("a=a b&c=d&c=e", rsp.body().string());
              });

      client.post("/multipart", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("a", "b")
          .addFormDataPart("f", "19kb.txt", create(_19kb, MediaType.parse("text/plain")))
          .build(), rsp -> {
        assertEquals("a=b", rsp.body().string());
      });
    });
  }

  @ServerTest(executionMode = {EVENT_LOOP, WORKER})
  public void sendByteArray(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/bytes", ctx -> {
        return ctx.getRequestPath().getBytes(StandardCharsets.UTF_8);
      });
      app.get("/inline", ctx -> {
        return new byte[]{(byte) 'h', (byte) 'e'};
      });
    }).ready(client -> {
      client.get("/bytes", rsp -> {
        assertEquals("/bytes", rsp.body().string());
      });
      client.get("/inline", rsp -> {
        assertEquals("he", rsp.body().string());
      });
    });
  }

  @ServerTest(executionMode = {EVENT_LOOP, WORKER})
  public void sideEffectVsFunctional(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/string", ctx -> {
        ctx.send("side-effects");
        return "ignored";
      });

      app.get("/bytes", ctx -> {
        ctx.send("side-effects");
        return new byte[]{1, 3, 4};
      });

      app.get("/code", ctx -> {
        ctx.send(StatusCode.CREATED);
        return "ignored";
      });

      app.before(ctx -> {
        ctx.send(StatusCode.CREATED);
      });

      app.get("/filter", ctx -> {
        throw new IllegalStateException("Should never gets here");
      });
    }).ready(client -> {
      client.get("/string", rsp -> {
        assertEquals("side-effects", rsp.body().string());
      });

      client.get("/bytes", rsp -> {
        assertEquals("side-effects", rsp.body().string());
      });

      client.get("/code", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(StatusCode.CREATED.value(), rsp.code());
      });

      client.get("/filter", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(StatusCode.CREATED.value(), rsp.code());
      });
    });
  }

  @ServerTest
  public void cors(ServerTestRunner runner) {
    runner.define(app -> {
      app.decorator(new CorsHandler());

      app.get("/greeting", ctx -> "Hello " + ctx.query("name").value("World") + "!");
    }).ready(client -> {
      client
          .header("Origin", "http://foo.com")
          .get("/greeting", rsp -> {
            assertEquals("Hello World!", rsp.body().string());
            assertEquals("http://foo.com", rsp.header("Access-Control-Allow-Origin"));
            assertEquals("true", rsp.header("Access-Control-Allow-Credentials"));
          });

      client.get("/greeting", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(null, rsp.header("Access-Control-Allow-Origin"));
        assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
      });

      /** null chrome local file: */
      client.header("Origin", "null").get("/greeting", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals("*", rsp.header("Access-Control-Allow-Origin"));
        assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
      });

      // preflight
      client
          .header("Origin", "http://foo.com")
          .header("Access-Control-Request-Method", "GET")
          .options("/greeting", rsp -> {
            assertEquals("", rsp.body().string());
            assertEquals(200, rsp.code());
            assertEquals("http://foo.com", rsp.header("Access-Control-Allow-Origin"));
            assertEquals("true", rsp.header("Access-Control-Allow-Credentials"));
            assertEquals("GET,POST", rsp.header("Access-Control-Allow-Methods"));
            assertEquals("X-Requested-With,Content-Type,Accept,Origin",
                rsp.header("Access-Control-Allow-Headers"));
            assertEquals("1800", rsp.header("Access-Control-Max-Age"));
          });

      // preflight without access-control-request-method => normal OPTIONS call
      client
          .header("Origin", "http://foo.com")
          .options("/greeting", rsp -> {
            assertEquals("", rsp.body().string());
            assertEquals(StatusCode.OK_CODE, rsp.code());
            assertEquals("GET,OPTIONS", rsp.header("Allow"));
            assertEquals(null, rsp.header("Access-Control-Allow-Origin"));
            assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
            assertEquals(null, rsp.header("Access-Control-Allow-Methods"));
            assertEquals(null, rsp.header("Access-Control-Allow-Headers"));
            assertEquals(null, rsp.header("Access-Control-Max-Age"));
          });

      client
          .header("Origin", "http://foo.com")
          .header("Access-Control-Request-Method", "PUT")
          .options("/greeting", rsp -> {
            assertEquals("", rsp.body().string());
            assertEquals(StatusCode.FORBIDDEN_CODE, rsp.code());
            assertEquals(null, rsp.header("Access-Control-Allow-Origin"));
            assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
            assertEquals(null, rsp.header("Access-Control-Allow-Methods"));
            assertEquals(null, rsp.header("Access-Control-Allow-Headers"));
            assertEquals(null, rsp.header("Access-Control-Max-Age"));
          });

      client
          .header("Origin", "http://foo.com")
          .header("Access-Control-Request-Method", "GET")
          .header("Access-Control-Request-Headers", "Custom-Header")
          .options("/greeting", rsp -> {
            assertEquals("", rsp.body().string());
            assertEquals(StatusCode.FORBIDDEN_CODE, rsp.code());
            assertEquals(null, rsp.header("Access-Control-Allow-Origin"));
            assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
            assertEquals(null, rsp.header("Access-Control-Allow-Methods"));
            assertEquals(null, rsp.header("Access-Control-Allow-Headers"));
            assertEquals(null, rsp.header("Access-Control-Max-Age"));
          });
    });
  }

  @ServerTest
  public void options(ServerTestRunner runner) {
    runner.define(app -> {

      app.get("/foo", Context::pathString);

      app.post("/foo", Context::pathString);

      app.get("/foo/{id}", Context::pathString);

      app.patch("/foo/{id}", Context::pathString);
    }).ready(client -> {
      Function<String, Set<String>> toSet = value -> Stream.of(value.split("\\s*,\\s*")).collect(
          Collectors.toSet());
      client.options("/foo", rsp -> {
        assertEquals(new HashSet<>(Arrays.asList("GET", "POST")),
            toSet.apply(rsp.header("Allow")));
        assertEquals(StatusCode.OK.value(), rsp.code());
      });

      client.options("/foo/1", rsp -> {
        assertEquals(new HashSet<>(Arrays.asList("GET", "PATCH")),
            toSet.apply(rsp.header("Allow")));
        assertEquals(StatusCode.OK.value(), rsp.code());
      });
    });
  }

  @ServerTest
  public void trace(ServerTestRunner runner) {
    runner.define(app -> {
      app.decorator(new TraceHandler());

      app.get("/foo", Context::pathString);
    }).ready(client -> {
      client.trace("/foo", rsp -> {
        assertEquals(StatusCode.OK.value(), rsp.code());
        assertEquals("message/http", rsp.header("Content-Type"));
        assertTrue(rsp.body().string().startsWith("TRACE /foo HTTP/1.1"));
      });

      client.get("/foo", rsp -> {
        assertEquals(StatusCode.OK.value(), rsp.code());
        assertEquals("/foo", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void head(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());
      app.decorator(new HeadHandler());

      app.get("/fn", ctx -> "string");

      app.get("/side-effects", ctx -> ctx.send("side-effects"));

      app.get("/json", ctx -> new HashMap<>());

      app.get("/render", ctx -> ctx.render(new HashMap<>()));

      app.assets("/?*",
          new AssetHandler("fallback.html", AssetSource.create(app.getClassLoader(), "/www")));
    }).ready(client -> {
      client.head("/fn", rsp -> {
        assertEquals("6", rsp.header("Content-Length"));
        assertEquals("", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.head("/json", rsp -> {
        assertEquals("2", rsp.header("Content-Length"));
        assertEquals("", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.head("/side-effects", rsp -> {
        assertEquals(Integer.toString("side-effects".length()), rsp.header("Content-Length"));
        assertEquals("", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.head("/render", rsp -> {
        assertEquals("2", rsp.header("Content-Length"));
        assertEquals("", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      String cl = String
          .valueOf(getClass().getResource("/www/foo.js").openConnection().getContentLength());
      client.head("/foo.js", rsp -> {
        assertEquals(cl, rsp.header("Content-Length"));
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertNotNull(rsp.header("ETag"));
        assertNotNull(rsp.header("Last-Modified"));
        assertEquals("", rsp.body().string());
        assertEquals(200, rsp.code());
      });
    });
  }

  @ServerTest
  public void beanConverter(ServerTestRunner runner) {
    runner.define(app -> {
      app.converter(new MyValueBeanConverter());

      app.get("/", ctx -> ctx.query(MyValue.class));

      app.get("/error", ctx -> ctx.query("string").to(MyValue.class));

      app.post("/", ctx -> ctx.form(MyValue.class));
    }).ready(client -> {
      client.header("Accept", "text/plain");
      client.get("/error?string=value", rsp -> {
        assertEquals("GET /error 400 Bad Request\n"
            + "Cannot convert value: 'string', to: 'io.jooby.MyValue'", rsp.body().string());
      });

      client.get("/?string=value", rsp -> {
        assertEquals("value", rsp.body().string());
      });

      client.post("/", new FormBody.Builder()
          .add("string", "form")
          .build(), rsp -> {
        assertEquals("form", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void byteArrayResponse(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/bytearray", ctx -> {
        return ctx.send(partition(_19kb.getBytes(StandardCharsets.UTF_8), 1536));
      });
    }).ready(client -> {
      client.get("/bytearray", rsp -> {
        assertEquals(_19kb, rsp.body().string());
      });
    });
  }

  @ServerTest
  public void requestUrl(ServerTestRunner runner) {
    runner.define(app -> {
      app.decorator(new ProxyPeerAddressHandler());

      app.get("/{path}", ctx -> ctx.getRequestURL());
    }).ready(client -> {
      client.get("/somepath", rsp -> {
        assertEquals("http://localhost:" + client.getPort() + "/somepath", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost");
      client.get("/somepath", rsp -> {
        assertEquals("http://myhost/somepath", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost");
      client.header("X-Forwarded-Proto", "https");
      client.get("/somepath?useProxy=true", rsp -> {
        assertEquals("https://myhost/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost:80");
      client.get("/somepath?useProxy=true", rsp -> {
        assertEquals("http://myhost/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost:90");
      client.get("/somepath?useProxy=true", rsp -> {
        assertEquals("http://myhost:90/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "first,second");
      client.get("/somepath?useProxy=true", rsp -> {
        assertEquals("http://first/somepath?useProxy=true", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void requestUrlWithContextPath(ServerTestRunner runner) {
    runner.define(app -> {
      app.decorator(new ProxyPeerAddressHandler());
      app.setContextPath("/x");
      app.get("/{path}", ctx -> ctx.getRequestURL(ctx.query("useProxy").booleanValue(false)));
    }).ready(client -> {
      client.get("/x/somepath", rsp -> {
        assertEquals("http://localhost:" + client.getPort() + "/x/somepath", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost");
      client.get("/x/somepath", rsp -> {
        assertEquals("http://myhost/x/somepath", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost");
      client.header("X-Forwarded-Proto", "https");
      client.get("/x/somepath?useProxy=true", rsp -> {
        assertEquals("https://myhost/x/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost:80");
      client.get("/x/somepath?useProxy=true", rsp -> {
        assertEquals("http://myhost/x/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "myhost:90");
      client.get("/x/somepath?useProxy=true", rsp -> {
        assertEquals("http://myhost:90/x/somepath?useProxy=true", rsp.body().string());
      });
      client.header("X-Forwarded-Host", "first,second");
      client.get("/x/somepath?useProxy=true", rsp -> {
        assertEquals("http://first/x/somepath?useProxy=true", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void shouldAccessToWebVariables(ServerTestRunner runner) {
    runner.define(app -> {
      app.decorator(new WebVariables());

      app.get("/webvars", ctx ->
          Arrays.asList(ctx.attribute("contextPath"), ctx.attribute("path"), ctx.attribute("user"))
      );
    }).ready(client -> {
      client.get("/webvars", rsp -> {
        assertEquals("[, /webvars, null]", rsp.body().string().trim());
      });
    });

    runner.define(app -> {
      app.setContextPath("/app");

      app.decorator(new WebVariables());

      app.get("/webvars", ctx ->
          Arrays.asList(ctx.attribute("contextPath"), ctx.attribute("path"), ctx.attribute("user"))
      );
    }).ready(client -> {
      client.get("/app/webvars", rsp -> {
        assertEquals("[/app, /app/webvars, null]", rsp.body().string().trim());
      });
    });

    runner.define(app -> {
      app.decorator(new WebVariables("scope"));

      app.get("/webvars", ctx ->
          Arrays.asList(ctx.attribute("scope.contextPath"), ctx.attribute("scope.path"),
              ctx.attribute("scope.user"))
      );
    }).ready(client -> {
      client.get("/webvars", rsp -> {
        assertEquals("[, /webvars, null]", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void errorOnParsingFormUrlEncoded(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());

      app.post("/", ctx -> {
        return ctx.body(Map.class);
      });

      app.error(StatusCode.UNSUPPORTED_MEDIA_TYPE, (ctx, cause, status) -> {
        ctx.setResponseCode(status).send(cause.getMessage());
      });
    }).ready(client -> {
      client.post("/", new FormBody.Builder()
          .add("foo", "bar")
          .build(), rsp -> {
        assertEquals("application/x-www-form-urlencoded", rsp.body().string());
        assertEquals(StatusCode.UNSUPPORTED_MEDIA_TYPE_CODE, rsp.code());
      });
    });
  }

  @ServerTest
  public void accessLog(ServerTestRunner runner) {
    runner.define(app -> {

      app.decorator(new AccessLogHandler().extended());

      app.get("/fn", Context::getRequestPath);

      app.get("/stream", ctx -> ctx.responseStream(text, out -> {
        IOUtils.copyLarge(new ByteArrayInputStream(_19kb.getBytes(StandardCharsets.UTF_8)), out);
      }));

      app.get("/status", ctx -> ctx.setResponseCode(StatusCode.CREATED).send(ctx.getRequestPath()));

      app.get("/send-status", ctx -> ctx.send(StatusCode.CREATED));

    }).ready(client -> {
      client.get("/fn", rsp -> {
        assertEquals(3L, rsp.body().contentLength());
        assertEquals("/fn", rsp.body().string());
      });

      client.get("/stream", rsp -> {
        assertEquals("chunked", rsp.header("Transfer-Encoding").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/status", rsp -> {
        assertEquals("/status", rsp.body().string());
        assertEquals(7L, rsp.body().contentLength());
      });

      client.get("/send-status", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(0, rsp.body().contentLength());
      });
    });
  }

  @ServerTest
  public void csrf(ServerTestRunner runner) {
    String token = UUID.randomUUID().toString();
    runner.define(app -> {
      app.before(new CsrfHandler().setTokenGenerator(ctx -> token));

      app.post("/form", Context::getRequestPath);

    }).ready(client -> {
      client.post("/form", new FormBody.Builder()
          .add("foo", "bar")
          .build(), rsp -> {
        assertEquals(StatusCode.FORBIDDEN.value(), rsp.code());
      });

      client.post("/form", new FormBody.Builder()
          .add("foo", "bar")
          .add("csrf", token)
          .build(), rsp -> {
        assertEquals(200, rsp.code());
      });

      client.post("/form?csrf=" + token, new FormBody.Builder()
          .add("foo", "bar")
          .build(), rsp -> {
        assertEquals(200, rsp.code());
      });

      client.header("csrf", token);
      client.post("/form", new FormBody.Builder()
          .add("foo", "bar")
          .build(), rsp -> {
        assertEquals(200, rsp.code());
      });

      client.header("Cookie", "csrf=" + token);
      client.post("/form", new FormBody.Builder()
          .add("foo", "bar")
          .build(), rsp -> {
        assertEquals(200, rsp.code());
      });
    });
  }

  @ServerTest
  public void forward(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/api/v1/{object}", ctx -> ctx.forward("/api/v1.0/" + ctx.path("object").value()));

      app.get("/api/v1.0/{name}", ctx -> ctx.path("name").value());
    }).ready(client -> {
      client.get("/api/v1/pets", rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("pets", rsp.body().string());
      });
    });
  }

  @ServerTest
  public void custom404WithTemplateEngine(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new HandlebarsModule());

      app.error((ctx, cause, code) -> {
        ctx.render(new ModelAndView("error.hbs").put("statusCode", code));
      });
    }).ready(client -> {
      client.get("/missing", rsp -> {
        assertEquals("Not Found (404)", rsp.body().string().trim());
        assertEquals(404, rsp.code());
      });
    });
  }

  private byte[][] partition(byte[] bytes, int size) {
    List<byte[]> result = new ArrayList<>();
    int offset = 0;
    while (offset < bytes.length) {
      int len = Math.min(size, bytes.length - offset);
      byte[] b = new byte[len];
      System.arraycopy(bytes, offset, b, 0, len);
      result.add(b);
      offset += size;
    }
    return result.toArray(new byte[0][0]);
  }

  private static String readText(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }

  public long transfer(Reader reader, Writer out) throws IOException {
    long transferred = 0;
    char[] buffer = new char[ServerOptions._16KB];
    int nRead;
    while ((nRead = reader.read(buffer, 0, ServerOptions._16KB)) >= 0) {
      out.write(buffer, 0, nRead);
      transferred += nRead;
    }
    return transferred;
  }

  private Map<String, Object> mapOf(String... values) {
    Map<String, Object> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return hash;
  }
}
