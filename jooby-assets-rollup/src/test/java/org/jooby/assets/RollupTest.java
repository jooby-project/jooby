package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class RollupTest {

  @Test
  public void name() throws Exception {
    assertEquals("rollup", new Rollup().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals("console.log( cube( 5 ) ); // 125",
        new Rollup()
            .process("/main.js",
                "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void inlineSourceMap() throws Exception {
    assertEquals("// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125\n" +
        "//#sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjpudWxsLCJzb3VyY2VzIjpbIi9tYXRocy5qcyIsIi9tYWluLmpzIl0sInNvdXJjZXNDb250ZW50IjpbIi8vIFRoaXMgZnVuY3Rpb24gaXNuJ3QgdXNlZCBhbnl3aGVyZSwgc29cbi8vIFJvbGx1cCBleGNsdWRlcyBpdCBmcm9tIHRoZSBidW5kbGUuLi5cbmV4cG9ydCBmdW5jdGlvbiBzcXVhcmUgKCB4ICkge1xuICByZXR1cm4geCAqIHg7XG59XG5cbi8vIFRoaXMgZnVuY3Rpb24gZ2V0cyBpbmNsdWRlZFxuZXhwb3J0IGZ1bmN0aW9uIGN1YmUgKCB4ICkge1xuICAvLyByZXdyaXRlIHRoaXMgYXMgYHNxdWFyZSggeCApICogeGBcbiAgLy8gYW5kIHNlZSB3aGF0IGhhcHBlbnMhXG4gIHJldHVybiB4ICogeCAqIHg7XG59IiwiaW1wb3J0IHsgY3ViZSB9IGZyb20gJy4vbWF0aHMuanMnO1xuY29uc29sZS5sb2coIGN1YmUoIDUgKSApOyAvLyAxMjUiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IjtBQU9PLFNBQVMsSUFBSSxHQUFHLENBQUMsR0FBRzs7O0VBR3pCLE9BQU8sQ0FBQyxHQUFHLENBQUMsR0FBRyxDQUFDOzs7QUNUbEIsT0FBTyxDQUFDLEdBQUcsRUFBRSxJQUFJLEVBQUUsQ0FBQyxFQUFFLEVBQUUsQ0FBQyJ9",
        new Rollup()
            .set("output.sourceMap", "inline")
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void imports() throws Exception {
    assertEquals("// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * x * x;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125",
        new Rollup()
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void importRelative() throws Exception {
    assertEquals("// This function gets included\n" +
        "function cube ( x ) {\n" +
        "  // rewrite this as `square( x ) * x`\n" +
        "  // and see what happens!\n" +
        "  return x * 3;\n" +
        "}\n" +
        "\n" +
        "console.log( cube( 5 ) ); // 125",
        new Rollup()
            .process("/relative/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void iife() throws Exception {
    assertEquals("(function () { 'use strict';\n" +
        "\n" +
        "  // This function gets included\n" +
        "  function cube ( x ) {\n" +
        "    // rewrite this as `square( x ) * x`\n" +
        "    // and see what happens!\n" +
        "    return x * x * x;\n" +
        "  }\n" +
        "\n" +
        "  console.log( cube( 5 ) ); // 125\n" +
        "\n" +
        "})();",
        new Rollup()
            .set("output.format", "iife")
            .process("/main.js",
                "import { cube } from './maths.js';\n" +
                    "console.log( cube( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

  @Test
  public void amd() throws Exception {
    assertEquals("define(function () { 'use strict';\n" +
        "\n" +
        "  // This function gets included\n" +
        "  function cube ( x ) {\n" +
        "    // rewrite this as `square( x ) * x`\n" +
        "    // and see what happens!\n" +
        "    return x * x * x;\n" +
        "  }\n" +
        "\n" +
        "  console.log( cube( 5 ) ); // 125\n" +
        "\n" +
        "});",
        new Rollup()
            .set("output.format", "amd")
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
        "var constants = {\n" +
        "  π: π,\n" +
        "  e: e,\n" +
        "  φ: φ,\n" +
        "  λ: λ\n" +
        "};\n" +
        "\n" +
        "// In some cases, you don't know which exports will\n" +
        "// be accessed until you actually run the code. In\n" +
        "// these cases, Rollup creates a namespace object\n" +
        "// for dynamic lookup\n" +
        "Object.keys( constants ).forEach( key => {\n" +
        "  console.log( `The value of ${key} is ${constants[key]}` );\n" +
        "});",
        new Rollup()
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
    new Rollup().process("/main.js", "import * as fnf from './fnf';\n", ConfigFactory.empty());
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
            .process("/main.js",
                "import { cubex } from './maths.js';\n" +
                    "console.log( cubex( 5 ) ); // 125",
                ConfigFactory.empty()));
  }

}
