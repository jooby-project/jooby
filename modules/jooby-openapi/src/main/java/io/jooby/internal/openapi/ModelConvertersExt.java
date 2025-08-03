/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.lang.reflect.Type;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.core.converter.*;
import io.swagger.v3.core.util.ReferenceTypeUtils;
import io.swagger.v3.oas.models.media.Schema;

public class ModelConvertersExt extends ModelConverters {

  /** Copy of {@link ModelConverterContextImpl} required for access to schemas by class name. */
  private static class ModelConverterContextExt implements ModelConverterContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelConverterContextExt.class);

    private final List<ModelConverter> converters;
    private final Map<String, Schema> modelByName;
    private final HashMap<AnnotatedType, Schema> modelByType;
    private final Set<AnnotatedType> processedTypes;

    public ModelConverterContextExt(List<ModelConverter> converters) {
      this.converters = converters;
      modelByName = new TreeMap<>();
      modelByType = new HashMap<>();
      processedTypes = new HashSet<>();
    }

    public ModelConverterContextExt(ModelConverter converter) {
      this(new ArrayList<ModelConverter>());
      converters.add(converter);
    }

    @Override
    public Iterator<ModelConverter> getConverters() {
      return converters.iterator();
    }

    @Override
    public void defineModel(String name, Schema model) {
      AnnotatedType aType = null;
      defineModel(name, model, aType, null);
    }

    @Override
    public void defineModel(String name, Schema model, Type type, String prevName) {
      defineModel(name, model, new AnnotatedType().type(type), prevName);
    }

    @Override
    public void defineModel(String name, Schema model, AnnotatedType type, String prevName) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(String.format("defineModel %s %s", name, model));
      }
      modelByName.put(name, model);

      if (prevName != null && !prevName.isBlank() && !prevName.equals(name)) {
        modelByName.remove(prevName);
      }

      if (type != null && type.getType() != null) {
        modelByType.put(type, model);
      }
    }

    @Override
    public Map<String, Schema> getDefinedModels() {
      return Collections.unmodifiableMap(modelByName);
    }

    @Override
    public Schema resolve(AnnotatedType type) {
      AnnotatedType aType = ReferenceTypeUtils.unwrapReference(type);
      if (aType != null) {
        return resolve(aType);
      }

      if (processedTypes.contains(type)) {
        return modelByType.get(type);
      } else {
        processedTypes.add(type);
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("resolve %s", type.getType()));
      }
      Iterator<ModelConverter> converters = this.getConverters();
      Schema resolved = null;
      if (converters.hasNext()) {
        ModelConverter converter = converters.next();
        LOGGER.trace("trying extension {}", converter);
        resolved = converter.resolve(type, this, converters);
      }
      if (resolved != null) {
        modelByType.put(type, resolved);

        Schema resolvedImpl = resolved;
        if (resolvedImpl.getName() != null) {
          modelByName.put(resolvedImpl.getName(), resolved);
        }
      } else {
        processedTypes.remove(type);
      }

      return resolved;
    }
  }

  public ModelConvertersExt() {
    super(false);
  }

  @Override
  public ResolvedSchemaExt readAllAsResolvedSchema(Type type) {
    return (ResolvedSchemaExt) super.readAllAsResolvedSchema(type);
  }

  @Override
  public ResolvedSchemaExt readAllAsResolvedSchema(AnnotatedType type) {
    return (ResolvedSchemaExt) super.readAllAsResolvedSchema(type);
  }

  @Override
  public ResolvedSchemaExt resolveAsResolvedSchema(AnnotatedType type) {
    var context = new ModelConverterContextExt(getConverters());
    var resolvedSchema = new ResolvedSchemaExt();
    resolvedSchema.schema = context.resolve(type);
    resolvedSchema.referencedSchemas = context.getDefinedModels();
    resolvedSchema.referencedSchemasByType = new HashMap<>();
    context.modelByType.forEach(
        (annotatedType, schema) ->
            resolvedSchema.referencedSchemasByType.put(annotatedType.getType(), schema));
    return resolvedSchema;
  }
}
