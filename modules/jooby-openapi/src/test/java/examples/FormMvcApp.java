package examples;

import io.jooby.Jooby;

public class FormMvcApp extends Jooby {

  {
    mvc(new FormController());
  }
}
