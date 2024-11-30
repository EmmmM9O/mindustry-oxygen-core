
package oxygen.annotations;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import com.google.auto.service.*;

/**
 * RecordGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("oxygen.annotations.RecordGen")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RecordGenerator extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotation, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(RecordGen.class);
      for (Element element : annotatedElements) {
      }
    }
    return false;
  }
}
