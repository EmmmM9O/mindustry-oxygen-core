/* (C) 2024 */
package oxygen.annotations.component;

import static oxygen.annotations.Utils.*;
import static oxygen.annotations.component.ComponentType.*;

import arc.struct.*;
import arc.util.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.*;
import com.github.javaparser.ast.expr.*;
import com.google.auto.service.*;
import com.google.gson.reflect.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.annotation.processing.Processor;
import javax.lang.model.element.*;
import javax.tools.*;
import oxygen.annotations.*;
import oxygen.annotations.mark.*;

/*
 * AutoComponentGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.component.AutoComponent")
public class AutoComponentGenerator extends BaseProcessor {
    Map<String, Object> configure;

    public boolean context(String context, AutoComponent annotation, TypeElement element) throws Exception {
        CompilationUnit compilationUnit = StaticJavaParser.parse(context);
        String origin = processingEnv.getElementUtils().getPackageOf(element).toString();
        String packageName = getMetaPath(origin, annotation.path());
        String metaName = getMetaName(element, annotation.className());
        compilationUnit.setPackageDeclaration(new PackageDeclaration().setName(packageName));
        compilationUnit.addImport(origin, false, true);
        ClassOrInterfaceDeclaration classDecl = compilationUnit
                .getClassByName(element.getSimpleName().toString())
                .orElseThrow(NoSuchElementException::new);
        classDecl.setName(metaName);
        Set<String> noCopyAnnotations = new HashSet<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            Element aElement = mirror.getAnnotationType().asElement();
            if (aElement.getAnnotation(NoCopy.class) != null) {
                noCopyAnnotations.add(aElement.getSimpleName().toString());
            }
        }
        for (String noCopy : noCopyAnnotations) {
            AnnotationExpr expr = classDecl.getAnnotationByName(noCopy).orElseThrow(NoSuchElementException::new);
            expr.remove();
        }
        classDecl.removeJavaDocComment();
        classDecl.setComment(new JavadocComment(
                "Generate by AutoGenerator \n from" + element.getQualifiedName().toString()
                        + "\n with" + annotation.toString()
                        + "\n configure:" + formatJson(annotation.configure())));
        for (ComponentType type : annotation.value()) {
            for (Element enclosedElement : element.getEnclosedElements()) {}
        }

        writeTo(compilationUnit.toString(), metaName, packageName, element, null);
        return true;
    }

    @Override
    public boolean process(RoundEnvironment roundEnv) throws Exception {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AutoComponent.class);
        processingEnv
                .getMessager()
                .printMessage(Diagnostic.Kind.NOTE, "Auto Component with " + annotatedElements.toString());
        for (Element element : annotatedElements) {
            AutoComponent annotation = element.getAnnotation(AutoComponent.class);
            configure = gson.fromJson(
                    getContext(annotation.configure()), new TypeToken<Map<String, Object>>() {}.getType());

            if (isTypeElement(element)) {
                TypeElement typeElement = ((TypeElement) element);
                String context = fromSource(typeElement).readString();
                if (context(context, annotation, typeElement)) return true;
                if (!isOuterClass(typeElement))
                    throw new ArcRuntimeException(
                            "TypeElement must be outer " + annotation.toString() + " " + element.toString());
            }
            throw new ArcRuntimeException(
                    "TypeElement not resolve with " + annotation.toString() + " " + element.toString());
        }
        return false;
    }
}
