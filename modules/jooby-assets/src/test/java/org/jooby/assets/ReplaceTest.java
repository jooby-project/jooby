package org.jooby.assets;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ReplaceTest {

  @Test
  public void replaceProcessEnvNODE_ENV() throws Exception {
    assertEquals("if (\"production\" === \"development\") {\n"
            + "  console.log('development only')\n"
            + "}\nconsole.log(headprocess.env.NODE_ENVtail)",
        new Replace()
            .set(conf("env"))
            .process("/index.js", "if (process.env.NODE_ENV === \"development\") {\n"
                    + "  console.log('development only')\n"
                    + "}\nconsole.log(headprocess.env.NODE_ENVtail)",
                ConfigFactory.empty()));
  }

  @Test
  public void replaceString() throws Exception {
    assertEquals("if (\"production\" !== \"production\") {\n"
            + "  console.log('development only')\n"
            + "}\n",
        new Replace()
            .set(conf("quotes"))
            .process("/index.js", "if (\"production\" !== \"development\") {\n"
                    + "  console.log('development only')\n"
                    + "}\n",
                ConfigFactory.empty()));
  }

  @Test
  public void replaceMultiString() throws Exception {
    assertEquals("if (\"production\" !== \"production\") {\n"
            + "  console.log('development only')\n"
            + "}\n",
        new Replace()
            .set(conf("multi"))
            .process("/index.js", "if (\"production\" !== \"development\") {\n"
                    + "  console.log('development only')\n"
                    + "}\n",
                ConfigFactory.empty()));
  }

  private Config conf(String path) {
    return ConfigFactory.parseResources("replace.conf").getConfig(path);
  }

}
