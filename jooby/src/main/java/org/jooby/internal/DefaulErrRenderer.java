package org.jooby.internal;

import java.util.Arrays;
import java.util.Map;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

public class DefaulErrRenderer implements Renderer {

  @SuppressWarnings("unchecked")
  @Override
  public void render(final Object object, final Context ctx) throws Exception {
    if (object instanceof View) {
      View view = (View) object;
      // assume it is the default error handler
      if (Err.DefHandler.VIEW.equals(view.name())) {
        Map<String, Object> model = (Map<String, Object>) view.model().get("err");
        Object status = model.get("status");
        Object reason = model.get("reason");
        Object message = model.get("message");
        String[] stacktrace = (String[]) model.get("stacktrace");

        StringBuilder html = new StringBuilder("<!doctype html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<meta charset=\"").append(ctx.charset().name()).append("\">\n")
            .append("<style>\n")
            .append("body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n")
            .append("h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n")
            .append("h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n")
            .append("footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n")
            .append("hr {background-color: #f7f7f9;}\n")
            .append("div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n")
            .append("p {padding-left: 20px;}\n")
            .append("p.tab {padding-left: 40px;}\n")
            .append("</style>\n")
            .append("<title>\n")
            .append(status).append(" ").append(reason)
            .append("\n</title>\n")
            .append("<body>\n")
            .append("<h1>").append(reason).append("</h1>\n")
            .append("<hr>");

        html.append("<h2>message: ").append(message).append("</h2>\n");
        html.append("<h2>status: ").append(status).append("</h2>\n");

        if (stacktrace != null) {
          html.append("<h2>stack:</h2>\n")
              .append("<div class=\"trace\">\n");

          Arrays.stream(stacktrace).forEach(line -> {
            html.append("<p class=\"line");
            if (line.startsWith("\t")) {
              html.append(" tab");
            }
            html.append("\">")
                .append("<code>")
                .append(line.replace("\t", "  "))
                .append("</code>")
                .append("</p>\n");
          });
          html.append("</div>\n");
        }

        html.append("</body>\n")
            .append("</html>\n");

        ctx.type(MediaType.html)
            .send(html.toString());
      }
    }

  }

  @Override
  public String toString() {
    return "default.err";
  }

}
