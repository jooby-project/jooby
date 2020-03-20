package issues.i1581;

import io.jooby.Jooby;

public class App1581 extends Jooby {
  {
    AppComponent dagger = DaggerAppComponent.builder().build();

    mvc(dagger.myController());
  }
}
