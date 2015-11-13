package ${package};

import org.junit.Test;

/**
 * @author jooby generator
 */
public class AppTest extends BaseTest {

  @Test
  public void index() throws Exception {
    server.get("/")
        .expect(200)
        .header("Content-Type", "text/html;charset=UTF-8");
  }

}
