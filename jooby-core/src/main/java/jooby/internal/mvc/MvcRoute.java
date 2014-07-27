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
import jooby.Route;
import jooby.Viewable;
import jooby.mvc.Template;
import net.sf.cglib.reflect.FastMethod;

class MvcRoute implements Route {

  private FastMethod route;

  private ParamProvider provider;

  public MvcRoute(final FastMethod route, final ParamProvider provider) {
    this.route = requireNonNull(route, "The route is required.");
    this.provider = requireNonNull(provider, "The resolver is required.");
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    Method method = route.getJavaMethod();

    Object handler = request.getInstance(method.getDeclaringClass());

    List<Param> parameters = provider.parameters(method);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      args[i] = parameters.get(i).get(request);
    }

    final Object result = route.invoke(handler, args);

    // negotiate!
    List<MediaType> accept = request.accept();

    ContentNegotiation.Provider viewable = () -> {
      if (result instanceof Viewable) {
        return result;
      }
      // default view name
      String defaultViewName = Optional.ofNullable(method.getAnnotation(Template.class))
          .map(template -> template.value().isEmpty() ? method.getName() : template.value())
          .orElse(method.getName());
      return Viewable.of(defaultViewName, result);
    };

    ContentNegotiation.Provider notViewable = () -> result;

    // viewable is apply when content type is text/html or accept header is size 1 matches text/html
    // and template annotiation is present.
    boolean htmlLike = accept.size() == 1 && accept.get(0).matches(MediaType.html) &&
        method.getAnnotation(Template.class) != null;
    Function<MediaType, ContentNegotiation.Provider> provider =
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
