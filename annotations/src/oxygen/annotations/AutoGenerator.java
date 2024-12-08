/* (C) 2024 */
package oxygen.annotations;

import static oxygen.annotations.GenType.*;
import static oxygen.annotations.Utils.*;

import com.google.auto.service.*;
import com.palantir.javapoet.*;
import com.palantir.javapoet.TypeSpec.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;
import oxygen.annotations.Utils.BuilderVisitor;

/**
 * RecordGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.AutoGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoGenerator extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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
                TypeSpec res = null;
                if (annotation.value() == EventTypeG) res = genEventType(typeElement, roundEnv, annotation);
                if (res != null) {
                    writeTo(res, annotation.path(), typeElement, processingEnv);
                    return false;
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

    public TypeSpec genEventType(TypeElement element, RoundEnvironment roundEnv, AutoGen annotation) {
        Set<TypeElement> toMark = new HashSet<>();
        Builder builder =
                copyElementBuilder(element, getMetaName(element, annotation.className()), new BuilderVisitor() {
                    @Override
                    public void buildEnd(Builder builder, TypeElement element) {
                        if (element.getKind() == ElementKind.ENUM) {
                            toMark.add(element);
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
                                            "this.$1N=$1N",
                                            fieldElement.getSimpleName().toString());
                                    // formatter will resolve it  LoL
                                }
                            }
                            if (flag) {
                                toMark.add(element);
                                builder.addMethod(constructorBuilder.build());
                                builder.addMethod(MethodSpec.constructorBuilder()
                                        .addModifiers(Modifier.PUBLIC)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public Set<Modifier> getFieldModifiers(VariableElement element, Set<Modifier> modifiers) {
                        return getPublic(modifiers);
                    }
                });
        builder.addJavadoc(
                "Generate by AutoGenerator \nfrom $N \nwith $N",
                element.getQualifiedName().toString(),
                annotation.toString());
        if (!annotation.withB()) {
            MethodSpec.Builder mb = MethodSpec.methodBuilder("mark")
                    .returns(void.class)
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(getEventType(annotation), "event");
            for (TypeElement nmark : toMark) {
                if (nmark.getKind() == ElementKind.CLASS) {
                    mb.addStatement(
                            "event.mark($N.class)", nmark.getQualifiedName().toString());
                }
                if (nmark.getKind() == ElementKind.ENUM) {
                    for (Element enclosedElement : nmark.getEnclosedElements()) {
                        if (enclosedElement.getKind() == ElementKind.ENUM_CONSTANT) {
                            VariableElement constant = (VariableElement) enclosedElement;
                            mb.addStatement(
                                    "event.markEnum($N.$N)",
                                    nmark.getQualifiedName().toString(),
                                    constant.getSimpleName().toString());
                        }
                    }
                }
            }
            builder.addMethod(mb.build());
        }
        return builder.build();
    }

    public TypeName getEventType(AutoGen annotation) {
        String str = annotation.withS();
        if (str.isEmpty() || !str.contains(":")) return ClassName.get("oxygen.utils", "OEvent");
        String[] list = str.split(":");
        return ClassName.get(list[0], list[1]);
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
