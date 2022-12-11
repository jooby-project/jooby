import io.jooby.ResultHandler;
import io.jooby.mutiny.Mutiny;

module io.jooby.mutiny {
  exports io.jooby.mutiny;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires io.smallrye.mutiny;

  provides ResultHandler with
      Mutiny;
}
