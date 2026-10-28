/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i4031;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Assertions;

import io.jooby.exception.MissingValueException;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

class Issue4031 {

  @ServerTest
  public void formDataAndQueryParameters(ServerTestRunner runner) {
    runner
        .define(
            app ->
                app.post(
                    "/",
                    ctx -> {
                      try {
                        Assertions.assertAll(
                            () ->
                                Assertions.assertEquals(
                                    1, ctx.form().size(), "form contains one entry"),
                            () -> Assertions.assertTrue(ctx.form().isPresent()),
                            () -> Assertions.assertFalse(ctx.form().isMissing()),
                            () -> Assertions.assertEquals("bar", ctx.form().get("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form().toMap().get("foo")),
                            () -> Assertions.assertEquals("bar", ctx.formMap().get("foo")),
                            () ->
                                Assertions.assertEquals(
                                    "?parameter1=value1", ctx.query().queryString()),
                            () -> Assertions.assertEquals("parameter1=value1", ctx.query().value()),
                            () ->
                                Assertions.assertEquals("value1", ctx.query("parameter1").value()),
                            () ->
                                Assertions.assertEquals("value1", ctx.queryMap().get("parameter1")),
                            () -> Assertions.assertEquals(1, ctx.queryMap().size()));
                        return "OK";
                      } catch (Exception ex) {
                        ctx.setResponseCode(500);
                        return ex.getMessage();
                      }
                    }))
        .ready(
            http -> {
              http.post(
                  "/",
                  new FormBody.Builder().add("foo", "bar").build(),
                  Map.of("parameter1", "value1"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              // form with bad media type
              http.post(
                  "/",
                  customForm(
                      Map.of("foo", "bar"), "application/x-www-form-urlencoded; charset=utf-8"),
                  Map.of("parameter1", "value1"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              http.post(
                  "/",
                  customForm(
                      Map.of("foo", "bar"), "application/x-www-form-urlencoded;charset=iso-8859-1"),
                  Map.of("parameter1", "value1"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              // multipart
              http.post(
                  "/",
                  new MultipartBody.Builder()
                      .addFormDataPart("foo", "bar")
                      .setType(MultipartBody.FORM)
                      .build(),
                  Map.of("parameter1", "value1"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });
            });
  }

  private static RequestBody customForm(Map<String, Object> form, String mediaType) {
    var payload =
        form.entrySet().stream()
            .map(
                entry ->
                    entry.getKey()
                        + "="
                        + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8))
            .reduce((a, b) -> a + "&" + b)
            .orElse("");

    return RequestBody.create(payload, MediaType.parse(mediaType));
  }

  @ServerTest
  public void readFormDataAfterBody(ServerTestRunner runner) {
    runner
        .define(
            app ->
                app.post(
                    "/",
                    ctx -> {
                      try {
                        Assertions.assertAll(
                            () ->
                                Assertions.assertEquals(
                                    1, ctx.form().size(), "form contains one entry"),
                            () -> Assertions.assertTrue(ctx.form().isPresent()),
                            () -> Assertions.assertFalse(ctx.form().isMissing()),
                            () -> Assertions.assertEquals("bar", ctx.form().get("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form().toMap().get("foo")),
                            () -> Assertions.assertEquals("bar", ctx.formMap().get("foo")),
                            // get body().value() after calling form methods
                            () ->
                                Assertions.assertThrows(
                                    MissingValueException.class, () -> ctx.body().value()));
                        return "OK";
                      } catch (Exception ex) {
                        ctx.setResponseCode(500);
                        return ex.getMessage();
                      }
                    }))
        .ready(
            http -> {
              http.post(
                  "/",
                  new FormBody.Builder().add("foo", "bar").build(),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              http.post(
                  "/",
                  customForm(
                      Map.of("foo", "bar"), "application/x-www-form-urlencoded; charset=utf-8"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              http.post(
                  "/",
                  new MultipartBody.Builder()
                      .addFormDataPart("foo", "bar")
                      .setType(MultipartBody.FORM)
                      .build(),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });
            });
  }

  @ServerTest
  public void readFormDataBeforeBody(ServerTestRunner runner) {
    runner
        .define(
            app ->
                app.post(
                    "/",
                    ctx -> {
                      try {
                        Assertions.assertAll(
                            // get body().value() before calling form methods
                            () ->
                                Assertions.assertThrows(
                                    MissingValueException.class, () -> ctx.body().value()),
                            () ->
                                Assertions.assertEquals(
                                    1, ctx.form().size(), "form contains one entry"),
                            () -> Assertions.assertTrue(ctx.form().isPresent()),
                            () -> Assertions.assertFalse(ctx.form().isMissing()),
                            () -> Assertions.assertEquals("bar", ctx.form().get("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form("foo").value()),
                            () -> Assertions.assertEquals("bar", ctx.form().toMap().get("foo")),
                            () -> Assertions.assertEquals("bar", ctx.formMap().get("foo")));
                        return "OK";
                      } catch (Exception ex) {
                        ctx.setResponseCode(500);
                        return ex.getMessage();
                      }
                    }))
        .ready(
            http -> {
              //              http.post(
              //                  "/",
              //                  new FormBody.Builder().add("foo", "bar").build(),
              //                  rsp -> {
              //                    Assertions.assertEquals("OK", rsp.body().string());
              //                    Assertions.assertEquals(200, rsp.code());
              //                  });

              http.post(
                  "/",
                  customForm(
                      Map.of("foo", "bar"), "application/x-www-form-urlencoded; charset=utf-8"),
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                    Assertions.assertEquals(200, rsp.code());
                  });

              //              http.post(
              //                  "/",
              //                  new MultipartBody.Builder()
              //                      .setType(MultipartBody.FORM)
              //                      .addFormDataPart("foo", "bar")
              //                      .build(),
              //                  rsp -> {
              //                    Assertions.assertEquals("OK", rsp.body().string());
              //                    Assertions.assertEquals(200, rsp.code());
              //                  });
            });
  }
}
