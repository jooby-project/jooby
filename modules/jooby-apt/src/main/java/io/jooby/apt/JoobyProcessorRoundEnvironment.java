package io.jooby.apt;

import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

public class JoobyProcessorRoundEnvironment {

  private final Elements eltUtils;
  private static Types typeUtils;
  private final Set<? extends Element> rootElements;

  JoobyProcessorRoundEnvironment(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
    this.rootElements = roundEnv.getRootElements();
    this.eltUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) {

    Set<Element> result = Collections.emptySet();
    ElementScanner8<Set<Element>, TypeElement> scanner = new JoobyAnnotationSetScanner(result);

    for (Element element : rootElements)
      result = scanner.scan(element, a);

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
        List<Element> clonedElements = new ArrayList<>();
        for(Element enclosedElement : superElement.getEnclosedElements()) {
          if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getAnnotationMirrors().size() > 0) {
            Symbol.MethodSymbol methodSymbol = ((Symbol.MethodSymbol)enclosedElement).clone((Symbol.ClassSymbol)e);
            methodSymbol.appendAttributes( ((Symbol.MethodSymbol)enclosedElement).getAnnotationMirrors() );
            methodSymbol.params = ((Symbol.MethodSymbol)enclosedElement).params;
            methodSymbol.extraParams = ((Symbol.MethodSymbol)enclosedElement).extraParams;
            methodSymbol.capturedLocals = ((Symbol.MethodSymbol)enclosedElement).capturedLocals;
            clonedElements.add(methodSymbol);
          }
        }
        scan(clonedElements, p);
      }
      return super.visitType(e, p);
    }
  }

  private Element mirrorAsElement(AnnotationMirror annotationMirror) {
    return annotationMirror.getAnnotationType().asElement();
  }

}
