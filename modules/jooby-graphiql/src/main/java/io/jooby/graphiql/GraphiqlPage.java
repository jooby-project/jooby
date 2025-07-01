/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.graphiql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * From: https://github.com/graphql/graphiql/tree/main/examples/graphiql-cdn
 * https://github.com/graphql/graphiql/blob/main/examples/graphiql-cdn/index.html
 */
class GraphiqlPage {
  public static String index(String path) throws IOException {
    var indexPath = "index.html";
    try (var in = GraphiqlPage.class.getResourceAsStream(indexPath)) {
      if (in == null) {
        throw new FileNotFoundException(indexPath);
      }
      var source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      /*
       * Look for
       *  const fetcher = createGraphiQLFetcher({
       *    url: 'https://countries.trevorblades.com',
       *  });
       */
      var key = "https://countries.trevorblades.com";
      int keyIndex = source.indexOf(key);
      if (keyIndex == -1) {
        throw new IllegalStateException("Graphiql page out of sync");
      }
      return source.replace(key, path);
    }
  }
}
