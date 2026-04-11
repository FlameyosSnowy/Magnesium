package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.Authenticated;
import net.magnesiumbackend.core.annotations.ConnectMapping;
import net.magnesiumbackend.core.annotations.DeleteMapping;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.HeadMapping;
import net.magnesiumbackend.core.annotations.Idempotent;
import net.magnesiumbackend.core.annotations.OptionsMapping;
import net.magnesiumbackend.core.annotations.PatchMappings;
import net.magnesiumbackend.core.annotations.PostMapping;
import net.magnesiumbackend.core.annotations.PutMapping;
import net.magnesiumbackend.core.annotations.RateLimit;
import net.magnesiumbackend.core.annotations.RequestHeader;
import net.magnesiumbackend.core.annotations.Requires;
import net.magnesiumbackend.core.annotations.TracesMapping;
import net.magnesiumbackend.core.annotations.ApplicationConfiguration;
import net.magnesiumbackend.core.annotations.VerifySignature;
import net.magnesiumbackend.core.annotations.service.GeneratedRouteRegistrationClass;
import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.http.exceptions.BadRequestException;
import net.magnesiumbackend.processor.path.CompiledPathTemplate;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteRegistrationGenerator {
    private final Types    types;
    private final Filer    filer;
    private final Elements elements;
    private final Messager messager;

    private final TypeElement requestTypeElement;
    private final TypeElement responseTypeElement;
    private final TypeElement routeRegistryTypeElement;
    private final TypeElement throwableTypeElement;
    private final TypeElement wrongRequestTypeElement;
    private final TypeElement stringTypeElement;

    public RouteRegistrationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types    = types;
        this.filer    = filer;
        this.elements = elements;
        this.messager = messager;

        this.requestTypeElement       = elements.getTypeElement("net.magnesiumbackend.core.route.RequestContext");
        this.wrongRequestTypeElement  = elements.getTypeElement("net.magnesiumbackend.core.http.Request");
        this.responseTypeElement      = elements.getTypeElement("net.magnesiumbackend.core.http.ResponseEntity");
        this.routeRegistryTypeElement = elements.getTypeElement("net.magnesiumbackend.core.route.HttpRouteRegistry");
        this.throwableTypeElement     = elements.getTypeElement("java.lang.Throwable");
        this.stringTypeElement        = elements.getTypeElement("java.lang.String");
    }

    public String generate(TypeElement serviceClass) {
        TypeSpec.Builder builder = generateClass(serviceClass);
        if (builder == null) return null;

        String pkg = elements.getPackageOf(serviceClass).getQualifiedName().toString();
        String className = serviceClass.getSimpleName() + "_magnesium_RouteRegistration";

        writeClass(serviceClass, builder);

        return pkg + "." + className;
    }

    @Nullable
    private TypeSpec.Builder generateClass(TypeElement serviceClass) {
        String    proxyName           = serviceClass.getSimpleName() + "_magnesium_RouteRegistration";

        MethodSpec.Builder register = MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumApplication.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .addParameter(HttpRouteRegistry.class, "routeRegistry")
            .returns(void.class);

        String varName = "__magnesium_" + serviceClass.getSimpleName();

        CodeBlock codeBlock = buildInstantiation(serviceClass, varName);
        if (codeBlock == null) {
            return null;
        }

        register.addStatement("var jsonProvider = application.jsonProvider()");
        register.addCode(codeBlock);

        boolean anyRoute = false;

        for (Element element : serviceClass.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }

            ExecutableElement method = ((ExecutableElement) element);
            GetMapping getMappingRoute = method.getAnnotation(GetMapping.class);
            anyRoute |= processRoute(getMappingRoute,     method, "GET",     getMappingRoute != null ? getMappingRoute.path()         : null, register, varName);

            PostMapping postMappingRoute = method.getAnnotation(PostMapping.class);
            anyRoute |= processRoute(postMappingRoute,    method, "POST",    postMappingRoute != null ? postMappingRoute.path()       : null, register, varName);

            PutMapping putMappingRoute = method.getAnnotation(PutMapping.class);
            anyRoute |= processRoute(putMappingRoute,     method, "PUT",     putMappingRoute != null ? putMappingRoute.path()         : null, register, varName);

            DeleteMapping deleteMappingRoute = method.getAnnotation(DeleteMapping.class);
            anyRoute |= processRoute(deleteMappingRoute,  method, "DELETE",  deleteMappingRoute != null ? deleteMappingRoute.path()   : null, register, varName);

            PatchMappings patchMappingsRoute = method.getAnnotation(PatchMappings.class);
            anyRoute |= processRoute(patchMappingsRoute,   method, "PATCH",   patchMappingsRoute != null ? patchMappingsRoute.path()     : null, register, varName);

            HeadMapping headMappingRoute = method.getAnnotation(HeadMapping.class);
            anyRoute |= processRoute(headMappingRoute,    method, "HEAD",    headMappingRoute != null ? headMappingRoute.path()       : null, register, varName);

            OptionsMapping optionsMappingRoute = method.getAnnotation(OptionsMapping.class);
            anyRoute |= processRoute(optionsMappingRoute, method, "OPTIONS", optionsMappingRoute != null ? optionsMappingRoute.path() : null, register, varName);

            TracesMapping tracesMappingRoute = method.getAnnotation(TracesMapping.class);
            anyRoute |= processRoute(tracesMappingRoute, method, "TRACES", tracesMappingRoute != null ? tracesMappingRoute.path()     : null, register, varName);

            ConnectMapping connectMappingRoute = method.getAnnotation(ConnectMapping.class);
            anyRoute |= processRoute(connectMappingRoute, method, "CONNECT", connectMappingRoute != null ? connectMappingRoute.path() : null, register, varName);
        }

        if (!anyRoute) {
            return null;
        }

        return TypeSpec.classBuilder(proxyName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedRouteRegistrationClass.class)
            .addMethod(register.build());
    }

    private void writeClass(TypeElement originatingElement, TypeSpec.Builder classBuilder) {
        String   pkg      = elements.getPackageOf(originatingElement).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(pkg, classBuilder.build())
            .skipJavaLangImports(true)
            .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated route registration class: " + e.getMessage(), originatingElement);
        }
    }

    private boolean processRoute(
        Object annotation,
        ExecutableElement method,
        String httpMethod,
        String path,
        MethodSpec.Builder register,
        String varName
    ) {
        if (annotation == null) return false;

        if (path == null || path.isBlank()) {
            error("@" + httpMethod + "Route route must not be blank.", method);
            return false;
        }

        if (path.charAt(0) != '/') {
            error("@" + httpMethod + "Route route must start with '/' but got: \"" + path + "\".", method);
            return false;
        }

        List<? extends VariableElement> params = method.getParameters();

        if (!validateParams(method, httpMethod, params)) {
            return false;
        }

        TypeMirror returnType = method.getReturnType();
        boolean returnsVoid     = returnType.getKind() == TypeKind.VOID;
        boolean returnsResponse = types.isAssignable(returnType, responseTypeElement.asType());
        if (!returnsVoid && !returnsResponse && !isJsonConvertible(returnType)) {
            error("The return type of a @" + httpMethod + "Route method must be void, Response, "
                + "or a type that JsonProvider can serialise.", method);
            return false;
        }

        return processRouteAfterValidation(method, httpMethod, path, register, params, varName);
    }

    /**
     * Returns {@code true} if {@code type} is something {@link net.magnesiumbackend.core.json.JsonProvider}
     * can meaningfully serialize or deserialize at runtime.
     *
     * <p>The check is intentionally permissive: we accept any reference type that is
     * not a known framework type ({@code Request}, {@code Response}) and not a
     * primitive / array / void.  The JsonProvider implementation will throw a
     * {@code JsonException} at runtime if the type is genuinely unhandleable, we
     * don't try to replicate that logic at compile time.
     */
    private boolean isJsonConvertible(javax.lang.model.type.TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE,
                 ARRAY, VOID, NULL, NONE, ERROR, WILDCARD, UNION, INTERSECTION -> { return false; }
            default -> {
                if (types.isAssignable(type, requestTypeElement.asType()))  return false;
                return !types.isAssignable(type, responseTypeElement.asType());
            }
        }
    }

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

    private boolean isApplicationConfiguration(TypeMirror t) {
        Element el = types.asElement(t);
        if (!(el instanceof TypeElement te)) return false;
        return te.getAnnotation(ApplicationConfiguration.class) != null;
    }

    @Nullable
    private CodeBlock buildInstantiation(TypeElement handlerClass, String varName) {
        List<ExecutableElement> constructors = new ArrayList<>();
        for (Element e : handlerClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add((ExecutableElement) e);
            }
        }

        // Prefer the richest constructor who's every param looks like a service type
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
            if (isApplicationConfiguration(params.get(i).asType())) {
                args.add("application.configurationManager().get($T.class)", paramType);
            } else {
                args.add("serviceRegistry.get($T.class)", paramType);
            }
            if (i < params.size() - 1) args.add(", ");
        }

        return CodeBlock.builder()
            .addStatement("$T $N = new $T($L)", handlerClassName, varName, handlerClassName, args.build())
            .build();
    }

    private boolean processRouteAfterValidation(
        @NotNull ExecutableElement method,
        String httpMethod,
        String path,
        @NotNull MethodSpec.Builder register,
        @NotNull List<? extends VariableElement> params,
        String varName
    ) {
        TypeMirror      returnType = method.getReturnType();

        boolean isResponse = types.isAssignable(returnType, responseTypeElement.asType());

        CodeBlock lambda = createLambda(method, isResponse, varName);

        CompiledPathTemplate compiled = CompiledPathTemplate.compile(path);

        CodeBlock literalsArray = !isEmptyOrAllNull(compiled.literals())
            ? buildStringArray(List.of(compiled.literals()))
            : CodeBlock.of("new String[0]");

        CodeBlock varNamesArray = !isEmptyOrAllNull(compiled.varNames())
            ? buildStringArray(List.of(compiled.varNames()))
            : CodeBlock.of("new String[0]");

        CodeBlock template = CodeBlock.of(
            "$T.precompiled($S, $L, $L)",
            ClassName.get(RoutePathTemplate.class),
            path,
            literalsArray,
            varNamesArray
        );

        Requires methodRequires = method.getAnnotation(Requires.class);
        Requires classRequires  = method.getEnclosingElement().getAnnotation(Requires.class);
        Requires requires       = methodRequires != null ? methodRequires : classRequires;

        boolean methodAuthenticated = method.getAnnotation(Authenticated.class) != null;
        boolean classAuthenticated  = method.getEnclosingElement().getAnnotation(Authenticated.class) != null;
        boolean requiresAuth        = methodAuthenticated || classAuthenticated || requires != null;

        boolean methodVerifySignature = method.getAnnotation(VerifySignature.class) != null;
        boolean classVerifySignature  = method.getEnclosingElement().getAnnotation(VerifySignature.class) != null;
        boolean requiresSignature     = methodVerifySignature || classVerifySignature;

        List<CodeBlock> filterBlocks = new ArrayList<>();

        if (requires != null) {
            filterBlocks.add(CodeBlock.of(
                "new $T($L, $L, $T.$L)",
                ClassName.get("net.magnesiumbackend.core.auth", "AuthorizationFilter"),
                true,
                buildStringArray(List.of(requires.value())),
                ClassName.get("net.magnesiumbackend.core.annotations.enums", "RequiresMode"),
                requires.mode().name()
            ));
        } else if (requiresAuth) {
            filterBlocks.add(CodeBlock.of(
                "new $T($L, null, null)",
                ClassName.get("net.magnesiumbackend.core.auth", "AuthorizationFilter"),
                true
            ));
        }

        if (requiresSignature) {
            filterBlocks.add(CodeBlock.of(
                "new $T(application.requestSigningFilter())",
                ClassName.get("net.magnesiumbackend.core.security", "RequestSigningFilter")
            ));
        }

        // @RateLimit
        RateLimit methodRateLimit = method.getAnnotation(RateLimit.class);
        RateLimit classRateLimit  = method.getEnclosingElement().getAnnotation(RateLimit.class);
        RateLimit rateLimit       = methodRateLimit != null ? methodRateLimit : classRateLimit;

// @Idempotent
        Idempotent methodIdempotent = method.getAnnotation(Idempotent.class);
        Idempotent classIdempotent  = method.getEnclosingElement().getAnnotation(Idempotent.class);
        Idempotent idempotent       = methodIdempotent != null ? methodIdempotent : classIdempotent;

        if (rateLimit != null) {
            filterBlocks.add(CodeBlock.of(
                "new $T($T.builder()\n" +
                    "    .requests($L)\n" +
                    "    .window($T.ofSeconds($L))\n" +
                    "    .$L()\n" +
                    "    .build(),\n" +
                    "    $T.$L())",
                ClassName.get("net.magnesiumbackend.core.security", "RateLimiterFilter"),
                ClassName.get("net.magnesiumbackend.core.security", "RateLimiter"),
                rateLimit.requests(),
                ClassName.get("java.time", "Duration"),
                rateLimit.windowSeconds(),
                resolveAlgorithmMethod(rateLimit),
                ClassName.get("net.magnesiumbackend.core.security", "RateLimiterFilter", "KeyResolver"),
                resolveKeyResolverMethod(rateLimit)
            ));
        }

        if (idempotent != null) {
            filterBlocks.add(CodeBlock.of(
                "new $T(application.idempotencyStore(), $L)",
                ClassName.get("net.magnesiumbackend.core.security", "IdempotencyFilter"),
                idempotent.ttlHours()
            ));
        }

        CodeBlock filtersArg = filterBlocks.isEmpty()
            ? CodeBlock.of("$T.of()", ClassName.get(List.class))
            : CodeBlock.of("$T.of($L)", ClassName.get(List.class),
            filterBlocks.stream().collect(CodeBlock.joining(", ")));

        register.addCode(
            "routeRegistry.register($T.$L, $L, $L, $L);\n",
            ClassName.get(HttpMethod.class),
            httpMethod,
            template,
            lambda,
            filtersArg
        );

        return true;
    }

    private static CodeBlock buildStringArray(List<String> values) {
        CodeBlock.Builder builder = CodeBlock.builder().add("new String[] { ");
        for (int i = 0; i < values.size(); i++) {
            builder.add("$S", values.get(i));
            if (i < values.size() - 1) builder.add(", ");
        }
        return builder.add(" }").build();
    }

    private boolean isEmptyOrAllNull(String[] arr) {
        if (arr == null) return true;
        for (String s : arr) {
            if (s != null) return false;
        }
        return true;
    }

    private @NotNull CodeBlock createLambda(ExecutableElement method, boolean returnsResponse, String varName) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.add("request -> {\n");

        List<? extends VariableElement> params = method.getParameters();
        List<String> argNames = new ArrayList<>(params.size());

        int headerIndex = 0;
        int bodyIndex = 0;

        for (int i = 0; i < params.size(); i++) {
            VariableElement param = params.get(i);
            TypeMirror t = param.asType();
            TypeName tn = TypeName.get(t);
            String argName = "__magnesium_arg_" + i;

            if (types.isAssignable(t, requestTypeElement.asType())) {
                b.addStatement("$T $N = request", tn, argName);
                argNames.add(argName);
                continue;
            }

            RequestHeader header = param.getAnnotation(RequestHeader.class);
            if (header != null) {
                String rawName = "__magnesium_header_" + (headerIndex++);
                b.addStatement("String $N = request.header($S)", rawName, header.value());

                boolean hasDefault = header.defaultValue() != null && !header.defaultValue().isEmpty();
                if (header.required() && !hasDefault) {
                    b.beginControlFlow("if ($N == null)", rawName);
                    b.addStatement("throw new $T(\"Missing required header: \" + $S)", BadRequestException.class, header.value());
                    b.endControlFlow();
                } else if (hasDefault) {
                    b.beginControlFlow("if ($N == null)", rawName);
                    b.addStatement("$N = $S", rawName, header.defaultValue());
                    b.endControlFlow();
                }

                if (types.isAssignable(t, stringTypeElement.asType())) {
                    b.addStatement("$T $N = $N", tn, argName, rawName);
                } else {
                    HeaderParserKind kind = findHeaderParserKind(t);
                    if (kind == HeaderParserKind.STATIC_PARSE) {
                        b.addStatement("$T $N = $T.parse($N)", tn, argName, tn, rawName);
                    } else if (kind == HeaderParserKind.STRING_CTOR) {
                        b.addStatement("$T $N = new $T($N)", tn, argName, tn, rawName);
                    } else {
                        // Should have been caught by validation, keep generated code valid
                        b.addStatement("throw new $T($S)", BadRequestException.class, "Unsupported @RequestHeader type: " + tn);
                    }
                }

                argNames.add(argName);
                continue;
            }

            // body param
            String bodyVar = "__magnesium_body_" + (bodyIndex++);
            b.addStatement("$T $N = jsonProvider.fromRequest(request.request(), $T.class)", tn, bodyVar, tn);
            b.addStatement("$T $N = $N", tn, argName, bodyVar);
            argNames.add(argName);
        }

        CodeBlock.Builder args = CodeBlock.builder();
        for (int i = 0; i < argNames.size(); i++) {
            args.add("$N", argNames.get(i));
            if (i < argNames.size() - 1) args.add(", ");
        }

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            b.addStatement("$L.$L($L)", varName, method.getSimpleName(), args.build());
            b.addStatement("return $T.ok()", ClassName.get("net.magnesiumbackend.core.http", "ResponseEntity"));
        } else if (returnsResponse) {
            b.addStatement("return $L.$L($L)", varName, method.getSimpleName(), args.build());
        } else {
            b.addStatement("var result = $L.$L($L)", varName, method.getSimpleName(), args.build());
            b.addStatement("return result");
        }

        b.add("}\n");
        return b.build();
    }

    private enum HeaderParserKind {
        NONE,
        STATIC_PARSE,
        STRING_CTOR
    }

    private HeaderParserKind findHeaderParserKind(TypeMirror t) {
        Element el = types.asElement(t);
        if (!(el instanceof TypeElement te)) return HeaderParserKind.NONE;

        for (Element enclosed : te.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            if (!(enclosed instanceof ExecutableElement m)) continue;

            if (!m.getSimpleName().contentEquals("parse")) continue;
            if (!m.getModifiers().contains(Modifier.STATIC)) continue;
            if (m.getParameters().size() != 1) continue;
            if (!types.isAssignable(m.getParameters().getFirst().asType(), stringTypeElement.asType())) continue;

            if (types.isAssignable(m.getReturnType(), t)) {
                return HeaderParserKind.STATIC_PARSE;
            }
        }

        for (Element enclosed : te.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            if (!(enclosed instanceof ExecutableElement ctor)) continue;
            if (!ctor.getModifiers().contains(Modifier.PUBLIC)) continue;
            if (ctor.getParameters().size() != 1) continue;
            if (!types.isAssignable(ctor.getParameters().getFirst().asType(), stringTypeElement.asType())) continue;
            return HeaderParserKind.STRING_CTOR;
        }

        return HeaderParserKind.NONE;
    }

    private boolean validateParams(ExecutableElement method, String httpMethod, List<? extends VariableElement> params) {
        boolean seenBody = false;
        for (VariableElement param : params) {
            TypeMirror t = param.asType();

            if (types.isAssignable(t, wrongRequestTypeElement.asType())) {
                error("The code should use net.magnesiumbackend.core.route.RequestContext and not the internal net.magnesiumbackend.core.http.Request.", param);
                return false;
            }

            boolean isRequest = types.isAssignable(t, requestTypeElement.asType());
            if (isRequest) {
                continue;
            }

            RequestHeader header = param.getAnnotation(RequestHeader.class);
            if (header != null) {
                if (t.getKind().isPrimitive()) {
                    error("@RequestHeader parameters must not be primitive types.", param);
                    return false;
                }

                if (types.isAssignable(t, stringTypeElement.asType())) {
                    continue;
                }

                HeaderParserKind kind = findHeaderParserKind(t);
                if (kind == HeaderParserKind.NONE) {
                    error("@RequestHeader parameter type must be String, or declare either a static parse(String) method, or a public constructor taking a single String.", param);
                    return false;
                }

                continue;
            }

            // Un-annotated non-RequestContext param => treat as request body
            if (seenBody) {
                error("@" + httpMethod + "Route method may only have one request body parameter (non-RequestContext, without @RequestHeader).", param);
                return false;
            }
            if (!isJsonConvertible(t)) {
                error("A @" + httpMethod + "Route method parameter must be RequestContext, @RequestHeader, or a type JsonProvider can deserialize from the request body.", param);
                return false;
            }
            if (!isErasedToConcreteClassLiteral(t)) {
                error("Request body parameter type must be a concrete (non-generic) class.", param);
                return false;
            }
            seenBody = true;
        }

        return true;
    }

    private boolean isErasedToConcreteClassLiteral(TypeMirror t) {
        if (!(t instanceof DeclaredType declaredType)) return false;
        return declaredType.getTypeArguments().isEmpty();
    }

    private void error(String message, Element element) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private String resolveAlgorithmMethod(RateLimit rateLimit) {
        return switch (rateLimit.algorithm()) {
            case FIXED_WINDOW   -> "fixedWindow";
            case TOKEN_BUCKET   -> "tokenBucket";
            default             -> "slidingWindow";
        };
    }

    private String resolveKeyResolverMethod(RateLimit rateLimit) {
        return switch (rateLimit.keyResolver()) {
            case USER    -> "byUser";
            case API_KEY -> "byApiKey";
            default      -> "byIp";
        };
    }
}