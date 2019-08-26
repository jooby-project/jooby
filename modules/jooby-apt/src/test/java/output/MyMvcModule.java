package output;

import io.jooby.Extension;
import io.jooby.MvcModule;
import source.Routes;

import javax.inject.Provider;

public class MyMvcModule implements MvcModule {
  @Override public boolean supports(Class type) {
    return type == Routes.class;
  }

  @Override public Extension create(Provider provider) {
    return new MvcExtension(provider);
  }

}
