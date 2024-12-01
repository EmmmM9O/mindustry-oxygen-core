/* (C) 2024 */
package oxygen.annotations;

import static oxygen.annotations.GenType.*;

import com.google.auto.service.*;
import com.palantir.javaformat.java.*;
import com.palantir.javapoet.*;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

/**
 * RecordGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.AutoGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoGenerator extends AbstractProcessor {
    public boolean isTypeElement(Element element) {
        return element.getKind() == ElementKind.CLASS
                || element.getKind() == ElementKind.INTERFACE
                || element.getKind() == ElementKind.ANNOTATION_TYPE
                || element.getKind() == ElementKind.ENUM;
    }

    public static JavaFormatterOptions option = JavaFormatterOptions.builder()
            .style(JavaFormatterOptions.Style.PALANTIR)
            .build();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // if (roundEnv.processingOver()) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AutoGen.class);
        processingEnv
                .getMessager()
                .printMessage(Diagnostic.Kind.NOTE, "Auto Generate with " + annotatedElements.toString());
        for (Element element : annotatedElements) {
            AutoGen annotation = element.getAnnotation(AutoGen.class);
            if (isTypeElement(element)) {
                TypeElement typeElement = ((TypeElement) element);
                if (!isOuterClass(typeElement))
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "TypeElement must be outer " + annotation.toString() + " " + element.toString());
                String packageName = annotation.path().isEmpty()
                        ? processingEnv
                                .getElementUtils()
                                .getPackageOf(typeElement)
                                .toString()
                        : annotation.path();
                TypeSpec res = null;
                if (annotation.value() == EventTypeG) res = genEventType(typeElement, roundEnv, annotation);
                if (res != null) {
                    try {
                        String formatted = Formatter.createFormatter(option)
                                .formatSource(JavaFile.builder(packageName, res)
                                        .build()
                                        .toString());
                        String fileName = packageName.isEmpty() ? res.name() : packageName + "." + res.name();
                        Filer filer = processingEnv.getFiler();
                        List<Element> originatingElements = res.originatingElements();
                        JavaFileObject filerSourceFile = filer.createSourceFile(
                                fileName, originatingElements.toArray(new Element[originatingElements.size()]));
                        try (Writer writer = filerSourceFile.openWriter()) {
                            writer.write(formatted);
                        } catch (Exception e) {
                            try {
                                filerSourceFile.delete();
                            } catch (Exception ignored) {
                            }
                            throw e;
                        }

                    } catch (Throwable e) {
                        processingEnv
                                .getMessager()
                                .printMessage(Diagnostic.Kind.ERROR, "Error generating file: " + e.getMessage());
                    }
                    return false;
                }
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "TypeElement not resolve with " + annotation.toString() + " " + element.toString());
            }
        }
        // }
        return false;
    }

    public boolean isOuterClass(TypeElement typeElement) {
        return typeElement.getEnclosingElement() == null
                || typeElement.getEnclosingElement().getKind() == ElementKind.PACKAGE;
    }

    public static String[] suffixs = new String[] {"Comp", "Meta", "Gen"};

    public String getMetaName(TypeElement element, AutoGen annotation) {
        String name = element.getSimpleName().toString();
        if (!annotation.className().isEmpty()) return annotation.className().replace("$", name);
        for (String suffix : suffixs) {
            if (name.endsWith(suffix)) name = name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    public TypeSpec genEventType(TypeElement element, RoundEnvironment roundEnv, AutoGen annotation) {
        return genEventTypeN(element, roundEnv, annotation, getMetaName(element, annotation), builder -> {
            builder.addJavadoc(
                    "Generate by AutoGenerator \nfrom $N \nwith $N",
                    element.getQualifiedName().toString(),
                    annotation.toString());
        });
    }

    public TypeSpec genEventTypeN(
            TypeElement element,
            RoundEnvironment roundEnv,
            AutoGen annotation,
            String metaName,
            Cons<TypeSpec.Builder> cons) {
        TypeSpec.Builder builder = null;
        if (element.getKind() == ElementKind.CLASS) builder = TypeSpec.classBuilder(metaName);
        if (element.getKind() == ElementKind.ENUM) builder = TypeSpec.enumBuilder(metaName);
        if (element.getKind() == ElementKind.INTERFACE) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "EventType do not support interface " + annotation.toString() + " " + element.toString());
            return null;
        }
        for (Modifier modifier : getPublic(element.getModifiers())) builder.addModifiers(modifier);

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.ENUM_CONSTANT) {
                VariableElement constant = (VariableElement) enclosedElement;
                builder.addEnumConstant(constant.getSimpleName().toString());
            } else if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement fieldElement = (VariableElement) enclosedElement;
                FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                        TypeName.get(fieldElement.asType()),
                        fieldElement.getSimpleName().toString());
                for (Modifier modifier : getPublic(fieldElement.getModifiers())) fieldBuilder.addModifiers(modifier);
                builder.addField(fieldBuilder.build());
            } else if (isTypeElement(enclosedElement)) {
                TypeElement type = (TypeElement) enclosedElement;
                TypeSpec spec = genEventTypeN(
                        type, roundEnv, annotation, type.getSimpleName().toString(), b -> {});
                if (spec != null) {
                    builder.addType(spec);
                }
            }
        }

        if (element.getKind() == ElementKind.CLASS) {

            boolean flag = false;
            MethodSpec.Builder constructorBuilder =
                    MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            for (Element enclosedElement : element.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    flag = true;
                    VariableElement fieldElement = (VariableElement) enclosedElement;
                    constructorBuilder.addParameter(
                            TypeName.get(fieldElement.asType()),
                            fieldElement.getSimpleName().toString());
                    constructorBuilder.addStatement(
                            "this.$1N = $1N", fieldElement.getSimpleName().toString());
                    // formatter will resolve it  LoL
                }
            }
            if (flag) {
                builder.addMethod(constructorBuilder.build());
                builder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .build());
            }
        }
        cons.get(builder);
        return builder.build();
    }

    public Set<Modifier> getPublic(Set<Modifier> omodifiers) {
        Set<Modifier> modifiers = new HashSet<Modifier>(omodifiers);
        if (modifiers.contains(Modifier.PRIVATE)) modifiers.remove(Modifier.PRIVATE);
        if (modifiers.contains(Modifier.PROTECTED)) modifiers.remove(Modifier.PROTECTED);
        if (modifiers.contains(Modifier.FINAL)) modifiers.remove(Modifier.FINAL);
        if (!modifiers.contains(Modifier.PUBLIC)) modifiers.add(Modifier.PUBLIC);
        return modifiers;
    }
}
