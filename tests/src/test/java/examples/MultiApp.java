/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.List;

import examples.multiapp.BarApp;
import examples.multiapp.FooApp;
import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.jetty.JettyServer;

public class MultiApp {

  public static void main(String[] args) {
    Jooby.runApp(args, new JettyServer(), ExecutionMode.DEFAULT, List.of(BarApp::new, FooApp::new));
  }
}
