package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.annotations.ApplicationProperties;
import net.magnesiumbackend.core.annotations.ApplicationProperty;
import net.magnesiumbackend.core.config.ConfigSource;
import net.magnesiumbackend.core.config.GeneratedConfigClass;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates compile-time configuration loaders for @ApplicationProperties classes.
 *
 * <p>For each class annotated with @ApplicationProperties, this generator creates
 * a GeneratedConfigClass implementation that loads properties from a ConfigSource
 * with type-safe binding, default values, and validation.</p>
 */
public final class ApplicationPropertiesGenerator {

    private final Types types;
    private final Filer filer;
    private final Elements elements;
    private final Messager messager;

    private final TypeElement stringTypeElement;
    private final TypeElement durationTypeElement;

    public ApplicationPropertiesGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types = types;
        this.filer = filer;
        this.elements = elements;
        this.messager = messager;
        this.stringTypeElement = elements.getTypeElement("java.lang.String");
        this.durationTypeElement = elements.getTypeElement("java.time.Duration");
    }

    /**
     * Generates a config loader for the given @ApplicationProperties class.
     *
     * @param propsClass the class annotated with @ApplicationProperties
     * @return the fully qualified name of the generated class, or null if generation failed
     */
    public String generate(TypeElement propsClass) {
        boolean isRecord = propsClass.getKind() == ElementKind.RECORD;
        if (propsClass.getKind() != ElementKind.CLASS && !isRecord) return null;

        ApplicationProperties propsAnnotation = propsClass.getAnnotation(ApplicationProperties.class);
        if (propsAnnotation == null) return null;

        String prefix = propsAnnotation.prefix();
        if (prefix.isEmpty()) {
            error("@ApplicationProperties prefix must not be empty", propsClass);
            return null;
        }

        // Records don't need no-arg constructor, classes do
        if (!isRecord && !hasNoArgConstructor(propsClass)) {
            error("@ApplicationProperties class must declare a public no-arg constructor (or use a record)", propsClass);
            return null;
        }

        TypeSpec spec = buildPropertiesLoader(propsClass, prefix, propsAnnotation, isRecord);
        if (spec == null) return null;

        String pkg = elements.getPackageOf(propsClass).getQualifiedName().toString();
        String generatedName = propsClass.getSimpleName() + "_magnesium_PropertiesLoader";

        try {
            JavaFile.builder(pkg, spec).skipJavaLangImports(true).build().writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated properties loader: " + e.getMessage(), propsClass);
            return null;
        }

        return pkg + "." + generatedName;
    }

    /**
     * Validates a @ApplicationProperties class without generating code.
     * Called during annotation processing to report errors early.
     *
     * @param propsClass the class to validate
     */
    public void validate(TypeElement propsClass) {
        boolean isRecord = propsClass.getKind() == ElementKind.RECORD;
        ApplicationProperties propsAnnotation = propsClass.getAnnotation(ApplicationProperties.class);
        if (propsAnnotation == null) return;

        String prefix = propsAnnotation.prefix();
        if (prefix.isEmpty()) {
            error("@ApplicationProperties prefix must not be empty", propsClass);
        }

        // Validate prefix format (should not start or end with dot)
        if (prefix.startsWith(".") || prefix.endsWith(".")) {
            error("@ApplicationProperties prefix should not start or end with a dot: " + prefix, propsClass);
        }

        // Validate all properties
        List<PropertyInfo> properties = findProperties(propsClass, isRecord);
        Set<String> seenNames = new java.util.HashSet<>();

        for (PropertyInfo prop : properties) {
            // Check for duplicate names
            if (!seenNames.add(prop.name())) {
                error("Duplicate property name: " + prop.name(), prop.element());
            }

            // Validate required + defaultValue conflict
            if (prop.required() && !prop.defaultValue().isEmpty()) {
                error("Property cannot be both required and have a default value: " + prop.name(), prop.element());
            }

            // Validate type is supported
            if (!isSupportedType(prop.type())) {
                error("Unsupported property type: " + prop.type() + " for property: " + prop.name(), prop.element());
            }

            // Validate pattern is only on String types
            if (!prop.pattern().isEmpty() && !isStringType(prop.type())) {
                error("Pattern validation can only be used on String properties: " + prop.name(), prop.element());
            }

            // Validate min/max are only on numeric types
            if ((prop.min() != Long.MIN_VALUE || prop.max() != Long.MAX_VALUE) && !isNumericType(prop.type())) {
                error("Min/max validation can only be used on numeric properties: " + prop.name(), prop.element());
            }
        }
    }

    private TypeSpec buildPropertiesLoader(TypeElement propsClass, String prefix, ApplicationProperties propsAnnotation, boolean isRecord) {
        String generatedName = propsClass.getSimpleName() + "_magnesium_PropertiesLoader";
        ClassName propsType = ClassName.get(propsClass);

        MethodSpec configTypeMethod = MethodSpec.methodBuilder("configType")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(Class.class)
            .addStatement("return $T.class", propsType)
            .build();

        MethodSpec.Builder load = MethodSpec.methodBuilder("load")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ConfigSource.class, "source")
            .returns(Object.class);

        List<PropertyInfo> properties = findProperties(propsClass, isRecord);

        if (isRecord) {
            // For records: collect values into local variables, then create record
            for (PropertyInfo prop : properties) {
                generateRecordVariable(load, prop, prefix);
            }

            // Build constructor arguments
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < properties.size(); i++) {
                if (i > 0) args.append(", ");
                args.append("__").append(properties.get(i).fieldName());
            }

            load.addStatement("return new $T($L)", propsType, args.toString());
        } else {
            // For classes: create instance and set fields
            load.addStatement("$T props = new $T()", propsType, propsType);

            for (PropertyInfo prop : properties) {
                if (!generatePropertyAssignment(load, prop, prefix)) {
                    return null;
                }
            }

            load.addStatement("return props");
        }

        return TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedConfigClass.class)
            .addMethod(configTypeMethod)
            .addMethod(load.build())
            .build();
    }

    private boolean generatePropertyAssignment(MethodSpec.Builder load, PropertyInfo prop, String prefix) {
        String fullKey = prefix + "." + prop.name();
        TypeMirror type = prop.type();
        String fieldName = prop.fieldName();

        // Generate the assignment based on type
        if (isStringType(type)) {
            generateStringAssignment(load, prop, fullKey, fieldName);
        } else if (isIntType(type)) {
            generateIntAssignment(load, prop, fullKey, fieldName);
        } else if (isLongType(type)) {
            generateLongAssignment(load, prop, fullKey, fieldName);
        } else if (isBooleanType(type)) {
            generateBooleanAssignment(load, prop, fullKey, fieldName);
        } else if (isDoubleType(type)) {
            generateDoubleAssignment(load, prop, fullKey, fieldName);
        } else if (isDurationType(type)) {
            generateDurationAssignment(load, prop, fullKey, fieldName);
        } else if (isEnumType(type)) {
            generateEnumAssignment(load, prop, fullKey, fieldName, type);
        } else if (isListType(type)) {
            generateListAssignment(load, prop, fullKey, fieldName, type);
        } else {
            error("Unsupported property type: " + type + " for property: " + prop.name(), prop.element());
            return false;
        }

        return true;
    }

    private void generateStringAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.required()) {
            load.addStatement("props.$N = source.requireString($S)", fieldName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("props.$N = source.getString($S) != null ? source.getString($S) : $S",
                fieldName, key, key, prop.defaultValue());
        } else {
            load.addStatement("props.$N = source.getString($S)", fieldName, key);
        }

        // Pattern validation
        if (!prop.pattern().isEmpty()) {
            load.beginControlFlow("if (props.$N != null && !props.$N.matches($S))", fieldName, fieldName, prop.pattern());
            load.addStatement("throw new IllegalStateException($S + props.$N + $S)",
                "Property " + key + " with value '", fieldName, "' does not match pattern: " + prop.pattern());
            load.endControlFlow();
        }
    }

    private void generateIntAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.required()) {
            load.addStatement("props.$N = source.requireInt($S)", fieldName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("props.$N = source.getInt($S) != null ? source.getInt($S) : $L",
                fieldName, key, key, Integer.parseInt(prop.defaultValue()));
        } else {
            load.addStatement("Integer _$N = source.getInt($S)", fieldName, key);
            load.addStatement("props.$N = _$N != null ? _$N : 0", fieldName, fieldName, fieldName);
        }

        generateMinMaxValidation(load, prop, key, fieldName);
    }

    private void generateLongAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.required()) {
            load.addStatement("props.$N = source.requireLong($S)", fieldName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("props.$N = source.getLong($S) != null ? source.getLong($S) : $LL",
                fieldName, key, key, Long.parseLong(prop.defaultValue()));
        } else {
            load.addStatement("Long _$N = source.getLong($S)", fieldName, key);
            load.addStatement("props.$N = _$N != null ? _$N : 0L", fieldName, fieldName, fieldName);
        }

        generateMinMaxValidation(load, prop, key, fieldName);
    }

    private void generateBooleanAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.required()) {
            load.addStatement("props.$N = source.requireBoolean($S)", fieldName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("props.$N = source.getBoolean($S) != null ? source.getBoolean($S) : $L",
                fieldName, key, key, Boolean.parseBoolean(prop.defaultValue()));
        } else {
            load.addStatement("Boolean _$N = source.getBoolean($S)", fieldName, key);
            load.addStatement("props.$N = _$N != null ? _$N : false", fieldName, fieldName, fieldName);
        }
    }

    private void generateDoubleAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.required()) {
            load.addStatement("props.$N = source.requireDouble($S)", fieldName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("props.$N = source.getDouble($S) != null ? source.getDouble($S) : $LD",
                fieldName, key, key, Double.parseDouble(prop.defaultValue()));
        } else {
            load.addStatement("Double _$N = source.getDouble($S)", fieldName, key);
            load.addStatement("props.$N = _$N != null ? _$N : 0.0", fieldName, fieldName, fieldName);
        }
    }

    private void generateDurationAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        load.addStatement("String _$N = source.getString($S)", fieldName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", fieldName, fieldName, fieldName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", fieldName);
        load.addStatement("props.$N = $T.parse(_$NValue)", fieldName, Duration.class, fieldName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", fieldName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required duration: " + key);
            load.endControlFlow();
        }
    }

    private void generateEnumAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName, TypeMirror enumType) {
        TypeName enumTypeName = TypeName.get(enumType);
        load.addStatement("String _$N = source.getString($S)", fieldName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", fieldName, fieldName, fieldName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", fieldName);
        load.addStatement("props.$N = $T.valueOf(_$NValue.toUpperCase().replace(\"-\", \"_\"))",
            fieldName, enumTypeName, fieldName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", fieldName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required enum: " + key);
            load.endControlFlow();
        }
    }

    private void generateListAssignment(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName, TypeMirror listType) {
        // For now, treat lists as comma-separated strings
        load.addStatement("String _$N = source.getString($S)", fieldName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", fieldName, fieldName, fieldName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", fieldName);
        load.addStatement("props.$N = java.util.Arrays.asList(_$NValue.split(\"\\s*,\\s*\"))",
            fieldName, fieldName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", fieldName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required list: " + key);
            load.endControlFlow();
        }
    }

    private void generateRecordVariable(MethodSpec.Builder load, PropertyInfo prop, String prefix) {
        String fullKey = prefix + "." + prop.name();
        TypeMirror type = prop.type();
        String varName = "__" + prop.fieldName();

        if (isStringType(type)) {
            generateRecordStringVariable(load, prop, fullKey, varName);
        } else if (isIntType(type)) {
            generateRecordIntVariable(load, prop, fullKey, varName);
        } else if (isLongType(type)) {
            generateRecordLongVariable(load, prop, fullKey, varName);
        } else if (isBooleanType(type)) {
            generateRecordBooleanVariable(load, prop, fullKey, varName);
        } else if (isDoubleType(type)) {
            generateRecordDoubleVariable(load, prop, fullKey, varName);
        } else if (isDurationType(type)) {
            generateRecordDurationVariable(load, prop, fullKey, varName);
        } else if (isEnumType(type)) {
            generateRecordEnumVariable(load, prop, fullKey, varName, type);
        } else if (isListType(type)) {
            generateRecordListVariable(load, prop, fullKey, varName);
        }
    }

    private void generateRecordStringVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        if (prop.required()) {
            load.addStatement("String $N = source.requireString($S)", varName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("String $N = source.getString($S) != null ? source.getString($S) : $S",
                varName, key, key, prop.defaultValue());
        } else {
            load.addStatement("String $N = source.getString($S)", varName, key);
        }

        // Pattern validation
        if (!prop.pattern().isEmpty()) {
            load.beginControlFlow("if ($N != null && !$N.matches($S))", varName, varName, prop.pattern());
            load.addStatement("throw new IllegalStateException($S + $N + $S)",
                "Property " + key + " with value '", varName, "' does not match pattern: " + prop.pattern());
            load.endControlFlow();
        }
    }

    private void generateRecordIntVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        if (prop.required()) {
            load.addStatement("int $N = source.requireInt($S)", varName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("int $N = source.getInt($S) != null ? source.getInt($S) : $L",
                varName, key, key, Integer.parseInt(prop.defaultValue()));
        } else {
            load.addStatement("Integer _$N = source.getInt($S)", varName, key);
            load.addStatement("int $N = _$N != null ? _$N : 0", varName, varName, varName);
        }

        // Min/max validation
        if (prop.min() != Long.MIN_VALUE) {
            load.beginControlFlow("if ($N < $LL)", varName, prop.min());
            load.addStatement("throw new IllegalStateException($S + $N + $S + $LL + $S)",
                "Property " + key + " with value ", varName, " is less than minimum: ", prop.min(), "");
            load.endControlFlow();
        }
        if (prop.max() != Long.MAX_VALUE) {
            load.beginControlFlow("if ($N > $LL)", varName, prop.max());
            load.addStatement("throw new IllegalStateException($S + $N + $S + $LL + $S)",
                "Property " + key + " with value ", varName, " is greater than maximum: ", prop.max(), "");
            load.endControlFlow();
        }
    }

    private void generateRecordLongVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        if (prop.required()) {
            load.addStatement("long $N = source.requireLong($S)", varName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("long $N = source.getLong($S) != null ? source.getLong($S) : $LL",
                varName, key, key, Long.parseLong(prop.defaultValue()));
        } else {
            load.addStatement("Long _$N = source.getLong($S)", varName, key);
            load.addStatement("long $N = _$N != null ? _$N : 0L", varName, varName, varName);
        }

        // Min/max validation
        if (prop.min() != Long.MIN_VALUE) {
            load.beginControlFlow("if ($N < $LL)", varName, prop.min());
            load.addStatement("throw new IllegalStateException($S + $N + $S + $LL + $S)",
                "Property " + key + " with value ", varName, " is less than minimum: ", prop.min(), "");
            load.endControlFlow();
        }
        if (prop.max() != Long.MAX_VALUE) {
            load.beginControlFlow("if ($N > $LL)", varName, prop.max());
            load.addStatement("throw new IllegalStateException($S + $N + $S + $LL + $S)",
                "Property " + key + " with value ", varName, " is greater than maximum: ", prop.max(), "");
            load.endControlFlow();
        }
    }

    private void generateRecordBooleanVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        if (prop.required()) {
            load.addStatement("boolean $N = source.requireBoolean($S)", varName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("boolean $N = source.getBoolean($S) != null ? source.getBoolean($S) : $L",
                varName, key, key, Boolean.parseBoolean(prop.defaultValue()));
        } else {
            load.addStatement("Boolean _$N = source.getBoolean($S)", varName, key);
            load.addStatement("boolean $N = _$N != null ? _$N : false", varName, varName, varName);
        }
    }

    private void generateRecordDoubleVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        if (prop.required()) {
            load.addStatement("double $N = source.requireDouble($S)", varName, key);
        } else if (!prop.defaultValue().isEmpty()) {
            load.addStatement("double $N = source.getDouble($S) != null ? source.getDouble($S) : $LD",
                varName, key, key, Double.parseDouble(prop.defaultValue()));
        } else {
            load.addStatement("Double _$N = source.getDouble($S)", varName, key);
            load.addStatement("double $N = _$N != null ? _$N : 0.0", varName, varName, varName);
        }
    }

    private void generateRecordDurationVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        load.addStatement("String _$N = source.getString($S)", varName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", varName, varName, varName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", varName);
        load.addStatement("$T $N = $T.parse(_$NValue)", java.time.Duration.class, varName, java.time.Duration.class, varName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", varName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required duration: " + key);
            load.endControlFlow();
        } else {
            load.addStatement("$T $N = _$NValue != null ? $T.parse(_$NValue) : null",
                java.time.Duration.class, varName, varName, java.time.Duration.class, varName);
        }
    }

    private void generateRecordEnumVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName, TypeMirror enumType) {
        TypeName enumTypeName = TypeName.get(enumType);
        load.addStatement("String _$N = source.getString($S)", varName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", varName, varName, varName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", varName);
        load.addStatement("$T $N = $T.valueOf(_$NValue.toUpperCase().replace(\"-\", \"_\"))",
            enumTypeName, varName, enumTypeName, varName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", varName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required enum: " + key);
            load.endControlFlow();
        } else {
            load.addStatement("$T $N = _$NValue != null ? $T.valueOf(_$NValue.toUpperCase().replace(\"-\", \"_\")) : null",
                enumTypeName, varName, enumTypeName, varName);
        }
    }

    private void generateRecordListVariable(MethodSpec.Builder load, PropertyInfo prop, String key, String varName) {
        load.addStatement("String _$N = source.getString($S)", varName, key);

        String defaultValue = prop.defaultValue().isEmpty() ? "null" : "\"" + prop.defaultValue() + "\"";
        load.addStatement("String _$NValue = _$N != null ? _$N : $L", varName, varName, varName, defaultValue);

        load.beginControlFlow("if (_$NValue != null)", varName);
        load.addStatement("$T $N = java.util.Arrays.asList(_$NValue.split(\"\\s*,\\s*\"))",
            java.util.List.class, varName, varName);
        load.endControlFlow();

        if (prop.required()) {
            load.beginControlFlow("if (_$NValue == null)", varName);
            load.addStatement("throw new IllegalStateException($S)", "Missing required list: " + key);
            load.endControlFlow();
        } else {
            load.addStatement("$T $N = _$NValue != null ? java.util.Arrays.asList(_$NValue.split(\"\\s*,\\s*\")) : null",
                java.util.List.class, varName, varName);
        }
    }

    private void generateMinMaxValidation(MethodSpec.Builder load, PropertyInfo prop, String key, String fieldName) {
        if (prop.min() != Long.MIN_VALUE) {
            load.beginControlFlow("if (props.$N < $LL)", fieldName, prop.min());
            load.addStatement("throw new IllegalStateException($S + props.$N + $S + $LL + $S)",
                "Property " + key + " with value ", fieldName, " is less than minimum: ", prop.min(), "");
            load.endControlFlow();
        }

        if (prop.max() != Long.MAX_VALUE) {
            load.beginControlFlow("if (props.$N > $LL)", fieldName, prop.max());
            load.addStatement("throw new IllegalStateException($S + props.$N + $S + $LL + $S)",
                "Property " + key + " with value ", fieldName, " is greater than maximum: ", prop.max(), "");
            load.endControlFlow();
        }
    }

    private List<PropertyInfo> findProperties(TypeElement propsClass, boolean isRecord) {
        List<PropertyInfo> properties = new ArrayList<>();

        if (isRecord) {
            // For records: look at record components (constructor params that become fields)
            for (Element element : propsClass.getEnclosedElements()) {
                if (element.getKind() == ElementKind.RECORD_COMPONENT) {
                    VariableElement component = (VariableElement) element;
                    String componentName = component.getSimpleName().toString();

                    // Look for @ApplicationProperty annotation on the component
                    ApplicationProperty propAnnotation = component.getAnnotation(ApplicationProperty.class);

                    // Also check the accessor method
                    if (propAnnotation == null) {
                        for (Element e : propsClass.getEnclosedElements()) {
                            if (e.getKind() == ElementKind.METHOD) {
                                ExecutableElement method = (ExecutableElement) e;
                                if (method.getSimpleName().toString().equals(componentName)
                                    && method.getParameters().isEmpty()) {
                                    propAnnotation = method.getAnnotation(ApplicationProperty.class);
                                    break;
                                }
                            }
                        }
                    }

                    if (propAnnotation != null) {
                        String propName = propAnnotation.name().isEmpty()
                            ? componentName
                            : propAnnotation.name();

                        properties.add(new PropertyInfo(
                            propName,
                            componentName,
                            component.asType(),
                            propAnnotation.defaultValue(),
                            propAnnotation.required(),
                            propAnnotation.pattern(),
                            propAnnotation.min(),
                            propAnnotation.max(),
                            propAnnotation.description(),
                            component
                        ));
                    }
                }
            }
        } else {
            // For classes: look at fields and setter methods
            for (Element element : propsClass.getEnclosedElements()) {
                if (element.getKind() == ElementKind.FIELD) {
                    VariableElement field = (VariableElement) element;
                    if (field.getModifiers().contains(Modifier.STATIC)) continue;

                    ApplicationProperty propAnnotation = field.getAnnotation(ApplicationProperty.class);
                    if (propAnnotation != null) {
                        properties.add(new PropertyInfo(
                            propAnnotation.name(),
                            field.getSimpleName().toString(),
                            field.asType(),
                            propAnnotation.defaultValue(),
                            propAnnotation.required(),
                            propAnnotation.pattern(),
                            propAnnotation.min(),
                            propAnnotation.max(),
                            propAnnotation.description(),
                            field
                        ));
                    }
                } else if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) element;
                    // Check for setter methods (setXxx)
                    String methodName = method.getSimpleName().toString();
                    if (methodName.startsWith("set") && methodName.length() > 3
                        && method.getParameters().size() == 1
                        && method.getReturnType().getKind() == TypeKind.VOID) {

                        ApplicationProperty propAnnotation = method.getAnnotation(ApplicationProperty.class);
                        if (propAnnotation != null) {
                            // Derive property name from setter if not specified
                            String propName = propAnnotation.name();
                            if (propName.isEmpty()) {
                                // Convert setXxx to xxx (lowercase first char)
                                propName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                            }
                            properties.add(new PropertyInfo(
                                propName,
                                methodName,  // Use setter method name
                                method.getParameters().get(0).asType(),
                                propAnnotation.defaultValue(),
                                propAnnotation.required(),
                                propAnnotation.pattern(),
                                propAnnotation.min(),
                                propAnnotation.max(),
                                propAnnotation.description(),
                                method
                            ));
                        }
                    }
                }
            }
        }

        return properties;
    }

    private boolean hasNoArgConstructor(TypeElement propsClass) {
        for (Element e : propsClass.getEnclosedElements()) {
            if (e.getKind() != ElementKind.CONSTRUCTOR) continue;
            if (!(e instanceof javax.lang.model.element.ExecutableElement ctor)) continue;
            if (!ctor.getModifiers().contains(Modifier.PUBLIC)) continue;
            if (!ctor.getParameters().isEmpty()) continue;
            return true;
        }
        return false;
    }

    private boolean isSupportedType(TypeMirror type) {
        return isStringType(type)
            || isIntType(type)
            || isLongType(type)
            || isBooleanType(type)
            || isDoubleType(type)
            || isDurationType(type)
            || isEnumType(type)
            || isListType(type);
    }

    private boolean isStringType(TypeMirror type) {
        return types.isAssignable(type, stringTypeElement.asType());
    }

    private boolean isIntType(TypeMirror type) {
        return type.getKind() == TypeKind.INT
            || isBoxed(type, Integer.class);
    }

    private boolean isLongType(TypeMirror type) {
        return type.getKind() == TypeKind.LONG
            || isBoxed(type, Long.class);
    }

    private boolean isBooleanType(TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN
            || isBoxed(type, Boolean.class);
    }

    private boolean isDoubleType(TypeMirror type) {
        return type.getKind() == TypeKind.DOUBLE
            || isBoxed(type, Double.class);
    }

    private boolean isNumericType(TypeMirror type) {
        return isIntType(type) || isLongType(type) || isDoubleType(type)
            || isBoxed(type, Float.class)
            || type.getKind() == TypeKind.FLOAT;
    }

    private boolean isDurationType(TypeMirror type) {
        return durationTypeElement != null && types.isAssignable(type, durationTypeElement.asType());
    }

    private boolean isEnumType(TypeMirror type) {
        if (type instanceof DeclaredType declared) {
            Element el = declared.asElement();
            return el.getKind() == ElementKind.ENUM;
        }
        return false;
    }

    private boolean isListType(TypeMirror type) {
        if (type instanceof DeclaredType declared) {
            Element el = declared.asElement();
            if (el instanceof TypeElement te) {
                String name = te.getQualifiedName().toString();
                return name.equals("java.util.List") || name.equals("java.util.Set")
                    || name.equals("java.util.ArrayList") || name.equals("java.util.HashSet");
            }
        }
        return false;
    }

    private boolean isBoxed(TypeMirror mirror, Class<?> boxed) {
        TypeElement boxedElement = elements.getTypeElement(boxed.getCanonicalName());
        return boxedElement != null && types.isAssignable(mirror, boxedElement.asType());
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Immutable information about a single property.
     */
    private record PropertyInfo(
        String name,
        String fieldName,
        TypeMirror type,
        String defaultValue,
        boolean required,
        String pattern,
        long min,
        long max,
        String description,
        Element element
    ) {}
}
