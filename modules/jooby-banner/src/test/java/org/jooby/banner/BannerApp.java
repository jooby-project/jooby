package org.jooby.banner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.funzy.Throwing;

import java.util.List;

public class BannerApp {

  public static void main(final String[] args) throws Throwable {
    Config conf = ConfigFactory.empty()
        .withValue("application.name", ConfigValueFactory.fromAnyRef("banner"))
        .withValue("maxAge", ConfigValueFactory.fromAnyRef(-1))
        .withValue("application.version", ConfigValueFactory.fromAnyRef("1.0.0"));
    System.out.println(conf.getDuration("maxAge").getSeconds());
    Env env = Env.DEFAULT.build(conf);
    new Banner("jooby").configure(env, conf, null);
    List<Throwing.Consumer<Registry>> startTasks = env.startTasks();
    for (Throwing.Consumer<Registry> task : startTasks) {
      task.accept(null);
    }
  }
}
