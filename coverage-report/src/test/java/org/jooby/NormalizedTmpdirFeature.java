package org.jooby;

import java.io.File;
import java.nio.file.Paths;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class NormalizedTmpdirFeature extends ServerFeature {

  {
    get("/tmpdir", req ->
        req.require(Config.class).getString("application.tmpdir"));
  }

  @Test
  public void tmpdir() throws Exception {
    String tmpdir = Paths.get(System.getProperty("java.io.tmpdir") + File.separator,
        getClass().getSimpleName()).normalize().toString();
    request()
        .get("/tmpdir")
        .expect(tmpdir);
  }

}
