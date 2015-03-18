package org.jooby.internal.hbs;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.jknack.handlebars.Handlebars;

public class HbsHelpers {

  @Inject
  public HbsHelpers(final Handlebars hbs, @Named("hbs.helpers") final Set<Object> helpers) {
    helpers.forEach(h -> hbs.registerHelpers(h));
  }
}
