package org.jooby;

import java.io.File;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class DownloadFeature extends ServerFeature {

  {
    get("/content-disposition", (req, rsp) -> rsp
        .header("content-disposition", "attachment; filename=myfile.txt;")
        .download("name.js", new File("src/test/resources/"
            + DownloadFeature.class.getName().replace('.', '/') + ".js")));

    get("/download-reader", (req, rsp) -> rsp.download("name.js",
        new File("src/test/resources/"
            + DownloadFeature.class.getName().replace('.', '/') + ".js")));

    get("/customtype", (req, rsp) -> {
      rsp.type(req.param("type").value()).download("/name.json",
          new File("src/test/resources/"
              + DownloadFeature.class.getName().replace('.', '/') + ".json"));
    });

    get("/location", (req, rsp) -> rsp.download("name", req.param("file").value()));

    get("/file",
        (req, rsp) -> rsp.download(DownloadFeature.class.getName().replace('.', '/') + ".json"));

    get("/fs", (req, rsp) -> rsp.download(new File(req.param("file").value())));

    get("/favicon.ico", (req, rsp) -> rsp.download("favicon.ico",
        new File("src/test/resources/"
            + DownloadFeature.class.getName().replace('.', '/') + ".ico")));
  }

  @Test
  public void downloadReader() throws Exception {
    request()
        .get("/download-reader")
        .expect("(function () {})();\n")
        .header("Content-Disposition", "attachment; filename=\"name.js\"; filename*=utf-8''name.js")
        .header("Content-Length", "20")
        .header("Content-Type", "application/javascript;charset=UTF-8");
  }

  @Test
  public void contentDisposition() throws Exception {
    request()
        .get("/content-disposition")
        .expect(200)
        .header("Content-Disposition", "attachment; filename=myfile.txt;")
        .header("Content-Length", "20")
        .header("Content-Type", "application/javascript;charset=UTF-8");
  }

  @Test
  public void downloadBinWithLocation() throws Exception {
    request()
        .get("/location?file=" + getClass().getName().replace('.', '/') + ".ico")
        .expect(200)
        .header("Content-Disposition", "attachment; filename=\"name\"; filename*=utf-8''name")
        .header("Content-Length", "2238")
        .header("Content-Type", "image/x-icon");
  }

  @Test
  public void downloadBinWithFsLocation() throws Exception {
    request()
        .get("/fs?file=src/test/resources/" + getClass().getName().replace('.', '/') + ".ico")
        .expect(200)
        .header("Content-Disposition",
            "attachment; filename=\"DownloadFeature.ico\"; filename*=utf-8''DownloadFeature.ico")
        .header("Content-Length", "2238")
        .header("Content-Type", "image/x-icon");
  }

  @Test
  public void downloadTextWithLocation() throws Exception {
    request()
        .get("/location?file=" + getClass().getName().replace('.', '/') + ".js")
        .expect(200)
        .expect("(function () {})();\n")
        .header("Content-Disposition", "attachment; filename=\"name\"; filename*=utf-8''name")
        .header("Content-Length", "20")
        .header("Content-Type", "application/javascript;charset=UTF-8");
  }

  @Test
  public void downloadNotFound() throws Exception {
    request()
        .get("/location?file=" + getClass().getName().replace('.', '/') + "Missing.js")
        .expect(404);
  }

  @Test
  public void downloadTextWithFsLocation() throws Exception {
    request()
        .get("/fs?file=src/test/resources/" + getClass().getName().replace('.', '/') + ".js")
        .expect("(function () {})();\n")
        .header("Content-Disposition",
            "attachment; filename=\"DownloadFeature.js\"; filename*=utf-8''DownloadFeature.js")
        .header("Content-Length", "20")
        .header("Content-Type", "application/javascript;charset=UTF-8");
  }

  @Test
  public void customType() throws Exception {
    request()
        .get("/customtype?type=json")
        .expect("{}\n")
        .header("Content-Disposition",
            "attachment; filename=\"name.json\"; filename*=utf-8''name.json")
        .header("Content-Length", "3")
        .header("Content-Type", "application/json;charset=UTF-8");
  }

  @Test
  public void downladInputStream() throws Exception {
    request()
        .get("/favicon.ico")
        .expect(200)
        .header("Content-Disposition",
            "attachment; filename=\"favicon.ico\"; filename*=utf-8''favicon.ico")
        .header("Content-Length", "2238")
        .header("Content-Type", "image/x-icon");
  }

  @Test
  public void download() throws Exception {
    request()
        .get("/file")
        .expect(200)
        .header("Content-Disposition",
            "attachment; filename=\"downloadfeature.json\"; filename*=utf-8''downloadfeature.json")
        .header("Content-Length", "3")
        .header("Content-Type", "application/json;charset=utf-8");
  }

}
