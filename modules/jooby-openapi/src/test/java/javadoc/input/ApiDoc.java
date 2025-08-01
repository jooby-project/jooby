/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

/**
 * Api summary.
 *
 * <p>Proin sit amet lectus interdum, porta libero quis, fringilla metus. Integer viverra ante id
 * vestibulum congue. Nam et tortor at magna tempor congue.
 *
 * @x-badges.name Beta
 * @x-badges.position before
 * @x-badges.color purple
 * @tag ApiTag
 */
@Path("/api")
public class ApiDoc {

  /**
   * This is the Hello <code>/endpoint</code>
   *
   * <p>Operation description
   *
   * @param name Person name.
   * @param age Person age. Multi line doc.
   * @param list This line has a break.
   * @param str Some <code>string</code>.
   * @return Welcome message <code>200</code>.
   * @throws NullPointerException One something is null.
   */
  @NonNull @GET
  public String hello(
      @QueryParam List<java.util.List<java.lang.String>> name,
      @QueryParam int age,
      @QueryParam List<java.lang.String> list,
      @QueryParam java.lang.String str) {
    return "hello";
  }

  /**
   * Search database.
   *
   * <p>Search DB
   *
   * @param query
   * @return
   */
  @GET
  public String search(@QueryParam QueryBeanDoc query) {
    return "hello";
  }

  /**
   * Record database.
   *
   * @param query
   * @return
   */
  @GET
  public String recordBean(@QueryParam RecordBeanDoc query) {
    return "hello";
  }

  /**
   * Enum database.
   *
   * @param query Enum doc.
   * @return
   */
  @GET
  public String enumParam(@QueryParam EnumDoc query) {
    return "hello";
  }
}
