import io.jooby.ResultHandler;
import io.jooby.rxjava3.Reactivex;

module io.jooby.rxjava3 {
  exports io.jooby.rxjava3;

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires io.reactivex.rxjava3;
  requires org.reactivestreams;

  provides ResultHandler with
      Reactivex;
}
