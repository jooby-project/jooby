package apps;

import org.jooby.Jooby;

public class PetMvcApp extends Jooby {

  {
    use(PetMvc.class);
  }
}
