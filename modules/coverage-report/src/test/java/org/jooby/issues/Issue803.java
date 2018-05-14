package org.jooby.issues;

import java.util.Optional;

import org.jooby.Upload;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue803 extends ServerFeature {

  @Path("/803")
  public static class R803 {
    @POST
    public String create(final Optional<Upload> myfile) {
      return myfile.map(u -> u.name()).orElse("NONE");
    }

  }

  {
    use(R803.class);
  }

  @Test
  public void shouldNotThrowClassCastExceptionWhenThereIsNoFilename() throws Exception {

    request()
        .post("/803")
        .body(
            "--6yGZC7VT1Szi3opIM0ILA1XWmC6cb0ZQH\r\nContent-Disposition: form-data; name=\"myfile\"\r\nContent-Type: application/xml\r\nContent-Transfer-Encoding: binary\r\n\r\n<xml></xml>\r\n--6yGZC7VT1Szi3opIM0ILA1XWmC6cb0ZQH--\r\n",
            "multipart/form-data; boundary=6yGZC7VT1Szi3opIM0ILA1XWmC6cb0ZQH")
        .expect("NONE");
  }

}
