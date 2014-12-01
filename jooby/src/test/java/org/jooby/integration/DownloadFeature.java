package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class DownloadFeature extends ServerFeature {

  {
    get("/download-reader", (req, rsp) -> rsp.download("/name.js",
        new FileReader(new File("src/test/resources/assets/js/file.js"))));

    get("/customtype", (req, rsp) -> {
      rsp.type(req.param("type").stringValue()).download("/name.js",
          new FileReader(new File("src/test/resources/assets/js/file.js")));
    });

    get("/location", (req, rsp) -> rsp.download("name", req.param("file").stringValue()));

    get("/fs", (req, rsp) -> rsp.download(new File(req.param("file").stringValue())));

    get("/favicon.ico", (req, rsp) -> rsp.download("favicon.ico",
        new FileInputStream(new File("src/main/resources/favicon.ico"))));
  }

  @Test
  public void downloadReader() throws Exception {
    HttpResponse rsp = Request.Get(uri("/download-reader").build()).execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals("(function () {})();\n", EntityUtils.toString(entity));

    assertEquals("attachment; filename=name.js",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("application/javascript; charset=UTF-8",
        rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void downloadBinWithLocation() throws Exception {
    HttpResponse rsp = Request.Get(uri("location?file=favicon.ico").build())
        .execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals(2238, EntityUtils.toByteArray(entity).length);

    assertEquals("attachment; filename=name",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("image/x-icon", rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void downloadBinWithFsLocation() throws Exception {
    HttpResponse rsp = Request.Get(uri("fs?file=src/main/resources/favicon.ico").build())
        .execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals(2238, EntityUtils.toByteArray(entity).length);

    assertEquals("attachment; filename=favicon.ico",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("image/x-icon", rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void downloadTextWithLocation() throws Exception {
    HttpResponse rsp = Request.Get(uri("location?file=assets/js/file.js").build())
        .execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals("(function () {})();\n", EntityUtils.toString(entity));

    assertEquals("attachment; filename=name",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("application/javascript; charset=UTF-8",
        rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void downloadNotFound() throws Exception {
    HttpResponse rsp = Request.Get(uri("location?file=assets/js/missing.js").build())
        .execute().returnResponse();
    assertEquals(500, rsp.getStatusLine().getStatusCode());
  }

  @Test
  public void downloadTextWithFsLocation() throws Exception {
    HttpResponse rsp = Request.Get(uri("fs?file=src/test/resources/assets/js/file.js").build())
        .execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals("(function () {})();\n", EntityUtils.toString(entity));

    assertEquals("attachment; filename=file.js",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("application/javascript; charset=UTF-8",
        rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void customType() throws Exception {
    HttpResponse rsp = Request.Get(uri("/customtype?type=json").build()).execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals("(function () {})();\n", EntityUtils.toString(entity));

    assertEquals("attachment; filename=name.js",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("application/json; charset=UTF-8",
        rsp.getFirstHeader("Content-Type").getValue());
  }

  @Test
  public void downladInputStream() throws Exception {
    HttpResponse rsp = Request.Get(uri("/favicon.ico").build()).execute().returnResponse();
    HttpEntity entity = rsp.getEntity();

    assertEquals(2238, EntityUtils.toByteArray(entity).length);

    assertEquals("attachment; filename=favicon.ico",
        rsp.getFirstHeader("Content-Disposition").getValue());

    assertEquals("chunked", rsp.getFirstHeader("Transfer-Encoding").getValue());

    assertEquals("image/x-icon", rsp.getFirstHeader("Content-Type").getValue());
  }

}
