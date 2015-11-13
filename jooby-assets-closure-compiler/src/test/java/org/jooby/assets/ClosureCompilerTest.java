package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.Arrays;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class ClosureCompilerTest {

  @Test
  public void ws() throws Exception {
    assertEquals("function xs(longvar){return longvar*2};",
        new ClosureCompiler().set("level", "ws")
            .process("/x.js", "function xs(longvar) {\nreturn longvar * 2;\n}",
                ConfigFactory.empty()));
  }

  @Test
  public void simple() throws Exception {
    assertEquals("function xs(a){return 2*a};",
        new ClosureCompiler().process("/x.js", "function xs(longvar) {\nreturn longvar * 2;\n}",
            ConfigFactory.empty()));
  }

  @Test
  public void advanced() throws Exception {
    assertEquals("",
        new ClosureCompiler().set("level", "advanced")
            .process("/x.js", "function xs(longvar) {\nreturn longvar * 2;\n}",
                ConfigFactory.empty()));

    assertEquals("window.a=function(b){return 2*b};",
        new ClosureCompiler().set("level", "advanced")
            .process("/x.js", "function xs(longvar) {\nreturn longvar * 2;\n}; window.keep=xs;",
                ConfigFactory.empty()));

    assertEquals("window.a=function(){return 2*keep};",
        new ClosureCompiler()
            .set("level", "advanced")
            .set("externs", Arrays.asList("myextern.js"))
            .process("/x.js", "function xs() {\nreturn keep * 2;\n}; window.xs = xs;",
                ConfigFactory.empty()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void levelNotFound() throws Exception {
    new ClosureCompiler()
        .set("level", "x")
        .process("/x.js", "function xs() {\nreturn keep * 2;\n}; window.xs = xs;",
            ConfigFactory.empty());
  }

  @Test(expected = FileNotFoundException.class)
  public void externNotFound() throws Exception {
    new ClosureCompiler()
        .set("externs", Arrays.asList("missing.js"))
        .process("/x.js", "function xs() {\nreturn keep * 2;\n}; window.xs = xs;",
            ConfigFactory.empty());
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals("function(a){return a*2;}",
        new ClosureCompiler().process("/x.js", "function (longvar) {\nreturn longvar  2;\n}",
            ConfigFactory.empty()));
  }

}
