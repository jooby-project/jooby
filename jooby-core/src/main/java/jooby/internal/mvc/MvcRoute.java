package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Router;
import jooby.Viewable;
import jooby.fn.ExSupplier;
import jooby.mvc.Template;

class MvcRoute implements Router {

  private Method router;

  private ParamProvider provider;

  public MvcRoute(final Method router, final ParamProvider provider) {
    this.router = requireNonNull(router, "A router method is required.");
    this.provider = requireNonNull(provider, "The resolver is required.");
  }

  @Override
  public void handle(final Request req, final Response res) throws Exception {

    Object handler = req.getInstance(router.getDeclaringClass());

    List<Param> parameters = provider.parameters(router);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      args[i] = parameters.get(i).get(req, res);
    }

    final Object result = router.invoke(handler, args);

    Class<?> returnType = router.getReturnType();
    if (returnType == void.class || returnType == Void.class) {
      // ignore glob pattern
      if (!req.route().pattern().contains("*")) {
        res.status(HttpStatus.NO_CONTENT);
      }
      return;
    }
    res.status(HttpStatus.OK);

    // format!
    List<MediaType> accept = req.accept();

    ExSupplier<Object> viewable = () -> {
      if (result instanceof Viewable) {
        return result;
      }
      // default view name
      String defaultViewName = Optional.ofNullable(router.getAnnotation(Template.class))
          .map(template -> template.value().isEmpty() ? router.getName() : template.value())
          .orElse(router.getName());
      return Viewable.of(defaultViewName, result);
    };

    ExSupplier<Object> notViewable = () -> result;

    // viewable is apply when content type is text/html or accept header is size 1 matches text/html
    // and template annotation is present.
    boolean htmlLike = accept.size() == 1 && accept.get(0).matches(MediaType.html) &&
        router.getAnnotation(Template.class) != null;
    Function<MediaType, ExSupplier<Object>> provider =
        (type) -> MediaType.html.equals(type) || htmlLike ? viewable : notViewable;

    Response.Formatter formatter = res.format();

    // add formatters
    accept.forEach(type -> formatter.when(type, provider.apply(type)));
    // send it!
    formatter.send();
  }
}
