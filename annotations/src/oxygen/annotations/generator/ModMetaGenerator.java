/* (C) 2024 */
package oxygen.annotations.generator;

import arc.util.*;
import com.google.auto.service.*;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import oxygen.annotations.*;

/**
 * ModMetaGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.generator.ModMetaG")
public class ModMetaGenerator extends BaseProcessor {

    @Override
    public boolean process(RoundEnvironment roundEnv) throws Exception {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(ModMetaG.class);
        for (Element element : annotatedElements) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = ((TypeElement) element);
                ModMetaG meta = typeElement.getAnnotation(ModMetaG.class);
                ModMetadata obj =
                        new ModMetadata(meta, typeElement.getQualifiedName().toString(), true);

                FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "mod.json");
                String con = Utils.gson.toJson(obj);
                info("generate mod.json : " + con);
                BufferedWriter bufferedWriter = new BufferedWriter(file.openWriter());
                bufferedWriter.write(con);
                bufferedWriter.close();
            } else {
                throw new ArcRuntimeException("@ModMeta only support class");
            }
        }
        return true;
    }
}
