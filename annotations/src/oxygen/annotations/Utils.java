/* (C) 2024 */
package oxygen.annotations;

import javax.lang.model.element.*;

/**
 * Utils
 */
public class Utils {
    public static boolean isTypeElement(Element element) {
        return element.getKind() == ElementKind.CLASS
                || element.getKind() == ElementKind.INTERFACE
                || element.getKind() == ElementKind.ANNOTATION_TYPE
                || element.getKind() == ElementKind.ENUM;
    }

    public static boolean isOuterClass(TypeElement typeElement) {
        return typeElement.getEnclosingElement() == null
                || typeElement.getEnclosingElement().getKind() == ElementKind.PACKAGE;
    }
}
