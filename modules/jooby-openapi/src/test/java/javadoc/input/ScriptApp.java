/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import java.util.function.Predicate;

import io.jooby.Context;
import io.jooby.Jooby;

/** Script App. Some description. */
public class ScriptApp extends Jooby {
  {
    // Ignored
    /*
     * This is a static path. No parameters
     *
     * @return Request Path.
     */
    get(
        "/static",
        ctx -> {
          // Ignored.
          return ctx.getRequestPath();
          // Ignored
        });

    // Ignored
    /*
     * Path param.
     *
     * @param id Path ID.
     * @return Some value.
     */
    // Ignored
    get("/{id}", Context::getRequestPath);

    /**
     * Delete something.
     *
     * @param id ID to delete.
     */
    delete("/{id}", Context::getRequestPath);

    /*
     * Tree summary.
     *
     * Tree doc.
     *
     * @tag Tree
     */
    path(
        "/tree",
        () -> {
          /*
           * Item doc.
           */
          get("/folder/{id}", Context::getRequestPath);

          /*
           * Items.
           */
          get("/folder", Context::getRequestPath);

          path(
              "/file",
              () -> {
                /*
                 * Sub Items.
                 */
                get("/{fileId}", Context::getRequestPath);
              });
          mount(
              Predicate.isEqual("/folder/{folderId}"),
              () -> {
                /*
                 * Mounted.
                 */
                get("/mount", Context::getRequestPath);
              });
        });

    routes(
        () -> {
          /*
           * Routes.
           */
          post("/routes", Context::getRequestPath);
          path(
              "/nested",
              () -> {
                /*
                 * Last.
                 */
                get("/last", Context::getRequestPath);
              });
        });
  }
}
