package parser;

import org.jooby.Jooby;
import org.jooby.Upload;

import java.util.List;

public class FileApp extends Jooby {
  {
    post("/", req -> {
      Upload f1 = req.file("f1");
      List<Upload> files = req.files("files");
      return "Success";
    });
  }
}
