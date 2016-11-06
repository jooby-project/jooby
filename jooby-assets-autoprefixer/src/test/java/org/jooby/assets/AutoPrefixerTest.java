package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jooby.MediaType;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class AutoPrefixerTest {

  @Test
  public void css() throws Exception {
    assertTrue(new AutoPrefixer().matches(MediaType.css));
    assertFalse(new AutoPrefixer().matches(MediaType.js));
  }

  @Test
  public void defaults() throws Exception {
    assertEquals(":-webkit-full-screen a {\n" +
        "    display: -webkit-box;\n" +
        "    display: flex\n" +
        "}\n" +
        ":-moz-full-screen a {\n" +
        "    display: flex\n" +
        "}\n" +
        ":-ms-fullscreen a {\n" +
        "    display: -ms-flexbox;\n" +
        "    display: flex\n" +
        "}\n" +
        ":fullscreen a {\n" +
        "    display: -webkit-box;\n" +
        "    display: -ms-flexbox;\n" +
        "    display: flex\n" +
        "}",
        new AutoPrefixer().process("/styles.css", ":fullscreen a {\n" +
            "    display: flex\n" +
            "}",
            ConfigFactory.empty()));
  }

  @Test
  public void browsers() throws Exception {
    assertEquals(":-webkit-full-screen a {\n" +
        "    display: -webkit-box;\n" +
        "    display: flex\n" +
        "}\n" +
        ":-moz-full-screen a {\n" +
        "    display: flex\n" +
        "}\n" +
        ":-ms-fullscreen a {\n" +
        "    display: flex\n" +
        "}\n" +
        ":fullscreen a {\n" +
        "    display: -webkit-box;\n" +
        "    display: flex\n" +
        "}",
        new AutoPrefixer()
            .set("browsers", Arrays.asList("> 1%", "IE 7"))
            .process("/styles.css", ":fullscreen a {\n" +
                "    display: flex\n" +
                "}",
                ConfigFactory.empty()));
  }
}
