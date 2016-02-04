package apps;

import org.jooby.Jooby;

public class AppWithExternalRoutes extends Jooby {

  {
    SwaggerUI.install("/swagger", this);
    use(MvcRoutes.class);
  }

}
