import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue960 extends ServerFeature {
  {
    get("/960", () -> "No package");
  }

  @Test
  public void appWithoutPackageShouldWork() throws Exception {
    request()
        .get("/960")
        .expect("No package");
  }
}
