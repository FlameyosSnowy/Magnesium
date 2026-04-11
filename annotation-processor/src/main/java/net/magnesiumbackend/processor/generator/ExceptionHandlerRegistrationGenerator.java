package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.ExceptionHandler;
import net.magnesiumbackend.core.annotations.service.GeneratedExceptionHandlerClass;
import net.magnesiumbackend.core.base.MagnesiumController;
import net.magnesiumbackend.core.meta.GeneratedExceptionHandlers;
import net.magnesiumbackend.core.services.ServiceRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExceptionHandlerRegistrationGenerator {
    private final Types    types;
    private final Filer    filer;
    private final Elements elements;
    private final Messager messager;

    // Resolved once and reused across all generate() calls
    private final TypeElement throwableTypeElement;
    private final TypeElement responseTypeElement;
    private final TypeElement requestTypeElement;
    private final TypeElement serviceRegistryTypeElement;

    @Contract(pure = true)
    public ExceptionHandlerRegistrationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types    = types;
        this.filer    = filer;
        this.elements = elements;
        this.messager = messager;

        this.throwableTypeElement      = elements.getTypeElement("java.lang.Throwable");
        this.responseTypeElement       = elements.getTypeElement("net.magnesiumbackend.core.http.response.ResponseEntity");
        this.requestTypeElement       = elements.getTypeElement("net.magnesiumbackend.core.route.RequestContext");
        this.serviceRegistryTypeElement = elements.getTypeElement("net.magnesiumbackend.core.services.ServiceRegistry");
    }

    public String generate(TypeElement exceptionHandler) {
        TypeSpec.Builder classBuilder = generateClass(exceptionHandler);
        if (classBuilder == null) return null;

        PackageElement packageOf = elements.getPackageOf(exceptionHandler);

        String pkg = packageOf.getQualifiedName().toString();
        String className = exceptionHandler.getSimpleName() + "_magnesium_ExceptionHandler";

        writeClass(exceptionHandler, classBuilder);

        return pkg + "." + className;
    }

    /**
     * Generates the full class for one {@code @ExceptionHandler}-annotated type.
     *
     * <p>Example output for a handler class {@code GlobalExceptionHandlers} with
     * a constructor that takes {@code OrderService}:
     *
     * <pre>{@code
     * public final class GlobalExceptionHandlers_magnesium_ExceptionHandler
     *         implements GeneratedExceptionHandlerClass {
     *
     *     @Override
     *     public void register(MagnesiumApplication application, ServiceRegistry serviceRegistry) {
     *         GlobalExceptionHandlers __handler = new GlobalExceptionHandlers(
     *             serviceRegistry.get(OrderService.class)
     *         );
     *         GeneratedExceptionHandlers.GLOBAL.put(ValidationException.class,
     *             (exception, response) -> __handler.handleValidation((ValidationException) exception));
     *         GeneratedExceptionHandlers.GLOBAL.put(Exception.class,
     *             (exception, response) -> __handler.handleGeneric((Exception) exception, response));
     *     }
     * }
     * }</pre>
     */
    @Nullable
    private TypeSpec.Builder generateClass(TypeElement exceptionHandler) {
        Class<? extends MagnesiumController> classLevelControllerType = checkControllerType(exceptionHandler);

        MethodSpec.Builder register = MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumApplication.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .returns(void.class);

        String handlerVar = "__handler";
        CodeBlock instantiation = buildInstantiation(exceptionHandler, handlerVar);
        if (instantiation == null) return null; // error already reported
        register.addCode(instantiation);

        boolean anyMethod = false;
        for (Element enclosed : exceptionHandler.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
            if (annotation == null) continue;

            // Pre-validated by the processor, still guard to be safe
            List<? extends VariableElement> params = method.getParameters();
            if (params.isEmpty()) {
                error("@ExceptionHandler method must have at least one parameter.", method);
                continue;
            }

            VariableElement firstParam  = params.getFirst();
            TypeMirror      exceptionType = firstParam.asType();
            boolean         hasResponse = params.size() == 2;

            // Decide GLOBAL vs LOCAL bucket
            Class<? extends MagnesiumController> methodControllerType = annotation.controllerType();
            Class<? extends MagnesiumController> effective = chooseStronger(methodControllerType, classLevelControllerType);
            String bucket = (effective == null || effective == MagnesiumController.class) ? "GLOBAL" : "LOCAL";

            // Cast the raw exception parameter to its concrete type
            // e.g. (ValidationException) exception
            CodeBlock lambda = hasResponse
                ? CodeBlock.of("(exception, response) -> $N.$N(($T) exception, response)",
                handlerVar, method.getSimpleName(), TypeName.get(exceptionType))
                : CodeBlock.of("(exception, response) -> $N.$N(($T) exception)",
                handlerVar, method.getSimpleName(), TypeName.get(exceptionType));

            register.addStatement("$T.$L.put($T.class, $L)",
                ClassName.get(GeneratedExceptionHandlers.class),
                bucket,
                TypeName.get(exceptionType),
                lambda);

            anyMethod = true;
        }

        if (!anyMethod) {
            // Nothing to register, skip generating this class entirely
            return null;
        }

        String generatedName = exceptionHandler.getSimpleName() + "_magnesium_ExceptionHandler";
        return TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedExceptionHandlerClass.class)
            .addMethod(register.build());
    }

    @Nullable
    private CodeBlock buildInstantiation(TypeElement handlerClass, String varName) {
        List<ExecutableElement> constructors = new ArrayList<>();
        for (Element e : handlerClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add((ExecutableElement) e);
            }
        }

        // Prefer the richest constructor whose every param looks like a service type
        // (i.e. is not Throwable, Response, or a primitive, those wouldn't be in ServiceRegistry)
        ExecutableElement chosen = null;
        for (ExecutableElement ctor : constructors) {
            if (ctor.getParameters().isEmpty()) {
                // Record as fallback but keep looking for a richer one
                if (chosen == null) chosen = ctor;
                continue;
            }
            if (allParamsAreServices(ctor)) {
                chosen = ctor; // richer constructor wins
                break;
            }
        }

        if (chosen == null) {
            error("@ExceptionHandler class must have a no-arg constructor or a constructor "
                + "whose parameters are all service types resolvable from ServiceRegistry.", handlerClass);
            return null;
        }

        ClassName handlerClassName = ClassName.get(handlerClass);

        if (chosen.getParameters().isEmpty()) {
            // new HandlerClass()
            return CodeBlock.builder()
                .addStatement("$T $N = new $T()", handlerClassName, varName, handlerClassName)
                .build();
        }

        // new HandlerClass(serviceRegistry.get(A.class), serviceRegistry.get(B.class), ...)
        CodeBlock.Builder args = CodeBlock.builder();
        List<? extends VariableElement> params = chosen.getParameters();
        for (int i = 0; i < params.size(); i++) {
            TypeName paramType = TypeName.get(params.get(i).asType());
            args.add("serviceRegistry.get($T.class)", paramType);
            if (i < params.size() - 1) args.add(", ");
        }

        return CodeBlock.builder()
            .addStatement("$T $N = new $T($L)", handlerClassName, varName, handlerClassName, args.build())
            .build();
    }

    /**
     * Returns true if every constructor parameter is plausibly a service type,
     * i.e. not Throwable, not Response, not a primitive, not an array.
     * This heuristic is intentionally permissive: if a type is in ServiceRegistry
     * the call will succeed at runtime; if not, it will throw a clear NPE or
     * IllegalArgumentException from ServiceRegistry itself.
     */
    private boolean allParamsAreServices(ExecutableElement ctor) {
        for (VariableElement param : ctor.getParameters()) {
            TypeMirror t = param.asType();
            switch (t.getKind()) {
                case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, ARRAY -> { return false; }
                default -> {
                    // Exclude known framework types that would never be in ServiceRegistry
                    if (types.isAssignable(t, throwableTypeElement.asType())) return false;
                    if (types.isAssignable(t, requestTypeElement.asType()))  return false;
                    if (types.isAssignable(t, responseTypeElement.asType()))  return false;
                }
            }
        }
        return true;
    }

    private void writeClass(TypeElement originatingElement, TypeSpec.Builder classBuilder) {
        String pkg = elements.getPackageOf(originatingElement).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(pkg, classBuilder.build())
            .skipJavaLangImports(true)
            .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated exception handler class: " + e.getMessage(), originatingElement);
        }
    }

    /**
     * Reads the class-level {@code @ExceptionHandler} and returns its
     * {@code controllerType()} if it's not the default sentinel, else {@code null}.
     */
    @Nullable
    private static Class<? extends MagnesiumController> checkControllerType(TypeElement exceptionHandler) {
        ExceptionHandler annotation = exceptionHandler.getAnnotation(ExceptionHandler.class);
        if (annotation == null) return null;
        Class<? extends MagnesiumController> type = annotation.controllerType();
        return (type == MagnesiumController.class) ? null : type;
    }

    /**
     * Returns whichever controller type is more specific (i.e. not the default sentinel).
     * Method-level beats class-level; class-level beats nothing.
     */
    @Nullable
    private static Class<? extends MagnesiumController> chooseStronger(
        @Nullable Class<? extends MagnesiumController> methodLevel,
        @Nullable Class<? extends MagnesiumController> classLevel
    ) {
        if (methodLevel != null && methodLevel != MagnesiumController.class) return methodLevel;
        if (classLevel  != null && classLevel  != MagnesiumController.class) return classLevel;
        return null; // global
    }

    private void error(String message, Element element) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}