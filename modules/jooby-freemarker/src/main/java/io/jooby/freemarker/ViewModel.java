package io.jooby.freemarker;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import java.util.Map;

class ViewModel extends SimpleHash {
  private final TemplateHashModel root;

  public ViewModel(ObjectWrapper wrapper, Map<String, Object> attributes, TemplateHashModel root) {
    super(attributes, wrapper);
    this.root = root;
  }

  @Override protected Map copyMap(Map map) {
    // Copy OFF
    return map;
  }

  @Override public TemplateModel get(String key) throws TemplateModelException {
    TemplateModel model = root.get(key);
    if (model == null) {
      return super.get(key);
    }
    return model;
  }
}
