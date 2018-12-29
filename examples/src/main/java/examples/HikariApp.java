/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
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
    run(HikariApp::new, args);
  }
}
