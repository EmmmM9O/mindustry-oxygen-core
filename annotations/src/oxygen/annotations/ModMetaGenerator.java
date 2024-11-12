/* (C) 2024 */
package oxygen.annotations;

import com.google.auto.service.*;
import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

/**
 * ModMetaGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.ModMeta")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ModMetaGenerator extends AbstractProcessor {
    private Gson gson = new GsonBuilder().create();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(ModMeta.class);
        for (Element element : annotatedElements) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = ((TypeElement) element);
                ModMeta meta = typeElement.getAnnotation(ModMeta.class);
                ModMetaObj obj = new ModMetaObj(meta, typeElement.getQualifiedName().toString(), true);

                try {
                    FileObject file =
                            processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "mod.json");
                    String con = gson.toJson(obj);
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "generate mod.json : " + con);
                    BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());
                    bufferedWriter.write(con);

                } catch (IOException error) {
                    processingEnv
                            .getMessager()
                            .printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "error while generate mod.json from @ModMeta :" + error.getMessage());
                }
            }
        }
        return false;
    }
}
