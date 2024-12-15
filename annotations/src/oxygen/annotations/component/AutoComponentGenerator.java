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
import com.github.javaparser.ast.nodeTypes.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.*;
import com.google.auto.service.*;
import com.google.gson.reflect.*;
import java.lang.annotation.*;
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
    public boolean withAnnotation(Class<? extends Annotation> clazz,AnnotationMirror mirror){
        return  mirror.getAnnotationType().toString().equals(clazz.getCanonicalName());
    }
    public void work(AutoComponent annotation, Element element,ClassOrInterfaceDeclaration classDecl, CompilationUnit compilationUnit){
        forEachAnnotations(element, classDecl, (mirror, expr) -> {
            Element annotationElement = mirror.getAnnotationType().asElement();
            if (annotationElement.getAnnotation(NoCopy.class) != null) {
                if(withAnnotation(With.class, mirror)){
                    With anno=element.getAnnotation(With.class);
                    if(anno==null){
                        throw new ArcRuntimeException("with is not found");
                    }

                }
                expr.remove();
            }
        });
    }
    public void resolve(AutoComponent annotation, TypeElement element, CompilationUnit compilationUnit)
            throws Exception {
        ClassOrInterfaceDeclaration classDecl = compilationUnit
                .getClassByName(element.getSimpleName().toString())
                .orElseThrow(NoSuchElementException::new);
        work(annotation, element, classDecl, compilationUnit);
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (isTypeElement(enclosedElement)) {
                resolve(annotation, (TypeElement) enclosedElement, compilationUnit);
            }else{
                work(annotation, element, classDecl, compilationUnit);
            }
        }
    }

    public boolean context(String context, AutoComponent annotation, TypeElement element) throws Exception {
        CompilationUnit compilationUnit = StaticJavaParser.parse(context);
        String origin = processingEnv.getElementUtils().getPackageOf(element).toString();
        String oriName = element.getSimpleName().toString();
        String packageName = getMetaPath(origin, annotation.path());
        String metaName = getMetaName(element, annotation.className());
        compilationUnit.setPackageDeclaration(new PackageDeclaration().setName(packageName));
        compilationUnit.addImport(origin, false, true);
        compilationUnit.findAll(Type.class, t -> t.toString().equals(oriName)).forEach(t -> {
            if (t instanceof ClassOrInterfaceType cit) {
                cit.setName(metaName);
            }
        });
        compilationUnit
                .findAll(ConstructorDeclaration.class, con -> con.getNameAsString()
                        .equals(oriName))
                .forEach(con -> {
                    con.setName(metaName);
                });
        ClassOrInterfaceDeclaration classDecl = compilationUnit
                .getClassByName(element.getSimpleName().toString())
                .orElseThrow(NoSuchElementException::new);
        classDecl.setName(metaName);
        resolve(annotation, element, compilationUnit);
        classDecl.removeJavaDocComment();
        classDecl.setComment(new JavadocComment(
                "Generate by AutoGenerator \n from" + element.getQualifiedName().toString()
                        + "\n with" + annotation.toString()
                        + "\n configure:" + gson.toJson(configure)));
        for (ComponentType type : annotation.value()) {
            if (type == Abstract) {}
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
            info(getContext(annotation.configure()));
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
