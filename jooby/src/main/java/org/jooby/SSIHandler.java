package org.jooby;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class SSIHandler implements Route.Handler {

  private final String file;
  private final String type;

  public SSIHandler(String file, String type) {
    this.file = file;
    this.type = type;
  }

  @Override
  public void handle(Request req, Response rsp) throws Throwable {
    StringBuilder op = new StringBuilder();
    process(op, file);
    Charset charset = Charset.forName("UTF-8");
    byte[] output = op.toString().getBytes(charset);
    rsp.charset(charset);
    rsp.length(output.length);
    rsp.type(type);
    rsp.status(200);
    rsp.send(output);
  }

  private void process(StringBuilder op, String file) throws IOException {
    InputStream stream = this.getClass().getResourceAsStream(file);
    if (stream == null) {
      stream = findResource(file, "/target/test-classes", "/target/classes");
    }
    LineNumberReader lnr = new LineNumberReader(new InputStreamReader(stream));
    String line = lnr.readLine();
    while (line != null) {
      if (line.contains("JOOBY-INCLUDE")) {
        process(op, toInclude(line));
      } else {
        op.append(line).append("\n");
      }
      line = lnr.readLine();
    }
  }

  private InputStream findResource(String file, String... paths) throws IOException {
    final String canonicalPath = new File(".").getCanonicalPath();
    for (String path : paths) {
      final String filePath = (canonicalPath + path) + file;
      System.err.println("--> " + filePath);
      if(new File(filePath).exists()) {
        return new FileInputStream(filePath);
      }
    }
    throw new UnsupportedOperationException(file + " not found in " + Arrays.toString(paths));
  }

  private String toInclude(String line) {
    int str = line.indexOf("JOOBY-INCLUDE ") + "JOOBY-INCLUDE ".length();
    return line.substring(str, line.indexOf(" ", str + 1));
  }
}
