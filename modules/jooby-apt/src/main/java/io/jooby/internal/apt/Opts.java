/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import javax.annotation.processing.ProcessingEnvironment;

public interface Opts {

  String OPT_DEBUG = "jooby.debug";
  String OPT_INCREMENTAL = "jooby.incremental";
  String OPT_SERVICES = "jooby.services";
  String OPT_SKIP_ATTRIBUTE_ANNOTATIONS = "jooby.skipAttributeAnnotations";

  static boolean boolOpt(ProcessingEnvironment processingEnvironment, String option, boolean defaultValue) {
    return Boolean.parseBoolean(processingEnvironment
        .getOptions().getOrDefault(option, String.valueOf(defaultValue)));
  }

  static String[] stringListOpt(ProcessingEnvironment processingEnvironment, String option, String defaultValue) {
    String value = processingEnvironment.getOptions().getOrDefault(option, defaultValue);
    return value == null || value.isEmpty() ? new String[0] : value.split(",");
  }
}
