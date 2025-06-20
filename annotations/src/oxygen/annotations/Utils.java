/* (C) 2024 */
package oxygen.annotations;

import arc.files.*;
import arc.func.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.*;
import com.google.gson.*;
import com.palantir.javaformat.java.*;
import com.palantir.javaformat.java.Formatter;
import com.palantir.javapoet.*;
import java.lang.annotation.*;
import java.util.*;
import javax.lang.model.element.*;
import oxygen.annotations.mark.*;

/**
 * Utils
 */
public class Utils {
  public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public static boolean isTypeElement(ElementKind kind) {
    return kind == ElementKind.CLASS || kind == ElementKind.INTERFACE
        || kind == ElementKind.ANNOTATION_TYPE || kind == ElementKind.ENUM;
  }

  public static boolean isTypeElement(Element element) {
    return isTypeElement(element.getKind());
  }

  public static boolean isTypeUse(ElementKind kind) {
    return kind == ElementKind.PARAMETER || kind == ElementKind.LOCAL_VARIABLE
        || kind == ElementKind.TYPE_PARAMETER || kind == ElementKind.METHOD;
  }

  public static String getClassPath(TypeElement typeElement) {
    if (typeElement.getNestingKind().isNested()) {
      return getClassPath((TypeElement) (typeElement.getEnclosingElement())) + "$"
          + typeElement.getSimpleName().toString();
    }
    return typeElement.getQualifiedName().toString();
  }

  public static boolean isTypeUse(Element element) {
    return isTypeUse(element.getKind());
  }

  public static boolean isOuterClass(TypeElement typeElement) {
    return typeElement.getEnclosingElement() == null
        || typeElement.getEnclosingElement().getKind() == ElementKind.PACKAGE;
  }

  public static boolean isSame(ElementType type, ElementKind kind) {
    switch (type) {
      case METHOD:
        return kind == ElementKind.METHOD;
      case PACKAGE:
        return kind == ElementKind.PACKAGE;
      case FIELD:
        return kind == ElementKind.FIELD || kind == ElementKind.ENUM_CONSTANT;
      case ANNOTATION_TYPE:
        return kind == ElementKind.ANNOTATION_TYPE;
      case LOCAL_VARIABLE:
        return kind == ElementKind.LOCAL_VARIABLE;
      case CONSTRUCTOR:
        return kind == ElementKind.CONSTRUCTOR;
      case PARAMETER:
        return kind == ElementKind.PARAMETER;
      case TYPE_PARAMETER:
        return kind == ElementKind.TYPE_PARAMETER;
      case TYPE_USE:
        return isTypeUse(kind);
      case TYPE:
        return isTypeElement(kind);
      default:
        return false;
    }
  }

  public static String[] metaSuffixs = new String[] {"Comp", "Meta", "Gen"};

  public static String getMetaName(TypeElement element, String or) {
    String name = element.getSimpleName().toString();
    if (!or.isEmpty())
      return or;
    for (String suffix : metaSuffixs) {
      if (name.endsWith(suffix))
        name = name.substring(0, name.length() - suffix.length());
    }
    return name;
  }

  public static String[] pathSuffixs = new String[] {".comp", ".meta", ".gen"};

  public static String getMetaPath(String path, String or) {
    if (!or.isEmpty())
      return or;
    for (String suffix : metaSuffixs) {
      if (path.endsWith(suffix))
        path = path.substring(0, path.length() - suffix.length());
    }
    return path;
  }

  public static AnnotationSpec createAnnotationSpec(AnnotationMirror mirror) {

    AnnotationSpec.Builder builder =
        AnnotationSpec.builder(ClassName.get((TypeElement) mirror.getAnnotationType().asElement()));
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror
        .getElementValues().entrySet()) {
      ExecutableElement key = entry.getKey();
      AnnotationValue value = entry.getValue();
      builder.addMember(key.getSimpleName().toString(), "$L", value.getValue());
    }
    return builder.build();
  }

  public static interface BuilderVisitor {
    public default boolean checkTypeModifiers(TypeElement element, Modifier modifier) {
      return true;
    }

    public default boolean checkFieldModifiers(VariableElement element, Modifier modifier) {
      return true;
    }

    public default boolean checkTypeAnnotation(TypeElement element, AnnotationMirror mirror) {
      return true;
    }

    public default boolean checkFieldAnnotation(VariableElement element, AnnotationMirror mirror) {
      return true;
    }

    public default Set<Modifier> getTypeModifiers(TypeElement element, Set<Modifier> modifiers) {
      return modifiers;
    }

    public default Set<Modifier> getFieldModifiers(VariableElement element,
        Set<Modifier> modifiers) {
      return modifiers;
    }

    public default void buildStart(TypeSpec.Builder builder, TypeElement element) {}

    public default void buildEnd(TypeSpec.Builder builder, TypeElement element) {}

    public default void buildField(FieldSpec.Builder builder, VariableElement element) {}

    public default String buildEnumConstant(String builder, VariableElement element) {
      return builder;
    }
  }

  public static void copyModifiers(TypeElement element, TypeSpec.Builder builder,
      BuilderVisitor visitor) {
    for (Modifier modifier : visitor.getTypeModifiers(element, element.getModifiers()))
      if ((element.getKind() != ElementKind.ENUM || modifier != Modifier.FINAL)
          && visitor.checkTypeModifiers(element, modifier))
        builder.addModifiers(modifier);
  }

  public static void copyModifiers(VariableElement element, FieldSpec.Builder builder,
      BuilderVisitor visitor) {
    for (Modifier modifier : visitor.getFieldModifiers(element, element.getModifiers()))
      if (visitor.checkFieldModifiers(element, modifier))
        builder.addModifiers(modifier);
  }

  public static void copyAnnotations(TypeElement element, TypeSpec.Builder builder,
      BuilderVisitor visitor) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors())
      if (mirror.getAnnotationType().asElement().getAnnotation(NoCopy.class) == null
          && visitor.checkTypeAnnotation(element, mirror))
        builder.addAnnotation(createAnnotationSpec(mirror));
  }

  public static void copyAnnotations(VariableElement element, FieldSpec.Builder builder,
      BuilderVisitor visitor) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors())
      if (mirror.getAnnotationType().asElement().getAnnotation(NoCopy.class) == null
          && visitor.checkFieldAnnotation(element, mirror))
        builder.addAnnotation(createAnnotationSpec(mirror));
  }

  public static FieldSpec copyField(VariableElement element, BuilderVisitor visitor) {
    FieldSpec.Builder builder =
        FieldSpec.builder(TypeName.get(element.asType()), element.getSimpleName().toString());
    visitor.buildField(builder, element);
    copyAnnotations(element, builder, visitor);
    copyModifiers(element, builder, visitor);
    return builder.build();
  }

  public static TypeSpec copyElement(TypeElement element, String name, BuilderVisitor visitor) {
    return copyElementBuilder(element, name, visitor).build();
  }

  public static String capitalize(String s) {
    StringBuilder result = new StringBuilder(s.length());

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '_' && c != '-') {
        if (i > 0 && (s.charAt(i - 1) == '_' || s.charAt(i - 1) == '-')) {
          result.append(Character.toUpperCase(c));
        } else {
          result.append(c);
        }
      }
    }

    return result.toString();
  }

  public static TypeSpec.Builder copyElementBuilder(TypeElement element, String name,
      BuilderVisitor visitor) {
    TypeSpec.Builder builder = null;
    if (element.getKind() == ElementKind.CLASS)
      builder = TypeSpec.classBuilder(name);
    if (element.getKind() == ElementKind.ENUM)
      builder = TypeSpec.enumBuilder(name);
    if (element.getKind() == ElementKind.INTERFACE)
      builder = TypeSpec.annotationBuilder(name);
    visitor.buildStart(builder, element);
    copyModifiers(element, builder, visitor);
    copyAnnotations(element, builder, visitor);
    for (Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() == ElementKind.ENUM_CONSTANT) {
        VariableElement constant = (VariableElement) enclosedElement;
        String str = constant.getSimpleName().toString();
        builder.addEnumConstant(visitor.buildEnumConstant(str, constant));
      } else if (enclosedElement.getKind() == ElementKind.FIELD) {
        builder.addField(copyField((VariableElement) (enclosedElement), visitor));
      } else if (isTypeElement(enclosedElement)) {
        TypeElement type = (TypeElement) enclosedElement;
        builder.addType(copyElement(type, type.getSimpleName().toString(), visitor));
      }
    }
    visitor.buildEnd(builder, element);
    return builder;
  }

  public static JavaFormatterOptions option =
      JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build();
  public static Formatter formatter = Formatter.createFormatter(option);

  public static String format(String str) throws FormatterException {
    return formatter.formatSource(str);
  }

  public static String formatJson(String json) {
    return gson.toJson(gson.toJsonTree(json));
  }

  public static void forEachAnnotations(Element element, NodeWithAnnotations<?> classDecl,
      Cons2<AnnotationMirror, AnnotationExpr> func) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      func.get(mirror,
          classDecl.getAnnotationByName(mirror.getAnnotationType().asElement().toString())
              .orElseThrow(NoSuchElementException::new));
    }
  }

  public static void removeAnnotations(Element element, NodeWithAnnotations<?> classDecl) {
    forEachAnnotations(element, classDecl, (mirror, expr) -> {
      if (mirror.getAnnotationType().asElement().getAnnotation(NoCopy.class) != null) {
        expr.remove();
      }
    });
  }

  public static Fi sub(Fi file, String path) {
    Fi ro = file;
    for (var k : path.split(path.contains("/") ? "/" : "\\")) {
      if (!k.isEmpty())
        ro = ro.child(k);
    }
    return ro;
  }
}
