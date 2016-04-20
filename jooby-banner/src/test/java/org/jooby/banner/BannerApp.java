package org.jooby.banner;

import java.util.List;

import org.jooby.Env;
import org.jooby.Registry;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedConsumer;

public class BannerApp {

  public static void main(final String[] args) throws Throwable {
    Config conf = ConfigFactory.empty()
        .withValue("application.name", ConfigValueFactory.fromAnyRef("banner"))
        .withValue("maxAge", ConfigValueFactory.fromAnyRef(-1))
        .withValue("application.version", ConfigValueFactory.fromAnyRef("1.0.0"));
    System.out.println(conf.getDuration("maxAge").getSeconds());
    Env env = Env.DEFAULT.build(conf);
    new Banner("jooby").configure(env, conf, null);
    List<CheckedConsumer<Registry>> startTasks = env.startTasks();
    for (CheckedConsumer<Registry> task : startTasks) {
      task.accept(null);
    }
  }
}
