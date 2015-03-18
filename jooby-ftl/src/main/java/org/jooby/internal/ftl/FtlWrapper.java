package org.jooby.internal.ftl;

import java.util.Map;

import org.jooby.Locals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import freemarker.template.DefaultMapAdapter;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

public class FtlWrapper implements ObjectWrapper {

  private ObjectWrapper wrapper;

  public FtlWrapper(final ObjectWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public TemplateModel wrap(final Object obj) throws TemplateModelException {
    if (obj instanceof Config) {
      ConfigObject config = ((Config) obj).root();
      return DefaultMapAdapter.adapt(config.unwrapped(), (ObjectWrapperWithAPISupport) wrapper);
    }
    if (obj instanceof Locals) {
      Map<String, Object> locals = ((Locals) obj).attributes();
      return DefaultMapAdapter.adapt(locals, (ObjectWrapperWithAPISupport) wrapper);
    }
    return wrapper.wrap(obj);
  }

}
