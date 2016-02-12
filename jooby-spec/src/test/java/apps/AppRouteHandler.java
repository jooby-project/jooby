package apps;

import org.jooby.Jooby;
import org.jooby.Route;

import apps.model.Pet;

public class AppRouteHandler extends Jooby {

  {

    Route.OneArgHandler home = req -> {
      return new Pet();
    };

    // doc1
    get("/var", home);
  }
}
