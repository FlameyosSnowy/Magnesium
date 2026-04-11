package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.annotations.ConfigKey;
import net.magnesiumbackend.core.config.ConfigSource;
import net.magnesiumbackend.core.config.GeneratedConfigClass;
import net.magnesiumbackend.core.config.RequiredValue;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ApplicationConfigurationGenerator {

    private final Types types;
    private final Filer filer;
    private final Elements elements;
    private final Messager messager;

    private final TypeElement stringTypeElement;

    public ApplicationConfigurationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types = types;
        this.filer = filer;
        this.elements = elements;
        this.messager = messager;
        this.stringTypeElement = elements.getTypeElement("java.lang.String");
    }

    public @Nullable String generate(TypeElement configClass) {
        if (configClass.getKind() != ElementKind.CLASS) return null;

        if (!hasNoArgConstructor(configClass)) {
            error("@ApplicationConfiguration class must declare a public no-arg constructor.", configClass);
            return null;
        }

        TypeSpec spec = buildConfigMapper(configClass);
        if (spec == null) return null;

        String pkg = elements.getPackageOf(configClass).getQualifiedName().toString();
        String generatedName = configClass.getSimpleName() + "_magnesium_Config";

        try {
            JavaFile.builder(pkg, spec).skipJavaLangImports(true).build().writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated config class: " + e.getMessage(), configClass);
            return null;
        }

        return pkg + "." + generatedName;
    }

    private @Nullable TypeSpec buildConfigMapper(TypeElement configClass) {
        String generatedName = configClass.getSimpleName() + "_magnesium_Config";
        ClassName configType = ClassName.get(configClass);

        MethodSpec configTypeMethod = MethodSpec.methodBuilder("configType")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(Class.class)
            .addStatement("return $T.class", configType)
            .build();

        MethodSpec.Builder load = MethodSpec.methodBuilder("load")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ConfigSource.class, "source")
            .returns(Object.class);

        load.addStatement("$T cfg = new $T()", configType, configType);

        List<VariableElement> fields = findConfigFields(configClass);
        for (VariableElement field : fields) {
            if (!generateFieldAssignment(load, configClass, field)) {
                return null;
            }
        }

        load.addStatement("return cfg");

        return TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedConfigClass.class)
            .addMethod(configTypeMethod)
            .addMethod(load.build())
            .build();
    }

    private boolean generateFieldAssignment(MethodSpec.Builder load, TypeElement configClass, VariableElement field) {
        TypeMirror fieldType = field.asType();
        String fieldName = field.getSimpleName().toString();

        String key = fieldName;
        ConfigKey keyAnnotation = field.getAnnotation(ConfigKey.class);
        if (keyAnnotation != null && keyAnnotation.value() != null && !keyAnnotation.value().isBlank()) {
            key = keyAnnotation.value();
        }

        boolean required = field.getAnnotation(RequiredValue.class) != null || fieldType.getKind().isPrimitive();

        TypeKind kind = fieldType.getKind();

        if (types.isAssignable(fieldType, stringTypeElement.asType())) {
            if (required) {
                load.addStatement("cfg.$N = source.requireString($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getString($S)", fieldName, key);
            }
            return true;
        }

        if (kind == TypeKind.INT || isBoxed(fieldType, Integer.class)) {
            if (required) {
                load.addStatement("cfg.$N = source.requireInt($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getInt($S)", fieldName, key);
            }
            return true;
        }

        if (kind == TypeKind.LONG || isBoxed(fieldType, Long.class)) {
            if (required) {
                load.addStatement("cfg.$N = source.requireLong($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getLong($S)", fieldName, key);
            }
            return true;
        }

        if (kind == TypeKind.BOOLEAN || isBoxed(fieldType, Boolean.class)) {
            if (required) {
                load.addStatement("cfg.$N = source.requireBoolean($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getBoolean($S)", fieldName, key);
            }
            return true;
        }

        if (kind == TypeKind.DOUBLE || isBoxed(fieldType, Double.class)) {
            if (required) {
                load.addStatement("cfg.$N = source.requireDouble($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getDouble($S)", fieldName, key);
            }
            return true;
        }

        if (kind == TypeKind.FLOAT || isBoxed(fieldType, Float.class)) {
            if (required) {
                load.addStatement("cfg.$N = source.requireFloat($S)", fieldName, key);
            } else {
                load.addStatement("cfg.$N = source.getFloat($S)", fieldName, key);
            }
            return true;
        }

        if (fieldType instanceof DeclaredType declared) {
            Element el = declared.asElement();
            if (el instanceof TypeElement te && te.getKind() == ElementKind.ENUM) {
                TypeName enumType = TypeName.get(fieldType);
                if (required) {
                    load.addStatement("var __enum = source.getEnum($S, $T.class)", key, enumType);
                    load.beginControlFlow("if (__enum == null)");
                    load.addStatement("throw new $T($S + source.name())",
                        IllegalStateException.class,
                        "Missing required config value: '" + key + "' from source: ");
                    load.endControlFlow();
                    load.addStatement("cfg.$N = __enum", fieldName);
                } else {
                    load.addStatement("cfg.$N = source.getEnum($S, $T.class)", fieldName, key, enumType);
                }
                return true;
            }
        }

        error("Unsupported @ApplicationConfiguration field type: " + fieldType + " (field: " + fieldName + ")", configClass);
        return false;
    }

    private boolean isBoxed(TypeMirror mirror, Class<?> boxed) {
        TypeElement boxedElement = elements.getTypeElement(boxed.getCanonicalName());
        return boxedElement != null && types.isAssignable(mirror, boxedElement.asType());
    }

    private List<VariableElement> findConfigFields(TypeElement configClass) {
        List<VariableElement> fields = new ArrayList<>();
        for (Element e : configClass.getEnclosedElements()) {
            if (e.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) e;
            if (field.getModifiers().contains(Modifier.STATIC)) continue;
            fields.add(field);
        }
        return fields;
    }

    private boolean hasNoArgConstructor(TypeElement configClass) {
        for (Element e : configClass.getEnclosedElements()) {
            if (e.getKind() != ElementKind.CONSTRUCTOR) continue;
            if (!(e instanceof javax.lang.model.element.ExecutableElement ctor)) continue;
            if (!ctor.getModifiers().contains(Modifier.PUBLIC)) continue;
            if (!ctor.getParameters().isEmpty()) continue;
            return true;
        }
        return false;
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
