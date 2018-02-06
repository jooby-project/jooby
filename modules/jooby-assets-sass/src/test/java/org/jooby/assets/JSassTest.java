package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSassTest {

  @Test
  public void name() throws Exception {
    assertEquals("sass", new Sass().name());
  }

  @Test
  public void basic() throws Exception {
    assertEquals(".some-selector {\n" +
        "  width: 123px; }\n",
        new Sass().process("/css/x.scss", "$someVar: 123px; .some-selector { width: $someVar; }",
            ConfigFactory.empty()));
  }

  @Test
  public void some() throws Exception {
    assertEquals("body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n",
        new Sass().process("/styles.scss", "$font-stack:    Helvetica, sans-serif;\n" +
            "$primary-color: #333;\n" +
            "\n" +
            "body {\n" +
            "  font: 100% $font-stack;\n" +
            "  color: $primary-color;\n" +
            "}\n", ConfigFactory.empty()));
  }

  @Test
  public void sass() throws Exception {
    assertEquals("body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n",
        new Sass().set("syntax", "sass").process("/styles.scss",
            "$font-stack:    Helvetica, sans-serif\n" +
                "$primary-color: #333\n" +
                "\n" +
                "body\n" +
                "  font: 100% $font-stack\n" +
                "  color: $primary-color\n",
            ConfigFactory.empty()));
  }

  @Test
  public void fn() throws Exception {
    assertEquals(".foo {\n" +
        "  color: lime; }\n",
        new Sass().process("/styles.scss",
            "$color: hsl(120deg, 100%, 50%);\n.foo {\n  color: $color;\n}", ConfigFactory.empty()));
  }

  @Test
  public void customfn() throws Exception {
    assertEquals(".my-module {\n" +
        "  padding: 15px; }\n",
        new Sass().process("/styles.scss",
            "@function my-calculation-function($some-number, $another-number){\n" +
                "  @return $some-number + $another-number\n" +
                "}\n.my-module {\n" +
                "  padding: my-calculation-function(10px, 5px);\n" +
                "}",
            ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void err() throws Exception {
    assertEquals("body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n",
        new Sass().process("/styles.scss", "$font-stack:    Helvetica, sans-serif;\n" +
            "$primary-color: #333;\n" +
            "\n" +
            "body {\n" +
            "  font: 100% $font-stack\n" +
            "  color: $primary-color;\n" +
            "}\n", ConfigFactory.empty()));
  }

  @Test
  public void importDirective() throws Exception {
    assertEquals(".foo {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "",
        new Sass().process("/styles.scss",
            "@import 'foo';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void importFnF() throws Exception {
    assertEquals(".foo {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "",
        new Sass().process("/styles.scss",
            "\n@import 'missing';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty()));
  }
  
  @Test
  public void importDirectiveRelative() throws Exception {
    assertEquals(".relative {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n",
        new Sass().process("/relative/styles.scss",
            "@import 'foo';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty()));
  }
  
  @Test
  public void importDirectiveFromDir() throws Exception {
    Sass sass = new Sass();
    // manually create a URLClassLoader that points to the resources directory, normally handled by the AssetClassLoader class
    Path dirPath = Paths.get(JSassTest.class.getResource("/relative/nested/bar.scss").toURI()).getParent();
    ClassLoader classLoader = new URLClassLoader(new URL[]{dirPath.toUri().toURL()});
    String result = sass.process("/styles.scss",
        "@import 'bar';\n\n" +
            "\n" +
            "body {\n" +
            "  color: blue;\n" +
            "}\n",
        ConfigFactory.empty(), classLoader);
    
    assertEquals(".bar {\n" +
            "  color: green; }\n" +
            "\n" +
            "body {\n" +
            "  color: blue; }\n",
        result);
  }

  @Test
  public void importDirectiveWithExt() throws Exception {
    assertEquals(".foo {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "",
        new Sass().process("/styles.scss",
            "@import 'foo.scss';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty()));
  }

  @Test
  public void inlineSourceMap() throws Exception {
    String output = new Sass().set("sourcemap", "inline")
        .process("/styles.scss",
            "@import 'foo.scss';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty());
    assertTrue(output.startsWith(".foo {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "\n" +
        "/*# sourceMappingURL=data:application/json;base64,ewoJInZlcnNpb24iOiAzLAoJImZpbGUiOiAiLi4vLi4v"));
  }

  @Test
  public void sourceMap() throws Exception {
    assertEquals(".relative {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "\n" +
        "/*# sourceMappingURL=styles.scss.map */",
        new Sass().set("sourcemap", "file")
            .process("/relative/styles.scss",
                "@import 'foo.scss';\n$font-stack:    Helvetica, sans-serif;\n" +
                    "$primary-color: #333;\n" +
                    "\n" +
                    "body {\n" +
                    "  font: 100% $font-stack;\n" +
                    "  color: $primary-color;\n" +
                    "}\n",
                ConfigFactory.empty()));

    String output = new Sass().set("sourcemap", "file")
        .process("/relative/styles.scss.map",
            "@import 'foo.scss';\n$font-stack:    Helvetica, sans-serif;\n" +
                "$primary-color: #333;\n" +
                "\n" +
                "body {\n" +
                "  font: 100% $font-stack;\n" +
                "  color: $primary-color;\n" +
                "}\n",
            ConfigFactory.empty());
    assertTrue(output.contains("\"version\": 3"));
  }

}
