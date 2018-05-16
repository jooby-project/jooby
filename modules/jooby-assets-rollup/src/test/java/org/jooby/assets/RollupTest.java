package org.jooby.assets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Arrays;

public class RollupTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void name() throws Exception {
    assertEquals("rollup", new Rollup().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals("console.log( cube( 5 ) ); // 125\n",
        new Rollup()
            .set(engineFactory)
            .process("/main.js",
                "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void babel() throws Exception {
    assertEquals("var name = \"Babel\";\n" +
        "console.log(\"Hello \" + name);\n",
        new Rollup()
            .set(engineFactory)
            .set("plugins",
                ImmutableMap.of("babel", ImmutableMap.of("presets", Arrays.asList("es2015"))))
            .process("/babel.js", "var name = \"Babel\";\n"
                + "console.log( `Hello ${name}` );",
                ConfigFactory.empty()));
  }

  @Test
  public void babelImport() throws Exception {
    assertEquals("var hi = (function (message) {\n" +
        "  console.log(\"Hello \" + message);\n" +
        "});\n" +
        "\n" +
        "hi(\"babel\");\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("plugins", ImmutableMap.of("babel",
                ImmutableMap.of("presets",
                    Arrays.asList(Arrays.asList("es2015", ImmutableMap.of("modules", false))))))
            .process("/app.js", "import hi from './lib/lib.js';\n"
                + "hi(\"babel\");",
                ConfigFactory.empty()));
  }

  @Test
  public void babelExcludes() throws Exception {
    assertEquals("var hi = (message) => {\n" +
        "  console.log(`Hello ${message}`);\n" +
        "};\n" +
        "\n" +
        "hi(\"babel\");\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("plugins", ImmutableMap.of("babel", ImmutableMap.of("presets",
                Arrays.asList(Arrays.asList("es2015", ImmutableMap.of("modules", false))),
                "excludes", "/lib/*.js")))
            .process("/app.js", "import hi from './lib/lib.js';\n"
                + "hi(\"babel\");",
                ConfigFactory.empty()));
  }

  @Test
  public void inlineSourceMap() throws Exception {
    assertEquals("// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "\n" +
        "//#sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjpudWxsLCJzb3VyY2VzIjpbIi9tYXRocy5qcyIsIi9tYWluLmpzIl0sInNvdXJjZXNDb250ZW50IjpbIi8vIFRoaXMgZnVuY3Rpb24gaXNuJ3QgdXNlZCBhbnl3aGVyZSwgc29cbi8vIFJvbGx1cCBleGNsdWRlcyBpdCBmcm9tIHRoZSBidW5kbGUuLi5cbmV4cG9ydCBmdW5jdGlvbiBzcXVhcmUgKCB4ICkge1xuICByZXR1cm4geCAqIHg7XG59XG5cbi8vIFRoaXMgZnVuY3Rpb24gZ2V0cyBpbmNsdWRlZFxuZXhwb3J0IGZ1bmN0aW9uIGN1YmUgKCB4ICkge1xuICAvLyByZXdyaXRlIHRoaXMgYXMgYHNxdWFyZSggeCApICogeGBcbiAgLy8gYW5kIHNlZSB3aGF0IGhhcHBlbnMhXG4gIHJldHVybiB4ICogeCAqIHg7XG59IiwiaW1wb3J0IHsgY3ViZSB9IGZyb20gJy4vbWF0aHMuanMnO1xuY29uc29sZS5sb2coIGN1YmUoIDUgKSApOyAvLyAxMjUiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IkFBQUE7O0FBRUEsQUFBTyxBQUVOOzs7QUFHRCxBQUFPLFNBQVMsSUFBSSxHQUFHLENBQUMsR0FBRzs7O0VBR3pCLE9BQU8sQ0FBQyxHQUFHLENBQUMsR0FBRyxDQUFDLENBQUM7OztBQ1RuQixPQUFPLENBQUMsR0FBRyxFQUFFLElBQUksRUFBRSxDQUFDLEVBQUUsRUFBRSxDQUFDIn0=",
        new Rollup()
            .set(engineFactory)
            .set("generate.sourceMap", "inline")
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void legacy() throws Exception {
    assertEquals("(function() {\n" +
        "  var exports = window || global || this;\n" +
        "  exports.summary = function (message) {\n" +
        "    console.log(message);\n" +
        "  };\n" +
        "})();\n" +
        "\n" +
        "fn('foo');\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("plugins", ImmutableMap.of("legacy", ImmutableMap.of("/lib/legacy.js", "fn")))
            .process("/main.js",
                "import fn from 'lib/legacy';\n" +
                    "fn('foo');",
                ConfigFactory.empty()));
  }

  @Test
  public void namedLegacy() throws Exception {
    assertEquals("(function(exports) {\n" +
        "  exports.Named = {\n" +
        "   foo: 'foo',\n" +
        "   bar: 'bar'\n" +
        "  };\n" +
        "})(window);\n" +
        "\n" +
        "var foo = Named.foo;\n" +
        "\n" +
        "var bar = Named.bar;\n" +
        "\n" +
        "console.log(foo + bar);\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("context", "window")
            .set("plugins", ImmutableMap.of("legacy", ImmutableMap.of("/lib/legacy-named.js",
                ImmutableMap.of("Named", ImmutableList.of("foo", "bar")))))
            .process("/main.js",
                "import {foo, bar} from 'lib/legacy-named';\n" +
                    "console.log(foo + bar);",
                ConfigFactory.empty()));
  }

  @Test
  public void alias() throws Exception {
    assertEquals("var message = (message) => {\n" +
        "  console.log(`Hello ${message}`);\n" +
        "};\n" +
        "\n" +
        "message('foo');\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("plugins", ImmutableMap.of("alias", ImmutableMap.of("mylib", "lib/lib.js")))
            .process("/alias.js",
                "import message from 'mylib';\n" +
                    "message('foo');",
                ConfigFactory.empty()));
  }

  @Test
  public void imports() throws Exception {
    assertEquals("// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void importRelative() throws Exception {
    assertEquals("// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * 3;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .process("/relative/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void importNoExt() throws Exception {
    assertEquals("// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * 3;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .process("/relative/main.js",
                "import { cube } from 'maths';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void iife() throws Exception {
    assertEquals("(function () {\n" +
        "'use strict';\n" +
        "\n" +
        "// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "\n" +
        "}());\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("generate.format", "iife")
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void amd() throws Exception {
    assertEquals("define(function () { 'use strict';\n" +
        "\n" +
        "// This function isn't used anywhere, so\n" +
        "// Rollup excludes it from the bundle...\n" +
        "\n" +
        "\n" +
        "// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "\n" +
        "});\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .set("generate.format", "amd")
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void dynamicNamespace() throws Exception {
    assertEquals("const π = 3.14159;\n" +
        "const e = 2.71828;\n" +
        "const φ = 1.61803;\n" +
        "const λ = 1.30357;\n" +
        "\n" +
        "var constants = Object.freeze({\n" +
        "  π: π,\n" +
        "  e: e,\n" +
        "  φ: φ,\n" +
        "  λ: λ\n" +
        "});\n" +
        "\n" +
        "// In some cases, you don't know which exports will\n" +
        "// be accessed until you actually run the code. In\n" +
        "// these cases, Rollup creates a namespace object\n" +
        "// for dynamic lookup\n" +
        "Object.keys( constants ).forEach( key => {\n" +
        "  console.log( `The value of ${key} is ${constants[key]}` );\n" +
        "});\n" +
        "",
        new Rollup()
            .set(engineFactory)
            .process("/main.js",
                "import * as constants from './constants';\n" +
                    "\n" +
                    "// In some cases, you don't know which exports will\n" +
                    "// be accessed until you actually run the code. In\n" +
                    "// these cases, Rollup creates a namespace object\n" +
                    "// for dynamic lookup\n" +
                    "Object.keys( constants ).forEach( key => {\n" +
                    "  console.log( `The value of ${key} is ${constants[key]}` );\n" +
                    "});",
                ConfigFactory.empty())
            .replace("\t", "  "));
  }

  @Test(expected = AssetException.class)
  public void fileNotFound() throws Exception {
    new Rollup()
        .set(engineFactory)
        .process("/main.js", "import * as fnf from './fnf';\n", ConfigFactory.empty());
  }

  @Test(expected = AssetException.class)
  public void err() throws Exception {
    assertEquals("// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125",
        new Rollup()
            .set(engineFactory)
            .process("/main.js",
                "import { cubex } from './maths.js';\n" +
                    "console.log( cubex( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

}
