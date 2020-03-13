/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JoobyProcessorRoundEnvironment {

  private final Elements eltUtils;
  private static Types typeUtils;
  private final Set<? extends Element> rootElements;

  JoobyProcessorRoundEnvironment(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
    this.rootElements = roundEnv.getRootElements();
    this.eltUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  /**
   * Returns the elements annotated with the given annotation type.
   * Only type elements <i>included</i> in this round of annotation
   * processing, or declarations of members, parameters, or type
   * parameters declared within those, are returned.  Included type
   * elements are {@linkplain #getRootElements specified
   * types} and any types nested within them.
   *
   * This implementation Jooby-specific and is based and supports
   * inherited MVC classes.
   *
   * @param a  annotation type being requested
   * @return the elements annotated with the given annotation type,
   * or an empty set if there are none
   */
  public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) {

    Set<Element> result = Collections.emptySet();
    ElementScanner8<Set<Element>, TypeElement> scanner = new JoobyAnnotationSetScanner(result);

    for (Element element : rootElements) {
      result = scanner.scan(element, a);
    }

    return result;
  }

  private class JoobyAnnotationSetScanner extends ElementScanner8<Set<Element>, TypeElement> {
    private Set<Element> annotatedElements = new LinkedHashSet<>();

    JoobyAnnotationSetScanner(Set<Element> defaultSet) {
      super(defaultSet);
    }

    @Override
    public Set<Element> scan(Element e, TypeElement annotation) {
      for (AnnotationMirror annotMirror :  eltUtils.getAllAnnotationMirrors(e)) {
        if (annotation.equals(mirrorAsElement(annotMirror))) {
          annotatedElements.add(e);
          break;
        }
      }
      e.accept(this, annotation);
      return annotatedElements;
    }

    @Override
    public Set<Element> visitType(TypeElement e, TypeElement p) {
      if (e.getSuperclass().getKind() == TypeKind.DECLARED) {
        javax.lang.model.element.TypeElement superElement = (javax.lang.model.element.TypeElement) ((DeclaredType) e.getSuperclass()).asElement();
        List<Element> superElements = new ArrayList<>();
        for (Element enclosedElement : superElement.getEnclosedElements()) {
          if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getAnnotationMirrors().size() > 0) {
            superElements.add(enclosedElement);
          }
        }
        scan(superElements, p);
      }
      return super.visitType(e, p);
    }
  }

  private Element mirrorAsElement(AnnotationMirror annotationMirror) {
    return annotationMirror.getAnnotationType().asElement();
  }

}
