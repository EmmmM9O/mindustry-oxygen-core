/* (C) 2024 */
package oxygen.annotations.generator;

import static oxygen.annotations.Utils.*;
import static oxygen.annotations.generator.GenType.*;

import arc.util.*;
import com.google.auto.service.*;
import com.palantir.javapoet.*;
import com.palantir.javapoet.TypeSpec.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import oxygen.annotations.*;
import oxygen.annotations.Utils.BuilderVisitor;

/**
 * RecordGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.generator.AutoGen")
public class AutoGenerator extends BaseProcessor {

  @Override
  public boolean process(RoundEnvironment roundEnv) throws Exception {
    Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AutoGen.class);
    info("Auto Generate with " + annotatedElements.toString());
    for (Element element : annotatedElements) {
      AutoGen annotation = element.getAnnotation(AutoGen.class);
      if (isTypeElement(element)) {
        TypeElement typeElement = ((TypeElement) element);
        if (!isOuterClass(typeElement))
          throw new ArcRuntimeException(
              "TypeElement must be outer " + annotation.toString() + " " + element.toString());
        TypeSpec res = null;
        if (annotation.value() == EventTypeG)
          res = genEventType(typeElement, roundEnv, annotation);
        if (annotation.value() == TexSG)
          res = genTex(typeElement, roundEnv, annotation);
        if (res != null) {
          writeTo(res, annotation.path(), typeElement);
          return false;
        }
        throw new ArcRuntimeException(
            "TypeElement not resolve with " + annotation.toString() + " " + element.toString());
      }
    }
    return false;
  }

  public TypeSpec genTex(TypeElement element, RoundEnvironment roundEnv, AutoGen annotation) {
    var builder =
        TypeSpec.classBuilder(annotation.className().isEmpty() ? "Tex" : annotation.className())
            .addModifiers(Modifier.PUBLIC);
    var loadStyles =
        MethodSpec.methodBuilder("loadStyles").addModifiers(Modifier.STATIC, Modifier.PUBLIC);
    var load = MethodSpec.methodBuilder("load").addModifiers(Modifier.STATIC, Modifier.PUBLIC);
    for (var e : element.getEnclosedElements()) {
      if (e.getKind() == ElementKind.FIELD) {
        var field = (VariableElement) e;
        var name = field.getSimpleName().toString();
        if (name.startsWith("default")) {
          loadStyles.addStatement("arc.Core.scene.addStyle(" + field.asType().toString() + ".class,"
              + element.asType().toString() + "." + name + ")");
        }
      }
    }
    sub(rootDirectory, (annotation.withS().isEmpty() ? "/assets/sprites/ui" : annotation.withS()))
        .walk(p -> {
          if (!p.extEquals("png"))
            return;
          var name = p.name();
          name = name.substring(0, name.indexOf("."));
          var dtype = "arc.scene.style.Drawable";
          var varname = capitalize(name);
          builder.addField(ClassName.bestGuess(dtype), varname, Modifier.STATIC, Modifier.PUBLIC);
          load.addStatement(varname + " = arc.Core.atlas.drawable($S)", annotation.withS2() + name);
          Log.info("add sprite @", name);
        });

    builder.addMethod(loadStyles.build()).addMethod(load.build());
    return builder.build();
  }

  public TypeSpec genEventType(TypeElement element, RoundEnvironment roundEnv, AutoGen annotation) {
    var toMark = new HashSet<TypeElement>();
    var builder = copyElementBuilder(element, getMetaName(element, annotation.className()),
        new BuilderVisitor() {
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
                  constructorBuilder.addParameter(TypeName.get(fieldElement.asType()),
                      fieldElement.getSimpleName().toString());
                  constructorBuilder.addStatement("this.$1N=$1N",
                      fieldElement.getSimpleName().toString());
                  // formatter will resolve it LoL
                }
              }
              if (flag) {
                toMark.add(element);
                builder.addMethod(constructorBuilder.build());
                builder.addMethod(
                    MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
              }
            }
          }

          @Override
          public Set<Modifier> getFieldModifiers(VariableElement element, Set<Modifier> modifiers) {
            return getPublic(modifiers);
          }
        });
    builder.addJavadoc("Generate by AutoGenerator \nfrom $N \nwith $N",
        element.getQualifiedName().toString(), annotation.toString());
    if (!annotation.withB()) {
      MethodSpec.Builder mb = MethodSpec.methodBuilder("mark").returns(void.class)
          .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
          .addParameter(getEventType(annotation), "event");
      for (TypeElement nmark : toMark) {
        if (nmark.getKind() == ElementKind.CLASS) {
          mb.addStatement("event.mark($N.class)", nmark.getQualifiedName().toString());
        }
        if (nmark.getKind() == ElementKind.ENUM) {
          for (Element enclosedElement : nmark.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.ENUM_CONSTANT) {
              VariableElement constant = (VariableElement) enclosedElement;
              mb.addStatement("event.markEnum($N.$N)", nmark.getQualifiedName().toString(),
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
    if (str.isEmpty() || !str.contains(":"))
      return ClassName.get("oxygen.utils", "OEvent");
    String[] list = str.split(":");
    return ClassName.get(list[0], list[1]);
  }

  public Set<Modifier> getPublic(Set<Modifier> omodifiers) {
    Set<Modifier> modifiers = new HashSet<Modifier>(omodifiers);
    if (modifiers.contains(Modifier.PRIVATE))
      modifiers.remove(Modifier.PRIVATE);
    if (modifiers.contains(Modifier.PROTECTED))
      modifiers.remove(Modifier.PROTECTED);
    if (modifiers.contains(Modifier.FINAL))
      modifiers.remove(Modifier.FINAL);
    if (!modifiers.contains(Modifier.PUBLIC))
      modifiers.add(Modifier.PUBLIC);
    return modifiers;
  }
}
