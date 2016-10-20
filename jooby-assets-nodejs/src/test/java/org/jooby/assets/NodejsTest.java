package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class NodejsTest {

  @Test
  public void deployjar() throws Exception {
    Nodejs node = new Nodejs(new File("target"));
    Path dir = node.deploy("META-INF/resources/webjars/jquery/3.1.1");
    assertEquals(Paths.get("target", "node_modules", "META-INF.resources.webjars.jquery.3.1.1"),
        dir);
    assertTrue(dir.resolve("package.json").toFile().exists());
  }
}
