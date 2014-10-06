package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Response.ContentNegotiation;
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
  public void handle(final Request request, final Response response) throws Exception {

    Object handler = request.getInstance(router.getDeclaringClass());

    List<Param> parameters = provider.parameters(router);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      args[i] = parameters.get(i).get(request, response);
    }

    final Object result = router.invoke(handler, args);

    Class<?> returnType = router.getReturnType();
    if (returnType == void.class || returnType == Void.class) {
      // move on!
      return;
    }
    // negotiate!
    List<MediaType> accept = request.accept();

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
    // and template annotiation is present.
    boolean htmlLike = accept.size() == 1 && accept.get(0).matches(MediaType.html) &&
        router.getAnnotation(Template.class) != null;
    Function<MediaType, ExSupplier<Object>> provider =
        (type) -> MediaType.html.equals(type) || htmlLike ? viewable : notViewable;

    ContentNegotiation negotiator = null;
    for (MediaType type : accept) {
      if (negotiator == null) {
        negotiator = response.when(type, provider.apply(type));
      } else {
        negotiator = negotiator.when(type, provider.apply(type));
      }
    }
    // enough, now send
    negotiator.send();
  }
}
