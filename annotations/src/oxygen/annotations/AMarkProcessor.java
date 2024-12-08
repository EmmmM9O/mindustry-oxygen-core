/* (C) 2024 */
package oxygen.annotations;

import static oxygen.annotations.Utils.*;

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
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AMarkProcessor extends AbstractProcessor {
    public Map<String, Set<Object>> classMap = new HashMap<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    public static class MethodObj {
        public String type;
        public String classpath;
        public String name;
        public List<String> params;

        public MethodObj(String name, List<String> params, String classpath, String type) {
            this.type = type;
            this.classpath = classpath;
            this.name = name;
            this.params = params;
        }
    }

    public static class ConstructorObj {
        public String type;
        public String classpath;
        public List<String> params;

        public ConstructorObj(List<String> params, String classpath, String type) {
            this.type = type;
            this.classpath = classpath;
            this.params = params;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : annotations) {
            AMark mark = element.getAnnotation(AMark.class);

            if (mark != null) {

                Set<Object> set = new HashSet<Object>();
                for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(element)) {
                    if (isTypeElement(annotatedElement)) {
                        set.add(new TypeObj(
                                getClassPath((TypeElement) annotatedElement),
                                annotatedElement.getKind().toString()));
                    } else if (annotatedElement.getKind() == ElementKind.FIELD
                            || annotatedElement.getKind() == ElementKind.ENUM_CONSTANT) {
                        if (annotatedElement.getKind() == ElementKind.FIELD
                                && !element.getModifiers().contains(Modifier.STATIC)
                                && mark.needStatic()) {
                            processingEnv
                                    .getMessager()
                                    .printMessage(
                                            Diagnostic.Kind.ERROR,
                                            "FIELD must be static "
                                                    + element.toString() + " for "
                                                    + annotatedElement.toString());
                        }
                        set.add(new FieldObj(
                                annotatedElement.getSimpleName().toString(),
                                getClassPath((TypeElement) annotatedElement.getEnclosingElement()),
                                annotatedElement.getKind().toString()));
                    } else if (annotatedElement.getKind() == ElementKind.METHOD) {
                        if (!element.getModifiers().contains(Modifier.STATIC) && mark.needStatic()) {
                            processingEnv
                                    .getMessager()
                                    .printMessage(
                                            Diagnostic.Kind.ERROR,
                                            "METHOD must be static "
                                                    + element.toString() + " for "
                                                    + annotatedElement.toString());
                        }
                        List<? extends VariableElement> paramsV =
                                ((ExecutableElement) annotatedElement).getParameters();
                        List<String> params = new ArrayList<>();
                        for (VariableElement variable : paramsV) {
                            params.add(variable.asType().toString());
                        }
                        set.add(new MethodObj(
                                annotatedElement.getSimpleName().toString(),
                                params,
                                getClassPath((TypeElement) annotatedElement.getEnclosingElement()),
                                annotatedElement.getKind().toString()));
                    } else if (annotatedElement.getKind() == ElementKind.CONSTRUCTOR) {
                        List<? extends VariableElement> paramsV =
                                ((ExecutableElement) annotatedElement).getParameters();
                        List<String> params = new ArrayList<>();
                        for (VariableElement variable : paramsV) {
                            params.add(variable.asType().toString());
                        }
                        set.add(new ConstructorObj(
                                params,
                                getClassPath((TypeElement) annotatedElement.getEnclosingElement()),
                                annotatedElement.getKind().toString()));
                    } else {
                        processingEnv
                                .getMessager()
                                .printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "@AMark unsupport pass Element Kind:"
                                                + annotatedElement.getKind().toString() + " for "
                                                + annotatedElement.toString());
                    }
                }
                classMap.put(getClassPath(element), set);
            }
        }
        if (roundEnv.processingOver()) {
            try {
                FileObject file =
                        processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "marks.json");
                String con = gson.toJson(classMap);
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.NOTE,
                                "generate mark.json with :" + classMap.keySet().toString());
                BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());
                bufferedWriter.write(con);
                bufferedWriter.close();

            } catch (IOException error) {
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "error while generate marks.json from @AMark :" + error.toString());
            }
        }
        return false;
    }
}
