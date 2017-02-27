package org.jooby.js;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.jooby.JoobyJs;
import org.junit.Test;

public class JsAppFeature {

  @Test
  public void apprsc() throws Exception {
    run("/org/jooby/js/app.js");
  }

  @Test
  public void appfile() throws Throwable {
    run(Paths.get("src", "test", "resources", "org", "jooby", "js", "appfile.js").toFile());
  }

  private void run(final String filename) throws Exception {
    new JoobyJs().run(new InputStreamReader(getClass().getResourceAsStream(filename))).get();
  }

  private void run(final File filename) throws Throwable {
    JoobyJs.main(new String[]{filename.getAbsolutePath() });
  }

}
