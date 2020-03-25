package issues.i1596;

import io.jooby.Jooby;

public class ClassLevelTagApp extends Jooby {
  {
    mvc(new ClassLevelController());
  }
}
