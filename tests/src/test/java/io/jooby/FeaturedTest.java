package io.jooby;

import io.jooby.json.Jackson;
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
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.jooby.MediaType.text;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reactor.core.scheduler.Schedulers.elastic;

public class FeaturedTest {

  private static String _19kb = readText(
      userdir("src", "test", "resources", "files", "19kb.txt"));

  private static MediaType json = MediaType.parse("application/json");

  private static MediaType textplain = MediaType.parse("text/plain");

  public static class Datafile {

    public final String name;

    public final Path filename;

    public Datafile(String name, Path filename) {
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

  @Test
  public void sayHi() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> "Hello World!");
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });
    });

  }

  @Test
  public void sayHiFromIO() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> "Hello World!");
      app.group(app.worker(), () -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
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
            + "<hr><h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(579, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void sayHiFromWorker() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> "Hello World!");
      app.group(app.worker(), () -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).mode(ExecutionMode.WORKER).ready(client -> {
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
            + "<title>"
            + "Not Found (404)"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr><h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(579, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void rawPath() {
    new JoobyRunner(app -> {

      app.get("/{code}", ctx -> ctx.pathString());

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

    });
  }

  @Test
  public void gzip() throws IOException {
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
    new JoobyRunner(app -> {

      app.get("/gzip", ctx -> text);

    }).configureServer(server -> {

      server.gzip(true);

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

      // No Accept-Encoding
      client.get("/gzip").prepare(req -> {
        // NOTE: required bc okhttp always set Accept-Encoding to gzip
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

  @Test
  public void pathVariable() {
    new JoobyRunner(app -> {
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

  @Test
  public void paramKeys() {
    new JoobyRunner(app -> {
      app.get("/articles/{id}", ctx -> ctx.route().pathVariables());

      app.get("/articles/*", ctx -> ctx.route().pathVariables());

      app.get("/file/*path", ctx -> ctx.route().pathVariables());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.route().pathVariables());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.route().pathVariables());

      app.get("/file/{file}.json", ctx -> ctx.route().pathVariables());

      app.get("/file/{file}.{ext}", ctx -> ctx.route().pathVariables());

      app.get("/profile/{pid}", ctx -> ctx.route().pathVariables());

      app.get("/profile/me", ctx -> ctx.route().pathVariables());

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

  @Test
  public void pathEncoding() {
    new JoobyRunner(app -> {
      app.get("/{value}", ctx -> ctx.pathString() + "@" + ctx.path("value").value());
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
    });
  }

  @Test
  public void form() {
    new JoobyRunner(app -> {
      app.post("/", ctx -> ctx.form());
    }).mode(ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER).ready(client -> {
      client.post("/", new FormBody.Builder()
          .add("q", "a b")
          .add("user.name", "user")
          .build(), rsp -> {
        assertEquals("{q=a b, user={name=user}}", rsp.body().string());
      });
    });
  }

  @Test
  public void multipartFromWorker() {
    new JoobyRunner(app -> {
      app.post("/f", ctx -> {
        FileUpload f = ctx.file("f");
        return f.filename() + "(type=" + f.contentType() + ";exists=" + Files
            .exists(f.path())
            + ")";
      });

      app.post("/files", ctx -> {
        List<FileUpload> files = ctx.files("f");
        return files.stream().map(f -> f.filename() + "=" + f.filesize())
            .collect(Collectors.toList());
      });

      app.post("/datafile", ctx -> {
        Datafile file = ctx.multipart(Datafile.class);
        return file.name + "=" + Files.exists(file.filename);
      });

      app.post("/datafiles", ctx -> {
        Datafiles file = ctx.multipart(Datafiles.class);
        return file.name + "=" + file.filename;
      });

      app.post("/multipart", ctx -> {
        return ctx.multipart().toMap();
      });
    }).ready(client -> {
      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "fileupload.js",
              create(MediaType.parse("application/javascript"),
                  userdir("src", "test", "resources", "files", "fileupload.js").toFile()))
          .build(), rsp -> {
        assertEquals("fileupload.js(type=application/javascript;exists=true)",
            rsp.body().string());
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

      client.post("/multipart", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("f", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("{user.name=[user], f=[f1.txt, f2.txt]}", rsp.body().string());
      });

      client.post("/datafile", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "fileupload.js",
              create(MediaType.parse("application/javascript"),
                  userdir("src", "test", "resources", "files", "fileupload.js").toFile()))
          .build(), rsp -> {
        assertEquals("foo_report=true", rsp.body().string());
      });

      client.post("/datafiles", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("filename", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("foo_report=[f1.txt, f2.txt]", rsp.body().string());
      });
    });
  }

  @Test
  public void multipartFromIO() {
    new JoobyRunner(app -> {
      app.post("/f", ctx -> ctx.multipart());

      app.group(app.worker(), () ->
          app.post("/w", ctx -> ctx.multipart()));

      app.error(IllegalStateException.class, (ctx, x, statusCode) -> {
        ctx.sendText(x.getMessage());
      });
    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("foo", "bar")
          .build(), rsp -> {
        assertEquals(
            "Attempted to do blocking EVENT_LOOP from the EVENT_LOOP thread. This is prohibited as it may result in deadlocks",
            rsp.body().string());
      });

      client.post("/w", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("foo", "bar")
          .build(), rsp -> {
        assertEquals("{foo=bar}", rsp.body().string());
      });
    }, Netty::new, Utow::new);
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

      app.group(app.worker(), () -> {
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

    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("before1:false;before2:false;result:false;after2:false;after1:false;",
            rsp.body().string());
      });
    }, Netty::new, Utow::new/* No Jetty bc always use a worker thread */);
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

      app.post("/str", ctx -> ctx.body().value());
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
    });
  }

  @Test
  public void rx2() {
    new JoobyRunner(app -> {
      app.get("/rx/flowable", ctx ->
          Flowable.fromCallable(() -> "Flowable")
              .map(s -> "Hello " + s)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/observable", ctx ->
          Observable.fromCallable(() -> "Observable")
              .map(s -> "Hello " + s)
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
    }).ready(client -> {
      client.get("/rx/flowable", rsp -> {
        assertEquals("Hello Flowable", rsp.body().string());
      });
      client.get("/rx/observable", rsp -> {
        assertEquals("Hello Observable", rsp.body().string());
      });
      client.get("/rx/single", rsp -> {
        assertEquals("Hello Single", rsp.body().string());
      });
      client.get("/rx/maybe", rsp -> {
        assertEquals("Hello Maybe", rsp.body().string());
      });
    });
  }

  @Test
  public void reactor() {
    new JoobyRunner(app -> {
      app.get("/reactor/mono", ctx ->
          Mono.fromCallable(() -> "Mono")
              .map(s -> "Hello " + s)
              .subscribeOn(elastic())
      );

      app.get("/reactor/flux", ctx ->
          Flux.just("Flux")
              .map(s -> "Hello " + s)
              .subscribeOn(elastic())
      );
    }).ready(client -> {
      client.get("/reactor/mono", rsp -> {
        assertEquals("Hello Mono", rsp.body().string());
      });
      client.get("/reactor/flux", rsp -> {
        assertEquals("Hello Flux", rsp.body().string());
      });
    });
  }

  @Test
  public void completableFuture() {
    new JoobyRunner(app -> {

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

  @Test
  public void basePath() {
    new JoobyRunner(app -> {
      app.basePath("/foo");
      app.get("/bar", ctx -> ctx.pathString());

    }).ready(client -> {
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

  @Test
  public void simpleRouterComposition() {
    new JoobyRunner(app -> {

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

  @Test
  public void dynamicRoutingComposition() {
    new JoobyRunner(app -> {

      Jooby v1 = new Jooby();
      v1.get("/api", ctx -> "v1");

      Jooby v2 = new Jooby();
      v2.get("/api", ctx -> "v2");

      app.use(ctx -> ctx.header("version").value().equals("v1"), v1);
      app.use(ctx -> ctx.header("version").value().equals("v2"), v2);

    }).ready(client -> {
      client.header("version", "v2");
      client.get("/api", rsp -> {
        assertEquals("v2", rsp.body().string());
      });

      client.header("version", "v1");
      client.get("/api", rsp -> {
        assertEquals("v1", rsp.body().string());
      });
    });
  }

  @Test
  public void prefixPathOnExistingRouter() {
    new JoobyRunner(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.use("/prefix", bar);

    }).ready(client -> {
      client.get("/prefix/bar", rsp -> {
        assertEquals("/prefix/bar", rsp.body().string());
      });
    });
  }

  @Test
  public void compose() {
    new JoobyRunner(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.group("/api", () -> {
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

  @Test
  public void methodNotAllowed() {
    new JoobyRunner(app -> {
      app.post("/method", Context::pathString);
    }).ready(client -> {
      client.get("/method", rsp -> {
        assertEquals(StatusCode.METHOD_NOT_ALLOWED.value(), rsp.code());
      });
    });
  }

  @Test
  public void silentFavicon() {
    new JoobyRunner(Jooby::new).ready(client -> {
      client.get("/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
      client.get("/foo/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
    });
  }

  @Test
  public void customHttpMethod() {
    new JoobyRunner(app -> {
      app.route("foo", "/bar", Context::method);
    }).ready(client -> {
      client.invoke("foo", "/bar").execute(rsp -> {
        assertEquals("FOO", rsp.body().string());
      });
    });
  }

  @Test
  public void setContentType() {
    new JoobyRunner(app -> {
      app.get("/plain", ctx -> ctx.type("text/plain").sendText("Text"));
    }).ready(client -> {
      client.get("/plain", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("Text", rsp.body().string());
      });
    });
  }

  @Test
  public void setContentLen() {
    String value = "...";
    new JoobyRunner(app -> {
      app.get("/len", ctx -> ctx.type(text).length(value.length()).sendText(value));
    }).ready(client -> {
      client.get("/len", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("...", rsp.body().string());
        assertEquals(3L, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void mixedMode() {
    new JoobyRunner(app -> {
      app.get("/blocking", ctx -> !ctx.isInIoThread());
      app.get("/reactive", ctx ->
          Single.fromCallable(() -> ctx.isInIoThread())
      );
    }).mode(ExecutionMode.DEFAULT).ready(client -> {
      client.get("/blocking", rsp -> {
        assertEquals("true", rsp.body().string());
      });
      client.get("/reactive", rsp -> {
        assertEquals("true", rsp.body().string());
      });
    }, Netty::new, Utow::new);
  }

  @Test
  public void defaultHeaders() {
    LinkedList<String> servers = new LinkedList<>(Arrays.asList("netty", "utow", "jetty"));
    new JoobyRunner(app -> {
      app.decorate(Filters.defaultHeaders());
      app.get("/", Context::pathString);
    }).ready(client -> {
      client.get("/", rsp -> {
        assertNotNull(rsp.header("Date"));
        assertEquals(servers.getFirst(), rsp.header("Server"));
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
        servers.removeFirst();
      });
    });
  }

  @Test
  public void defaultContentType() {
    new JoobyRunner(app -> {
      app.decorate(Filters.contentType(text));
      app.get("/type", Context::pathString);
    }).ready(client -> {
      client.get("/type", rsp -> {
        assertEquals("text/plain;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });
    });

    new JoobyRunner(app -> {
      app.decorate(Filters.contentType("text/plain"));
      app.get("/type-text", Context::pathString);
    }).ready(client -> {
      client.get("/type-text", rsp -> {
        assertEquals("text/plain;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });
    });

    new JoobyRunner(app -> {
      app.decorate(Filters.contentType("text/plain"));
      app.get("/type-override", ctx -> ctx.type("text/html").sendText("OK"));
    }).ready(client -> {
      client.get("/type-override", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
      });
    });
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
