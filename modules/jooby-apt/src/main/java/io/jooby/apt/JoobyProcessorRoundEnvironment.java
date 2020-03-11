package io.jooby.apt;

import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    ElementScanner8<Set<Element>, TypeElement> scanner = new AnnotationSetScanner(result);

    for (Element element : rootElements)
      result = scanner.scan(element, a);

    return result;
  }

  // Could be written as a local class inside getElementsAnnotatedWith
  private class AnnotationSetScanner extends
      ElementScanningIncludingTypeParameters<Set<Element>, TypeElement> {
    // Insertion-order preserving set
    private Set<Element> annotatedElements = new LinkedHashSet<>();

    AnnotationSetScanner(Set<Element> defaultSet) {
      super(defaultSet);
    }

    @Override
    public Set<Element> scan(Element e, TypeElement annotation) {
      //System.out.println("\t\t" + e);
      for (AnnotationMirror annotMirror :  eltUtils.getAllAnnotationMirrors(e)) {
        if (annotation.equals(mirrorAsElement(annotMirror))) {
          annotatedElements.add(e);
          break;
        }
      }
      e.accept(this, annotation);
      return annotatedElements;
    }

  }

  private static abstract class ElementScanningIncludingTypeParameters<R, P>
      extends ElementScanner8<R, P> {

    protected ElementScanningIncludingTypeParameters(R defaultValue) {
      super(defaultValue);
    }

    @Override
    public R visitType(TypeElement e, P p) {
      // Type parameters are not considered to be enclosed by a type
      if (e.getSuperclass().getKind() == TypeKind.DECLARED) {
        //System.out.println(e + " <<<< " + e.getSuperclass());
        TypeElement superElement = (TypeElement) ((DeclaredType) e.getSuperclass()).asElement();
        List<Element> clonedElements = new ArrayList<>();
        for(Element enclosedElement : superElement.getEnclosedElements()) {
          if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getAnnotationMirrors().size() > 0) {
            Symbol.MethodSymbol methodSymbol = ((Symbol.MethodSymbol)enclosedElement).clone((Symbol.ClassSymbol)e);
            methodSymbol.appendAttributes( ((Symbol.MethodSymbol)enclosedElement).getAnnotationMirrors() );
            //System.out.println("\t\tEnclosing: " + methodSymbol.getEnclosingElement() + "::" + methodSymbol + " #" + methodSymbol.getAnnotationMirrors().size());
            methodSymbol.params = ((Symbol.MethodSymbol)enclosedElement).params;
            methodSymbol.extraParams = ((Symbol.MethodSymbol)enclosedElement).extraParams;
            methodSymbol.capturedLocals = ((Symbol.MethodSymbol)enclosedElement).capturedLocals;
            //Symbol.MethodSymbol w = (Symbol.MethodSymbol)enclosedElement;
            //System.out.println("\t\t\tEnclosed (orig):" + w.params);
            //System.out.println("\t\t\tEnclosed (copy):" + methodSymbol.params);
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
