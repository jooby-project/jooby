package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class BabelTest {

  @Test
  public void name() throws Exception {
    assertEquals("babel", new Babel().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "code();",
        new Babel()
            .process("/x.js",
                "code();",
                ConfigFactory.empty()));
  }

  @Test
  public void imports() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } else { var newObj = {}; if (obj != null) { for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key]; } } newObj[\"default\"] = obj; return newObj; } }\n" +
        "\n" +
        "var _math = require(\"math\");\n" +
        "\n" +
        "var math = _interopRequireWildcard(_math);\n" +
        "\n" +
        "alert(\"2 = \" + math.sum(math.pi, math.pi));",
        new Babel()
            .process("/x.js",
                "import * as math from \"math\";\n" +
                "alert(\"2 = \" + math.sum(math.pi, math.pi));",
                ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void err() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "code(;",
        new Babel()
            .process("/x.js",
                "co de(;",
                ConfigFactory.empty()));
  }

  @Test
  public void simpleDom() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var myDivElement = React.createElement(\"div\", { className: \"foo\" });",
        new Babel()
            .process("/x.js",
                "var myDivElement = <div className=\"foo\" />;",
                ConfigFactory.empty()));
  }

  @Test
  public void inlineSourceMaps() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var myDivElement = React.createElement(\"div\", { className: \"foo\" });\n" +
        "//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi94LmpzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7O0FBQUEsSUFBSSxZQUFZLEdBQUcsNkJBQUssU0FBUyxFQUFDLEtBQUssR0FBRyxDQUFDIiwiZmlsZSI6Ii94LmpzIiwic291cmNlc0NvbnRlbnQiOlsidmFyIG15RGl2RWxlbWVudCA9IDxkaXYgY2xhc3NOYW1lPVwiZm9vXCIgLz47Il19",
        new Babel()
            .set("sourceMaps", "inline")
            .process("/x.js",
                "var myDivElement = <div className=\"foo\" />;",
                ConfigFactory.empty()));
  }

  @Test
  public void polyfill() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }\n"
        +
        "\n" +
        "var fibonacci = _defineProperty({}, Symbol.iterator, function () {\n" +
        "  var pre = 0,\n" +
        "      cur = 1;\n" +
        "  return {\n" +
        "    next: function next() {\n" +
        "      var _ref = [cur, pre + cur];\n" +
        "      pre = _ref[0];\n" +
        "      cur = _ref[1];\n" +
        "\n" +
        "      return { done: false, value: cur };\n" +
        "    }\n" +
        "  };\n" +
        "});\n" +
        "\n" +
        "var _iteratorNormalCompletion = true;\n" +
        "var _didIteratorError = false;\n" +
        "var _iteratorError = undefined;\n" +
        "\n" +
        "try {\n" +
        "  for (var _iterator = fibonacci[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {\n"
        +
        "    var n = _step.value;\n" +
        "\n" +
        "    // truncate the sequence at 1000\n" +
        "    if (n > 1000) break;\n" +
        "    console.log(n);\n" +
        "  }\n" +
        "} catch (err) {\n" +
        "  _didIteratorError = true;\n" +
        "  _iteratorError = err;\n" +
        "} finally {\n" +
        "  try {\n" +
        "    if (!_iteratorNormalCompletion && _iterator[\"return\"]) {\n" +
        "      _iterator[\"return\"]();\n" +
        "    }\n" +
        "  } finally {\n" +
        "    if (_didIteratorError) {\n" +
        "      throw _iteratorError;\n" +
        "    }\n" +
        "  }\n" +
        "}",
        new Babel()
            .process("/x.js",
                "let fibonacci = {\n" +
                    "  [Symbol.iterator]() {\n" +
                    "    let pre = 0, cur = 1;\n" +
                    "    return {\n" +
                    "      next() {\n" +
                    "        [pre, cur] = [cur, pre + cur];\n" +
                    "        return { done: false, value: cur }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n" +
                    "\n" +
                    "for (var n of fibonacci) {\n" +
                    "  // truncate the sequence at 1000\n" +
                    "  if (n > 1000)\n" +
                    "    break;\n" +
                    "  console.log(n);\n" +
                    "}",
                ConfigFactory.empty()));
  }

  @Test
  public void polyfillWithExternalHelpers() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var fibonacci = babelHelpers.defineProperty({}, Symbol.iterator, function () {\n" +
        "  var pre = 0,\n" +
        "      cur = 1;\n" +
        "  return {\n" +
        "    next: function next() {\n" +
        "      var _ref = [cur, pre + cur];\n" +
        "      pre = _ref[0];\n" +
        "      cur = _ref[1];\n" +
        "\n" +
        "      return { done: false, value: cur };\n" +
        "    }\n" +
        "  };\n" +
        "});\n" +
        "\n" +
        "var _iteratorNormalCompletion = true;\n" +
        "var _didIteratorError = false;\n" +
        "var _iteratorError = undefined;\n" +
        "\n" +
        "try {\n" +
        "  for (var _iterator = fibonacci[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {\n" +
        "    var n = _step.value;\n" +
        "\n" +
        "    // truncate the sequence at 1000\n" +
        "    if (n > 1000) break;\n" +
        "    console.log(n);\n" +
        "  }\n" +
        "} catch (err) {\n" +
        "  _didIteratorError = true;\n" +
        "  _iteratorError = err;\n" +
        "} finally {\n" +
        "  try {\n" +
        "    if (!_iteratorNormalCompletion && _iterator[\"return\"]) {\n" +
        "      _iterator[\"return\"]();\n" +
        "    }\n" +
        "  } finally {\n" +
        "    if (_didIteratorError) {\n" +
        "      throw _iteratorError;\n" +
        "    }\n" +
        "  }\n" +
        "}",
        new Babel()
            .set("externalHelpers", true)
            .process("/x.js",
                "let fibonacci = {\n" +
                    "  [Symbol.iterator]() {\n" +
                    "    let pre = 0, cur = 1;\n" +
                    "    return {\n" +
                    "      next() {\n" +
                    "        [pre, cur] = [cur, pre + cur];\n" +
                    "        return { done: false, value: cur }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n" +
                    "\n" +
                    "for (var n of fibonacci) {\n" +
                    "  // truncate the sequence at 1000\n" +
                    "  if (n > 1000)\n" +
                    "    break;\n" +
                    "  console.log(n);\n" +
                    "}",
                ConfigFactory.empty()));
  }

  @Test
  public void blacklist() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var myDivElement = <div className=\"foo\" />;",
        new Babel()
            .set("blacklist", Arrays.asList("react"))
            .process("/x.js",
                "var myDivElement = <div className=\"foo\" />;",
                ConfigFactory.empty()));
  }

  @Test
  public void ecma6Arrow() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var odds = evens.map(function (v) {\n" +
        "  return v + 1;\n" +
        "});",
        new Babel()
            .process("/x.js",
                "var odds = evens.map(v => v + 1);",
                ConfigFactory.empty()));
  }

  @Test
  public void ecma6TemplateStr() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var name = \"Bob\",\n" +
        "    time = \"today\";\n" +
        "\"Hello \" + name + \", how are you \" + time + \"?\";",
        new Babel()
            .process("/x.js",
                "var name = \"Bob\", time = \"today\";\n" +
                    "`Hello ${name}, how are you ${time}?`",
                ConfigFactory.empty()));
  }

  @Test
  public void ecma6LexicalThis() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var bob = {\n" +
        "  _name: \"Bob\",\n" +
        "  _friends: [],\n" +
        "  printFriends: function printFriends() {\n" +
        "    var _this = this;\n" +
        "\n" +
        "    this._friends.forEach(function (f) {\n" +
        "      return console.log(_this._name + \" knows \" + f);\n" +
        "    });\n" +
        "  }\n" +
        "};",
        new Babel()
            .process("/x.js",
                "var bob = {\n" +
                    "  _name: \"Bob\",\n" +
                    "  _friends: [],\n" +
                    "  printFriends() {\n" +
                    "    this._friends.forEach(f =>\n" +
                    "      console.log(this._name + \" knows \" + f));\n" +
                    "  }\n" +
                    "};",
                ConfigFactory.empty()));
  }

  @Test
  public void externalHelpers() throws Exception {
    assertEquals("\"use strict\";\n" +
        "\n" +
        "var odds = evens.map(function (v) {\n" +
        "  return v + 1;\n" +
        "});",
        new Babel()
            .set("externalHelpers", true)
            .process("/x.js",
                "var odds = evens.map(v => v + 1);",
                ConfigFactory.empty()));
  }

}
