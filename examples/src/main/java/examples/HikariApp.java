/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.hikari.Hikari;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class HikariApp extends Jooby {

  {
    install(new Hikari("jdbc:mysql://localhost/hello?user=root&password="));

    get("/", ctx -> {
      try (Connection connection = require(DataSource.class).getConnection()) {
        try (PreparedStatement stt = connection.prepareStatement("select * from users")) {
          try (ResultSet rs = stt.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) {
              names.add(rs.getString(1));
            }
            return names;
          }
        }
      }
    });
  }

  public static void main(String[] args) {
    runApp(args, HikariApp::new);
  }
}
