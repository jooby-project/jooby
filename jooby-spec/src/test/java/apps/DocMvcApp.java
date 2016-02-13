package apps;

import org.jooby.Jooby;

public class DocMvcApp extends Jooby {

  {
    use(Pets.class);
  }

}
