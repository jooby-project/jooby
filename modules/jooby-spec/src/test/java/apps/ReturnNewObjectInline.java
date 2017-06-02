package apps;

import org.jooby.Jooby;

import apps.model.Pet;

public class ReturnNewObjectInline extends Jooby {

  {
    get("/", req -> {
      return new Pet();
    });
  }

}
