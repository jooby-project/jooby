/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import picocli.CommandLine;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Optional;

/**
 * Jooby version. It try to fetch latest version from maven repository or fallback to package
 * implementation version.
 *
 */
public class Version implements CommandLine.IVersionProvider {

  /** VERSION. */
  public static final String VERSION = doVersion();

  @Override public String[] getVersion() {
    return new String[]{VERSION};
  }

  private static String doVersion() {
    try {
      URL url = URI
          .create("http://search.maven.org/solrsearch/select?q=+g:io.jooby+a:jooby&start=0&rows=1")
          .toURL();
      URLConnection connection = url.openConnection();
      try (InputStream in = connection.getInputStream()) {
        JSONObject json = new JSONObject(new JSONTokener(in));
        JSONObject response = json.getJSONObject("response");
        JSONArray docs = response.getJSONArray("docs");
        JSONObject jooby = docs.getJSONObject(0);
        return jooby.getString("latestVersion");
      }
    } catch (Exception x) {
      return Optional.ofNullable(Version.class.getPackage())
          .map(Package::getImplementationVersion)
          .filter(Objects::nonNull)
          .orElse("2.0.5");
    }
  }
}
