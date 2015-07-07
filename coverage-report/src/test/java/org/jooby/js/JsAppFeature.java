package org.jooby.js;

import java.io.File;
import java.nio.file.Paths;

import org.jooby.Jooby;
import org.jooby.internal.js.JsJooby;
import org.junit.Test;

public class JsAppFeature {

  @Test
  public void apprsc() throws Exception {
    run("/org/jooby/js/app.js");
  }

  @Test
  public void appfile() throws Exception {
    run(Paths.get("src", "test", "resources", "org", "jooby", "js", "appfile.js").toFile());
  }

  private void run(final String filename) throws Exception {
    new JsJooby().run(getClass().getResourceAsStream(filename));
  }

  private void run(final File filename) throws Exception {
    Jooby.main(new String[]{filename.getAbsolutePath() });
  }

}
