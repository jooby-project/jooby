package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class ReactTest {

  @Test
  public void name() throws Exception {
    assertEquals("react", new React().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals("(function () {\n" +
        "'use strict';\n" +
        "\n" +
        "(function(exports) {\n" +
        "  exports.React = {};\n" +
        "})(window);\n" +
        "\n" +
        "(function(exports) {\n" +
        "  exports.ReactDOM = {};\n" +
        "})(window);\n" +
        "\n" +
        "var Home = function Home() {\n" +
        "  return React.createElement(\n" +
        "    'div',\n" +
        "    null,\n" +
        "    React.createElement(\n" +
        "      'h2',\n" +
        "      null,\n" +
        "      'Home'\n" +
        "    )\n" +
        "  );\n" +
        "};\n" +
        "\n" +
        "ReactDOM.render(React.createElement(Home, null), document.getElementById('root'));\n" +
        "\n" +
        "}());\n" +
        "",
        new React()
            .set("basedir", Paths.get("src", "test", "resources").toString())
            .process("/index.js",
                "import React from 'react';\n" +
                    "import ReactDOM from 'react-dom';\n" +
                    "\n" +
                    "const Home = () => (\n" +
                    "  <div>\n" +
                    "    <h2>Home</h2>\n" +
                    "  </div>\n" +
                    ")\n" +
                    "\n" +
                    "ReactDOM.render(<Home />, document.getElementById('root'));",
                ConfigFactory.empty()));
  }

  @Test
  public void importFile() throws Exception {
    assertEquals("(function () {\n" +
        "'use strict';\n" +
        "\n" +
        "(function(exports) {\n" +
        "  exports.React = {};\n" +
        "})(window);\n" +
        "\n" +
        "(function(exports) {\n" +
        "  exports.ReactDOM = {};\n" +
        "})(window);\n" +
        "\n" +
        "var App = function App() {\n" +
        "  return React.createElement(\n" +
        "    'div',\n" +
        "    null,\n" +
        "    React.createElement(\n" +
        "      'h2',\n" +
        "      null,\n" +
        "      'App'\n" +
        "    )\n" +
        "  );\n" +
        "};\n" +
        "\n" +
        "ReactDOM.render(React.createElement(App, null), document.getElementById('root'));\n" +
        "\n" +
        "}());\n" +
        "",
        new React()
            .set("basedir", Paths.get("src", "test", "resources").toString())
            .process("/index.js",
                "import React from 'react';\n" +
                    "import ReactDOM from 'react-dom';\n" +
                    "import App from './App';\n" +
                    "\n" +
                    "ReactDOM.render(<App />, document.getElementById('root'));",
                ConfigFactory.empty()));
  }

}
