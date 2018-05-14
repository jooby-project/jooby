package hazelcast;

import org.jooby.Jooby;
import org.jooby.hazelcast.Hcast;
import org.jooby.hazelcast.HcastSessionStore;

public class HcastApp extends Jooby {

  {
    use(new Hcast());
    session(HcastSessionStore.class);
  }

  public static void main(final String[] args) {
    run(HcastApp::new, args);
  }
}
