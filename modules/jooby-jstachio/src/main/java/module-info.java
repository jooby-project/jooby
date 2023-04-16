/**
 * See {@link io.jooby.jstachio.JStachioModule}
 *
 * @author agentgt
 * @see io.jooby.jstachio.JStachioModule
 */
module io.jooby.jstachio {
  requires transitive io.jstach.jstachio;
  requires transitive io.jooby;
  requires jakarta.inject;
  requires static com.github.spotbugs.annotations;

  exports io.jooby.jstachio;

  uses io.jstach.jstachio.spi.JStachioExtension;
}
