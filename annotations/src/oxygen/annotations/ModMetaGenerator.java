package oxygen.annotations;

import java.lang.reflect.*;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import com.google.gson.*;

/**
 * ModMetaGenerator
 */
@SupportedAnnotationTypes("oxygen.annotations.ModMeta")
@SupportedSourceVersion(SourceVersion.RELEASE_8)

public class ModMetaGenerator extends AbstractProcessor {
  private Gson gson = new GsonBuilder()
    .registerTypeAdapter(Class.class, 
        new JsonSerializer<Class<?>>() {
              @Override
                  public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
return new JsonPrimitive(src.getName());
                  }
        }
        ).create();
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(ModMeta.class);
    for (Element element : annotatedElements) {
      if (element instanceof TypeElement typeElement) {
        ModMeta meta = typeElement.getAnnotation(ModMeta.class);
        
      }
    }
    return false;
  }
}
