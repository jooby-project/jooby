/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.graphql;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;

import javax.annotation.Nonnull;

/**
 * GraphiQL module: https://github.com/graphql/graphiql.
 *
 * Usage:
 *
 * <pre>{@code
 *
 *   install(new GraphiQLModule());
 *
 * }</pre>
 *
 * Module install a GET route under <code>/graphql</code> path. Optionally, you can change
 * the route path by setting the <code>graphql.path</code> property in your application
 * configuration file.
 *
 * @author edgar
 * @since 2.4.0
 */
public class GraphiQLModule implements Extension {

  private static final String RESOURCES =
      "  <link href=\"{{contextPath}}/graphql/static/graphiql.css\" rel=\"stylesheet\" />\n"
          + "  <script src=\"{{contextPath}}/graphql/static/es6-promise.auto.min.js\"></script>\n"
          + "  <script src=\"{{contextPath}}/graphql/static/fetch.min.js\"></script>\n"
          + "  <script src=\"{{contextPath}}/graphql/static/react.min.js\"></script>\n"
          + "  <script src=\"{{contextPath}}/graphql/static/react-dom.min.js\"></script>\n"
          + "  <script src=\"{{contextPath}}/graphql/static/graphiql.min.js\"></script>\n";

  @Override public void install(@Nonnull Jooby application) throws Exception {
    String cpath = application.getContextPath();
    if (cpath == null || cpath.equals("/")) {
      cpath = "";
    }
    String index = INDEX.replace("{{contextPath}}", cpath);

    String graphqlPath = application.getEnvironment().getProperty("graphql.path", "/graphql");
    application.assets("/graphql/static/*", "/graphiql");
    application.get(graphqlPath, ctx -> ctx.setResponseType(MediaType.html).send(index));
  }

  private static final String INDEX = "<!--\n"
      + "The request to this GraphQL server provided the header \"Accept: text/html\"\n"
      + "and as a result has been presented GraphiQL - an in-browser IDE for\n"
      + "exploring GraphQL.\n"
      + "\n"
      + "If you wish to receive JSON, provide the header \"Accept: application/json\" or\n"
      + "add \"&raw\" to the end of the URL within a browser.\n"
      + "-->\n"
      + "<!DOCTYPE html>\n"
      + "<html>\n"
      + "<head>\n"
      + "  <meta charset=\"utf-8\" />\n"
      + "  <title>GraphiQL</title>\n"
      + "  <meta name=\"robots\" content=\"noindex\" />\n"
      + "  <meta name=\"referrer\" content=\"origin\" />\n"
      + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n"
      + "  <style>\n"
      + "    body {\n"
      + "      margin: 0;\n"
      + "      overflow: hidden;\n"
      + "    }\n"
      + "    #graphiql {\n"
      + "      height: 100vh;\n"
      + "    }\n"
      + "  </style>\n"
      + RESOURCES
      + "</head>\n"
      + "<body>\n"
      + "<div id=\"graphiql\">Loading...</div>\n"
      + "<script>\n"
      + "  // Collect the URL parameters\n"
      + "  var parameters = {};\n"
      + "  window.location.search.substr(1).split('&').forEach(function (entry) {\n"
      + "    var eq = entry.indexOf('=');\n"
      + "    if (eq >= 0) {\n"
      + "      parameters[decodeURIComponent(entry.slice(0, eq))] =\n"
      + "          decodeURIComponent(entry.slice(eq + 1));\n"
      + "    }\n"
      + "  });\n"
      + "\n"
      + "  // Produce a Location query string from a parameter object.\n"
      + "  function locationQuery(params) {\n"
      + "    return '?' + Object.keys(params).filter(function (key) {\n"
      + "      return Boolean(params[key]);\n"
      + "    }).map(function (key) {\n"
      + "      return encodeURIComponent(key) + '=' +\n"
      + "          encodeURIComponent(params[key]);\n"
      + "    }).join('&');\n"
      + "  }\n"
      + "\n"
      + "  // Derive a fetch URL from the current URL, sans the GraphQL parameters.\n"
      + "  var graphqlParamNames = {\n"
      + "    query: true,\n"
      + "    variables: true,\n"
      + "    operationName: true\n"
      + "  };\n"
      + "\n"
      + "  var otherParams = {};\n"
      + "  for (var k in parameters) {\n"
      + "    if (parameters.hasOwnProperty(k) && graphqlParamNames[k] !== true) {\n"
      + "      otherParams[k] = parameters[k];\n"
      + "    }\n"
      + "  }\n"
      + "  var fetchURL = locationQuery(otherParams);\n"
      + "\n"
      + "  // Defines a GraphQL fetcher using the fetch API.\n"
      + "  function graphQLFetcher(graphQLParams) {\n"
      + "    return fetch(fetchURL, {\n"
      + "      method: 'post',\n"
      + "      headers: {\n"
      + "        'Accept': 'application/json',\n"
      + "        'Content-Type': 'application/json'\n"
      + "      },\n"
      + "      body: JSON.stringify(graphQLParams),\n"
      + "      credentials: 'include',\n"
      + "    }).then(function (response) {\n"
      + "      return response.json();\n"
      + "    });\n"
      + "  }\n"
      + "\n"
      + "  // When the query and variables string is edited, update the URL bar so\n"
      + "  // that it can be easily shared.\n"
      + "  function onEditQuery(newQuery) {\n"
      + "    parameters.query = newQuery;\n"
      + "    updateURL();\n"
      + "  }\n"
      + "\n"
      + "  function onEditVariables(newVariables) {\n"
      + "    parameters.variables = newVariables;\n"
      + "    updateURL();\n"
      + "  }\n"
      + "\n"
      + "  function onEditOperationName(newOperationName) {\n"
      + "    parameters.operationName = newOperationName;\n"
      + "    updateURL();\n"
      + "  }\n"
      + "\n"
      + "  function updateURL() {\n"
      + "    history.replaceState(null, null, locationQuery(parameters));\n"
      + "  }\n"
      + "\n"
      + "  // Render <GraphiQL /> into the body.\n"
      + "  ReactDOM.render(\n"
      + "      React.createElement(GraphiQL, {\n"
      + "        fetcher: graphQLFetcher,\n"
      + "        onEditQuery: onEditQuery,\n"
      + "        onEditVariables: onEditVariables,\n"
      + "        onEditOperationName: onEditOperationName,\n"
      + "        query: undefined,\n"
      + "        response: undefined,\n"
      + "        variables: undefined,\n"
      + "        operationName: undefined,\n"
      + "      }),\n"
      + "      document.getElementById('graphiql')\n"
      + "  );\n"
      + "</script>\n"
      + "</body>\n"
      + "</html>\n";
}
