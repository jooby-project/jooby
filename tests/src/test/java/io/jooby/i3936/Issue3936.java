/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3936;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import io.jooby.handlebars.HandlebarsModule;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.htmx.HtmxErrorHandler;
import io.jooby.htmx.HtmxModule;
import io.jooby.htmx.HtmxResponse;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.TestUtil;
import okhttp3.FormBody;

class Issue3936 {

  @ServerTest
  void shouldUnderstandHtmxRequest(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new Jackson3Module());
              HtmxErrorHandler globalErrorHandler =
                  (ctx, cause, status) ->
                      HtmxResponse.empty(status)
                          .addOob(
                              "toast.hbs",
                              Map.of(
                                  "message",
                                  status.reason() + ": " + cause.getMessage(),
                                  "isError",
                                  true));
              app.install(new HtmxModule(globalErrorHandler));
              app.install(new HandlebarsModule(TestUtil.userdir("src/test/resources/htmx")));
              app.install(new HibernateValidatorModule());

              app.mvc(new TaskUIHtmx_(new TaskRepo3936()));
            })
        .ready(
            http -> {
              // 1. Index page loads normally
              http.get(
                  "/",
                  rsp -> {
                    assertEquals(200, rsp.code());
                  });

              // No header => 406
              http.header("Content-Type", "application/x-www-form-urlencoded");
              http.post(
                  "/tasks",
                  new FormBody.Builder().add("title", "Buy groceries").build(),
                  rsp -> {
                    assertEquals(406, rsp.code());
                    assertThat(rsp.body().string())
                        .containsIgnoringWhitespaces(
                            """
                            <h2>message: Direct browser access to this HTMX fragment is not allowed.</h2>
                            <h2>status code: 406</h2>
                            """);
                  });

              // 2. Add Task - Success Path
              http.header("Content-Type", "application/x-www-form-urlencoded");
              http.header("Hx-Request", "true");
              http.post(
                  "/tasks",
                  new FormBody.Builder().add("title", "Buy groceries").build(),
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("taskAdded", rsp.header("HX-Trigger"));

                    String body = rsp.body().string();
                    assertThat(body)
                        .containsIgnoringWhitespaces(
                            """
                            <div class="text-lg font-medium text-gray-700">
                              Buy groceries
                            </div>
                            """)
                        .containsIgnoringWhitespaces(
                            """
                            1 Tasks Remaining
                            """)
                        .containsIgnoringWhitespaces(
                            """
                            <span class="font-medium">Task added successfully!</span>
                            """);
                  });

              // 3.a simulate a network error => 500 response with Htmx error handler
              http.header("Content-Type", "application/x-www-form-urlencoded");
              http.header("Hx-Request", "true");
              http.post(
                  "/tasks",
                  new FormBody.Builder().add("title", "Wont save").build(),
                  rsp -> {
                    assertEquals(500, rsp.code());

                    String body = rsp.body().string();
                    assertThat(body)
                        .containsIgnoringWhitespaces(
                            """
                            <div hx-swap-oob="beforeend:#toast-container">
                                <div class="bg-red-600 border-red-700 text-white px-6 py-3 rounded-lg shadow-xl border flex items-center justify-between"
                                     style="animation: fadeOutAndShrink 0.5s ease-out 3s forwards;"
                                     onanimationend="this.remove()">
                                    <span class="font-medium">Server Error: Connection error! Please try again.</span>
                                </div>
                            </div>
                            """);
                  });

              // 4. Load the initial board
              http.header("Hx-Request", "true");
              http.get(
                  "/tasks",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertThat(rsp.body().string())
                        .containsIgnoringWhitespaces(
                            """
                            <input type="hidden" name="taskIds" value="1">

                            <div class="text-lg font-medium text-gray-700">
                              Buy groceries
                            </div>
                            """);
                  });

              // 5. Add Task - Validation Error (Sad Path)
              // Should fail @Valid (e.g., title too short/blank) and return 422
              // as orchestrated by the @HxError class-level annotation
              http.header("Content-Type", "application/x-www-form-urlencoded");
              http.header("Hx-Request", "true");
              http.post(
                  "/tasks",
                  new FormBody.Builder().add("title", "a").build(),
                  rsp -> {
                    assertEquals(422, rsp.code());
                    assertThat(rsp.body().string())
                        .containsIgnoringWhitespaces(
                            """
                            <li>size must be between 3 and 25</li>
                            """);
                  });

              // 6. Delete a task
              // Returns an empty HtmxResponse but HTTP status should be 200 OK
              http.header("Hx-Request", "true");
              http.delete(
                  "/tasks/123",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertThat(rsp.body().string())
                        .containsIgnoringWhitespaces(
                            """
                            <span class="font-medium">Task deleted!</span>
                            """);
                  });

              // 7. Reorder tasks
              // Verifies passing a list of IDs via form parameters
              http.header("Content-Type", "application/x-www-form-urlencoded");
              http.header("Hx-Request", "true");
              http.post(
                  "/tasks/reorder",
                  new FormBody.Builder()
                      .add("taskIds", "3")
                      .add("taskIds", "1")
                      .add("taskIds", "2")
                      .build(),
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertThat(rsp.body().string())
                        .containsIgnoringWhitespaces(
                            """
                            <span class="font-medium">Board saved.</span>
                            """);
                  });
            });
  }
}
