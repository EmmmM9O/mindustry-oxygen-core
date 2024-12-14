/* (C) 2024 */
package oxygen.annotations;

import static oxygen.annotations.Utils.*;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import com.google.gson.reflect.*;
import com.palantir.javapoet.*;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;
import javax.tools.Diagnostic.*;

/**
 * BaseProcessor
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BaseProcessor extends AbstractProcessor {
    public static Filer filer;
    public static Messager messager;
    public static Elements elementu;
    public String key = "no";
    protected int round;
    protected int rounds = 1;
    protected RoundEnvironment env;
    protected Fi rootDirectory;

    public static void info(String message) {
        messager.printMessage(Kind.NOTE, message);
        Log.err("[CODEGEN INFO] " + message);
    }

    public static void err(String message) {
        messager.printMessage(Kind.ERROR, message);
        Log.err("[CODEGEN ERROR] " + message);
    }

    public static void err(String message, Element elem) {
        messager.printMessage(Kind.ERROR, message, elem);
        Log.err("[CODEGEN ERROR] " + message + ": " + elem);
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
        messager = env.getMessager();
        elementu = env.getElementUtils();
        Log.level = LogLevel.info;

        if (System.getProperty("debug") != null) {
            Log.level = LogLevel.debug;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> an, RoundEnvironment roundEnv) {
        if (rootDirectory == null) {
            try {
                String path = Fi.get(filer.getResource(StandardLocation.CLASS_OUTPUT, key, key)
                                .toUri()
                                .toURL()
                                .toString()
                                .substring(OS.isWindows ? 6 : "file:".length()))
                        .parent()
                        .parent()
                        .parent()
                        .parent()
                        .parent()
                        .parent()
                        .toString()
                        .replace("%20", " ");
                rootDirectory = Fi.get(path);
                info(toString() + " work at " + rootDirectory.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.env = roundEnv;
        try {
            return processR(roundEnv, an);
        } catch (Throwable e) {
            err(e.toString());
            // e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    public boolean processR(RoundEnvironment env, Set<? extends TypeElement> arg0) throws Exception {
        return process(env);
    }

    public boolean process(RoundEnvironment env) throws Exception {
        return false;
    }

    public void writeTo(
            String context, String name, String path, TypeElement element, List<Element> originatingElements)
            throws Exception {
        if (!isOuterClass(element)) throw new ArcRuntimeException("TypeElement must be outer " + element.toString());
        String packageName = getMetaPath(elementu.getPackageOf(element).toString(), path);
        String formatted = format(context);
        String fileName = packageName.isEmpty() ? name : packageName + "." + name;
        JavaFileObject filerSourceFile = null;
        if (originatingElements != null)
            filerSourceFile = filer.createSourceFile(
                    fileName, originatingElements.toArray(new Element[originatingElements.size()]));
        else filerSourceFile = filer.createSourceFile(fileName);
        try (Writer writer = filerSourceFile.openWriter()) {
            writer.write(formatted);
        } catch (Exception e) {
            try {
                filerSourceFile.delete();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    public void writeTo(TypeSpec spec, String path, TypeElement element) throws Exception {
        String packageName = getMetaPath(elementu.getPackageOf(element).toString(), path);
        writeTo(
                JavaFile.builder(packageName, spec).build().toString(),
                spec.name(),
                path,
                element,
                spec.originatingElements());
    }

    public Fi find(String path, Func<Fi, Boolean> func) {
        Seq<Fi> list =
                rootDirectory.findAll(fi -> func.get(fi) && fi.absolutePath().contains(path));
        if (list.size != 1) {
            return list.get(0);
        }
        throw new ArcRuntimeException("can not find source file " + path + " from " + list.toString());
    }

    public Fi fromSource(String packageName, String className) {
        String path = packageName.replace(".", "/");
        return find(path, fi -> fi.nameWithoutExtension().equals(className));
    }

    public Fi fromSource(TypeElement element) {
        return fromSource(
                elementu.getPackageOf(element).toString(),
                element.getSimpleName().toString());
    }

    public static String resourcePrefix = "resource:";

    public String getContext(String str) {
        if (str.startsWith(resourcePrefix)) {
            String s = str.substring(resourcePrefix.length());
            String[] spi = s.split(":");
            if (spi.length == 1) {
                return find(spi[1], fi -> true).readString();
            }
            if (spi.length == 2 && spi[1].endsWith("json")) {
                return gson.toJson(((Map<String, Object>) gson.fromJson(
                                find(spi[1], fi -> true).readString(),
                                new TypeToken<Map<String, Object>>() {}.getType()))
                        .get(spi[2]));
            }
            throw new ArcRuntimeException("unknown path " + str);
        }
        return str;
    }
}
