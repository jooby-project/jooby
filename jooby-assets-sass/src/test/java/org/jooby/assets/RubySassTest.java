package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class RubySassTest {

  @Test
  public void name() throws Exception {
    assertEquals("sass", new Sass().name());
  }

  @Test
  public void defaults() throws Exception {
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
  public void importCurrent() throws Exception {
    assertEquals("h1 {\n" +
        "  color: #D32929; }\n" +
        "",
        new Sass().process("/assets/css/home.scss", "@import '_vars'; \n" +
            "\n" +
            "h1 {\n" +
            "  color: $ROJO;\n" +
            "}", ConfigFactory.empty()));
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
    assertEquals(".foo {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "/*# sourceMappingURL=data:application/json;base64,ewoidmVyc2lvbiI6IDMsCiJtYXBwaW5ncyI6ICJBQUFBLElBQUs7RUFDSCxLQUFLLEVBQUUsSUFBSTs7QUNHYixJQUFLO0VBQ0gsSUFBSSxFQUFFLDBCQUFnQjtFQUN0QixLQUFLLEVBSlMsSUFBSSIsCiJzb3VyY2VzIjogWyIvZm9vLnNjc3MiLCIvc3R5bGVzLnNjc3MiXSwKInNvdXJjZXNDb250ZW50IjogWyIuZm9vIHtcbiAgY29sb3I6ICNmZmY7XG59XG4iLG51bGxdLAoibmFtZXMiOiBbXSwKImZpbGUiOiAic3R5bGVzLnNjc3MiCn0= */\n"
        +
        "",
        new Sass().set("sourcemap", "inline")
            .process("/styles.scss",
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
  public void sourceMap() throws Exception {
    assertEquals(".relative {\n" +
        "  color: #fff; }\n" +
        "\n" +
        "body {\n" +
        "  font: 100% Helvetica, sans-serif;\n" +
        "  color: #333; }\n" +
        "/*# sourceMappingURL=/relative/styles.scss.map */\n" +
        "",
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

    assertEquals("{\n" +
        "\"version\": 3,\n" +
        "\"mappings\": \"AAAA,SAAU;EACR,KAAK,EAAE,IAAI;;ACGb,IAAK;EACH,IAAI,EAAE,0BAAgB;EACtB,KAAK,EAJS,IAAI\",\n"
        +
        "\"sources\": [\"/relative/foo.scss\",\"/relative/styles.scss\"],\n" +
        "\"names\": [],\n" +
        "\"file\": \"styles.scss\"\n" +
        "}",
        new Sass().set("sourcemap", "file")
            .process("/relative/styles.scss.map",
                "@import 'foo.scss';\n$font-stack:    Helvetica, sans-serif;\n" +
                    "$primary-color: #333;\n" +
                    "\n" +
                    "body {\n" +
                    "  font: 100% $font-stack;\n" +
                    "  color: $primary-color;\n" +
                    "}\n",
                ConfigFactory.empty()));
  }

}
