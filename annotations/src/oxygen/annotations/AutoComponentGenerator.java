/* (C) 2024 */
package oxygen.annotations;

import static oxygen.annotations.ComponentType.*;
import static oxygen.annotations.Utils.*;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.google.auto.service.*;
import com.google.gson.reflect.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.annotation.processing.Processor;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

/*
 * AutoComponentGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.AutoComponent")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoComponentGenerator extends AbstractProcessor {
    Map<String, Object> configure;

    public boolean context(String context, AutoComponent annotation, TypeElement element) throws Throwable{
        CompilationUnit compilationUnit = StaticJavaParser.parse(context);
        String origin=processingEnv.getElementUtils().getPackageOf(element).toString();
        String packageName = getMetaPath(
                origin , annotation.path());
        String metaName=
                getMetaName(element, annotation.className());
        compilationUnit.setPackageDeclaration(new PackageDeclaration().setName(packageName));
        compilationUnit.addImport(origin,false,true);
        ClassOrInterfaceDeclaration classDecl = compilationUnit.getClassByName(element.getSimpleName().toString()).orElseThrow(NoSuchElementException::new);
        classDecl.setName(metaName);
        for (ComponentType type : annotation.value()) {}
        writeTo(
                compilationUnit.toString(),
                metaName,
                packageName,
                element,
                null,
                processingEnv);
        return true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> sets, RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AutoComponent.class);
        processingEnv
                .getMessager()
                .printMessage(Diagnostic.Kind.NOTE, "Auto Component with " + annotatedElements.toString());
        for (Element element : annotatedElements) {
            AutoComponent annotation = element.getAnnotation(AutoComponent.class);
            configure = gson.fromJson(annotation.configure(), new TypeToken<Map<String, Object>>() {}.getType());

            if (isTypeElement(element)) {
                TypeElement typeElement = ((TypeElement) element);
                if (!isOuterClass(typeElement))
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "TypeElement must be outer " + annotation.toString() + " " + element.toString());
                FileObject source;
                try {
                    source = fromSource(typeElement, processingEnv);
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(source.openInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder fileContent = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            fileContent.append(line).append("\n");
                        }
                        if (context(fileContent.toString(), annotation, typeElement)) return false;
                    }
                } catch (Throwable err) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "error " + annotation.toString() + " " + element.toString() + " " + err.toString());
                }
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "TypeElement not resolve with " + annotation.toString() + " " + element.toString());
            }
        }
        return false;
    }
}
