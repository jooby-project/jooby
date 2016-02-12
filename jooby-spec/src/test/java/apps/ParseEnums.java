package apps;

import org.jooby.Jooby;

import apps.model.Event.Frequency;

public class ParseEnums extends Jooby {

  {
    get("/", req -> {
      Frequency freq = Frequency.valueOf(req.param("freq").value().toUpperCase());

      return freq;
    });

  }
}
