package oxygen.annotations;

import com.google.auto.service.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

/**
 * AnnotationsProcessor
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.AMark")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AMarkProcessor extends AbstractProcessor {
  public Map<String, Set<Object>> classMap = new HashMap<>();
  private Gson gson = new GsonBuilder().create();

  public static class TypeObj {
    public String type;
    public String classpath;

    public TypeObj(String classpath, String type) {
      this.type = type;
      this.classpath = classpath;
    }
  }

  public static class FieldObj {
    public String type;
    public String classpath;
    public String name;

    public FieldObj(String name, String classpath, String type) {
      this.type = type;
      this.classpath = classpath;
      this.name = name;
    }
  }
  public boolean flag=false;
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
	  if(flag) return true;
    Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AMark.class);
    for (Element element : annotatedElements) {
      if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
        TypeElement annotationElement = ((TypeElement) element);
        Set<Object> set = new HashSet<Object>();
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(annotationElement)) {
          if (annotatedElement.getKind() == ElementKind.CLASS || annotatedElement.getKind() == ElementKind.INTERFACE
              || annotatedElement.getKind() == ElementKind.ANNOTATION_TYPE
              || annotatedElement.getKind() == ElementKind.ENUM) {
            set.add(new TypeObj(((TypeElement) annotatedElement).getQualifiedName().toString(),
                annotatedElement.getKind().toString()));
          } else if (annotatedElement.getKind() == ElementKind.FIELD
              || annotatedElement.getKind() == ElementKind.ENUM_CONSTANT) {
            set.add(new FieldObj(annotatedElement.getSimpleName().toString(),
                ((TypeElement) annotatedElement.getEnclosingElement()).getQualifiedName().toString(),
                annotatedElement.getKind().toString()));
          } else {
            processingEnv
                .getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    "@AMark unsupport pass Element Kind:" + annotatedElement.getKind().toString()
                        + " for " + annotatedElement.toString());
          }

        }
        classMap.put(annotationElement.getQualifiedName().toString(), set);

      } else {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "@AMark only support annotation");
      }
    }
    flag=true;
        try {
          FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
              "marks.json");
          String con = gson.toJson(classMap);
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
              "generate mark.json with :" + classMap.keySet().toString());
          BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());
          bufferedWriter.write(con);
          bufferedWriter.close();

        } catch (IOException error) {
          processingEnv
              .getMessager()
              .printMessage(
                  Diagnostic.Kind.ERROR,
                  "error while generate marks.json from @AMark :" + error.getMessage());
        }
    return true;
  }
}
