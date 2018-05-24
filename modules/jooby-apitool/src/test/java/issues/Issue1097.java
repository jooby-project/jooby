package issues;

import apps.App1097;
import io.swagger.util.Yaml;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1097 {

  @Test
  public void shouldUseCustomTagger() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1097());

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"song\"\n"
        + "- name: \"album\"\n"
        + "- name: \"artist\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /v5/api/song:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"song\"\n"
        + "      operationId: \"getSong\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n"
        + "  /v5/api/album:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"album\"\n"
        + "      operationId: \"getAlbum\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n"
        + "  /v5/api/artist:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"artist\"\n"
        + "      operationId: \"getArtist\"\n"
        + "      parameters: []\n"
        + "      responses:\n"
        + "        200:\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", Yaml
        .mapper().writer().withDefaultPrettyPrinter().writeValueAsString(new SwaggerBuilder((r -> {
          String[] segments = r.pattern().split("/");
          return segments[segments.length - 1];
        }))
            .build(null, routes)));
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
