package net.magnesiumbackend.processor.validation;

import net.magnesiumbackend.core.annotations.*;
import net.magnesiumbackend.core.annotations.QueryParam;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs advanced compile-time validations for route, security, and configuration correctness.
 *
 * <p>This validator detects issues that other frameworks (Spring Boot, Micronaut) only catch at runtime:</p>
 * <ul>
 *   <li>Route shadowing and duplication</li>
 *   <li>Missing authentication on sensitive endpoints</li>
 *   <li>HTTP/WebSocket path collisions</li>
 *   <li>Path variable naming inconsistencies</li>
 *   <li>Configuration key validation</li>
 * </ul>
 */
public class CompileTimeValidator {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{(\\w+)}");
    private static final Pattern VALIDATION_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)*$");

    private final Types types;
    private final Elements elements;
    private final Messager messager;

    private final Map<String, Set<RouteKey>> routesByMethod = new HashMap<>();
    private final Map<String, String> pathVariableNames = new HashMap<>();
    private final Set<String> webSocketPaths = new HashSet<>();
    private final Map<String, Set<String>> configKeysByClass = new HashMap<>();

    // Sensitive type detection
    private static final Set<String> sensitiveTypes = Set.of(
        "java.lang.String", // Could be password, token, etc.
        "java.util.UUID",   // Often API keys
        "byte[]",           // Could be secrets
        "java.security.PrivateKey",
        "java.security.PublicKey",
        "javax.crypto.SecretKey"
    );

    public CompileTimeValidator(Types types, Elements elements, Messager messager) {
        this.types = types;
        this.elements = elements;
        this.messager = messager;
    }

    /**
     * Registers an HTTP route for validation and performs immediate checks.
     *
     * @param method the HTTP method (GET, POST, etc.)
     * @param path the route path
     * @param methodElement the annotated method
     * @param controllerClass the containing controller class
     */
    public void registerRoute(String method, String path, ExecutableElement methodElement,
                               TypeElement controllerClass) {
        RouteKey key = new RouteKey(method, normalizePath(path));

        checkDuplicateRoute(method, path, methodElement, key);
        checkRouteShadowing(method, path, methodElement, key);
        checkPathVariableConsistency(method, path, methodElement);
        checkHttpWebSocketCollision(method, path, methodElement);
        checkMissingAuth(method, path, methodElement, controllerClass);

        routesByMethod.computeIfAbsent(method, k -> new HashSet<>()).add(key);
    }

    /**
     * Registers a WebSocket path for collision detection.
     */
    public void registerWebSocketPath(String path, ExecutableElement methodElement) {
        String normalized = normalizePath(path);

        for (Map.Entry<String, Set<RouteKey>> entry : routesByMethod.entrySet()) {
            for (RouteKey route : entry.getValue()) {
                if (pathsCollide(normalized, route.path())) {
                    error("WebSocket path '" + path + "' collides with HTTP " + entry.getKey()
                        + " route '" + route.path() + "'", methodElement);
                    return;
                }
            }
        }

        webSocketPaths.add(normalized);
    }

    private void checkDuplicateRoute(String method, String path, ExecutableElement element, RouteKey key) {
        Set<RouteKey> existing = routesByMethod.get(method);
        if (existing != null && existing.contains(key)) {
            error("Duplicate route: " + method + " " + path + " - already defined", element);
        }
    }

    private void checkRouteShadowing(String method, String path, ExecutableElement element, RouteKey newRoute) {
        Set<RouteKey> existingRoutes = routesByMethod.get(method);
        if (existingRoutes == null) return;

        for (RouteKey existing : existingRoutes) {
            if (isShadowing(newRoute.path(), existing.path())) {
                warning("Route '" + method + " " + path + "' shadows earlier route '"
                    + method + " " + existing.path() + "' - the earlier route will never match",
                    element);
            } else if (isShadowing(existing.path(), newRoute.path())) {
                error("Route '" + method + " " + path + "' is shadowed by earlier route '"
                    + method + " " + existing.path() + "' - this route will never match",
                    element);
            }
        }
    }

    /**
     * Checks if path A shadows path B (A is more generic and matches before B).
     */
    private boolean isShadowing(String pathA, String pathB) {
        String[] segmentsA = pathA.split("/");
        String[] segmentsB = pathB.split("/");

        int length = segmentsA.length;
        if (length != segmentsB.length) return false;

        boolean aIsMoreGeneric = false;

        for (int i = 0; i < length; i++) {
            String segA = segmentsA[i];
            String segB = segmentsB[i];

            boolean aIsParam = segA.startsWith("{");
            boolean bIsParam = segB.startsWith("{");

            if (!aIsParam && !bIsParam && !segA.equals(segB)) {
                return false;
            }

            if (aIsParam && !bIsParam) {
                aIsMoreGeneric = true;
            }
        }

        return aIsMoreGeneric;
    }

    private void checkPathVariableConsistency(String method, String path, ExecutableElement element) {
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String pathPrefix = extractPrefix(path, matcher.start());

            String existingVar = pathVariableNames.get(pathPrefix);
            if (existingVar != null && !existingVar.equals(varName)) {
                warning("Inconsistent path variable naming: '" + varName + "' vs '"
                    + existingVar + "' at similar position in different routes", element);
            } else if (existingVar == null) {
                pathVariableNames.put(pathPrefix, varName);
            }
        }

        validatePathParamsMatch(method, path, element);
    }

    private void validatePathParamsMatch(String method, String path, ExecutableElement element) {
        Set<String> pathVars = extractPathVariables(path);

        for (VariableElement param : element.getParameters()) {
            PathParam annotation = param.getAnnotation(PathParam.class);
            if (annotation != null) {
                String paramName = annotation.value().isEmpty()
                    ? param.getSimpleName().toString()
                    : annotation.value();

                if (!pathVars.contains(paramName)) {
                    error("@PathParam '" + paramName + "' does not exist in route path: " + path, param);
                }
            }
        }


        for (String pathVar : pathVars) {
            boolean used = false;
            for (VariableElement param : element.getParameters()) {
                PathParam annotation = param.getAnnotation(PathParam.class);
                if (annotation != null) {
                    String paramName = annotation.value().isEmpty()
                        ? param.getSimpleName().toString()
                        : annotation.value();
                    if (paramName.equals(pathVar)) {
                        used = true;
                        break;
                    }
                }
            }
            if (!used) {
                warning("Path variable '" + pathVar + "' in route is not bound to any method parameter", element);
            }
        }

        validateQueryParams(element);
    }

    private void validateQueryParams(ExecutableElement method) {
        Set<String> seenQueryParams = new HashSet<>();

        for (VariableElement param : method.getParameters()) {
            QueryParam queryParam = param.getAnnotation(QueryParam.class);
            if (queryParam == null) continue;

            String paramName = queryParam.value();
            if (paramName == null || paramName.isEmpty()) {
                error("@QueryParam must specify a parameter name", param);
                continue;
            }

            if (!seenQueryParams.add(paramName)) {
                error("Duplicate @QueryParam name: '" + paramName + "'", param);
            }

            TypeMirror paramType = param.asType();
            if (!isValidQueryParamType(paramType)) {
                error("@QueryParam '" + paramName + "' has unsupported type: " + paramType
                    + ". Supported types: String, int, long, boolean, double, UUID, or types with parse(String) or String constructor",
                    param);
            }

            if (queryParam.required() && !queryParam.defaultValue().isEmpty()) {
                warning("@QueryParam '" + paramName + "' is required=true but also has defaultValue - defaultValue will be ignored",
                    param);
            }

            if (paramName.contains(" ") || paramName.contains("/") || paramName.contains("?")) {
                error("@QueryParam name '" + paramName + "' contains invalid characters", param);
            }
        }
    }

    private boolean isValidQueryParamType(TypeMirror type) {
        if (type.getKind().isPrimitive()) return true;
        if (isPrimitiveOrWrapper(type)) return true;

        String name = type.toString();
        if (name.equals("java.lang.String")) return true;
        if (name.equals("java.util.UUID")) return true;

        TypeElement typeElement = (TypeElement) types.asElement(type);
        if (typeElement == null) return false;

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement m = (ExecutableElement) enclosed;
                if (m.getSimpleName().contentEquals("parse") &&
                    m.getModifiers().contains(Modifier.STATIC) &&
                    m.getParameters().size() == 1 &&
                    m.getParameters().getFirst().asType().toString().equals("java.lang.String")) {
                    return true;
                }
            }
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement ctor = (ExecutableElement) enclosed;
                if (ctor.getModifiers().contains(Modifier.PUBLIC) &&
                    ctor.getParameters().size() == 1 &&
                    ctor.getParameters().get(0).asType().toString().equals("java.lang.String")) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkHttpWebSocketCollision(String method, String path, ExecutableElement element) {
        String normalized = normalizePath(path);

        for (String wsPath : webSocketPaths) {
            if (pathsCollide(normalized, wsPath)) {
                error("HTTP route '" + method + " " + path + "' collides with WebSocket path '" + wsPath + "'",
                    element);
                return;
            }
        }
    }

    private void checkMissingAuth(String method, String path, ExecutableElement methodElement,
                                   TypeElement controllerClass) {
        boolean isReadOnly = method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS");

        boolean hasAuth = hasAuthentication(methodElement, controllerClass);
        boolean isAnonymous = hasAnnotation(methodElement, Anonymous.class)
            || hasAnnotation(controllerClass, Anonymous.class);

        if (!isReadOnly && !hasAuth && !isAnonymous) {
            if (isMutation(method)) {
                warning("Mutation endpoint '" + method + " " + path + "' lacks authentication. "
                    + "Add @Requires, @Secured, @Authenticated, or @Anonymous to suppress", methodElement);
            }
        }

        if (isAnonymous && returnsSensitiveData(methodElement)) {
            error("Endpoint returning potentially sensitive data is marked @Anonymous: "
                + method + " " + path, methodElement);
        }

        boolean classHasAuth = hasAuthentication(null, controllerClass);
        boolean methodExplicitlyAnonymous = hasAnnotation(methodElement, Anonymous.class);

        if (classHasAuth && methodExplicitlyAnonymous) {
            warning("Method overrides controller-level authentication with @Anonymous: "
                + method + " " + path, methodElement);
        }
    }

    /**
     * Validates that a configuration key exists and is used consistently.
     *
     * @param key the configuration key
     * @param element the element using the key
     * @param valueType the expected value type
     */
    public void validateConfigKey(String key, Element element, TypeMirror valueType) {
        if (!isValidConfigKey(key)) {
            error("Invalid configuration key format: '" + key + "'. "
                + "Keys must be lowercase, dot-separated, alphanumeric.", element);
            return;
        }

        String className = element.getEnclosingElement().toString();
        configKeysByClass.computeIfAbsent(className, k -> new HashSet<>()).add(key);

        if (key.contains("password") || key.contains("secret") || key.contains("token")) {
            if (!key.startsWith("secure.") && !key.startsWith("crypto.")) {
                warning("Sensitive configuration key '" + key + "' should be prefixed with 'secure.' or 'crypto.'",
                    element);
            }
        }
    }

    /**
     * Validates event emission schema.
     *
     * @param eventType the event type class
     * @param methodElement the emitting method
     */
    public void validateEventSchema(TypeMirror eventType, ExecutableElement methodElement) {
        TypeElement eventElement = (TypeElement) types.asElement(eventType);
        if (eventElement == null) return;

        boolean hasTimestamp = false;
        boolean hasId = false;

        for (Element enclosed : eventElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                String name = enclosed.getSimpleName().toString();
                if (name.equals("timestamp") || name.equals("occurredOn")) {
                    hasTimestamp = true;
                }
                if (name.equals("id") || name.equals("eventId")) {
                    hasId = true;
                }
            }
        }

        if (!hasTimestamp) {
            warning("Event type '" + eventElement.getSimpleName() + "' lacks timestamp field", methodElement);
        }
        if (!hasId) {
            warning("Event type '" + eventElement.getSimpleName() + "' lacks ID field", methodElement);
        }
    }

    /**
     * Validates that a type can be properly serialized/deserialized.
     * Detects non-serializable fields, cyclic graphs, missing constructors/accessors.
     *
     * @param type the type to validate
     * @param element the element context for error reporting
     */
    public void validateSerialization(TypeMirror type, Element element) {
        if (type == null) return;

        if (isPrimitiveOrWrapper(type)) return;
        String typeName = type.toString();
        if (typeName.startsWith("java.lang.") || typeName.startsWith("java.util.") ||
            typeName.startsWith("java.time.") || typeName.equals("java.lang.Object")) {
            return;
        }

        TypeElement typeElement = (TypeElement) types.asElement(type);
        if (typeElement == null) return;

        validateSerializableFields(typeElement, new HashSet<>(), element);

        Set<String> visited = new HashSet<>();
        visited.add(typeElement.getQualifiedName().toString());
        checkCyclicGraph(typeElement, visited, new ArrayList<>(), element);

        checkDefaultConstructor(typeElement, element);

        checkAccessors(typeElement, element);
    }

    private void validateSerializableFields(TypeElement typeElement, Set<String> visited, Element context) {
        if (!visited.add(typeElement.getQualifiedName().toString())) return;

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;

            VariableElement field = (VariableElement) enclosed;
            if (field.getModifiers().contains(Modifier.STATIC)) continue;
            if (field.getModifiers().contains(Modifier.TRANSIENT)) {
                boolean hasJsonIgnore = hasAnnotationByName(field, "JsonIgnore", "JsonIgnoreProperties", "JsonTransient");
                if (!hasJsonIgnore) {
                    warning("Transient field '" + field.getSimpleName() + "' in '"
                        + typeElement.getSimpleName() + "' may cause serialization issues - consider adding @JsonIgnore",
                        field);
                }
                continue;
            }

            TypeMirror fieldType = field.asType();

            if (!isSerializableType(fieldType)) {
                String typeStr = fieldType.toString();
                if (typeStr.contains("Thread") || typeStr.contains("InputStream") ||
                    typeStr.contains("OutputStream") || typeStr.contains("Connection") ||
                    typeStr.contains("Socket")) {
                    error("Field '" + field.getSimpleName() + "' in class '"
                        + typeElement.getSimpleName() + "' has non-serializable type: " + typeStr,
                        field);
                }
            }

            if (fieldType instanceof javax.lang.model.type.DeclaredType declaredType) {
                if (!declaredType.getTypeArguments().isEmpty()) {
                    for (TypeMirror arg : declaredType.getTypeArguments()) {
                        if (arg.toString().equals("java.lang.Object")) {
                            warning("Raw generic usage detected in field '"
                                + field.getSimpleName() + "' - consider specifying concrete type parameter",
                                field);
                        }
                    }
                }
            }
        }
    }

    private void checkCyclicGraph(TypeElement typeElement, Set<String> visited, List<String> path, Element context) {
        String typeName = typeElement.getQualifiedName().toString();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;

            VariableElement field = (VariableElement) enclosed;
            if (field.getModifiers().contains(Modifier.STATIC)) continue;
            if (field.getModifiers().contains(Modifier.TRANSIENT)) continue;

            TypeMirror fieldType = field.asType();
            TypeElement fieldElement = (TypeElement) types.asElement(fieldType);
            if (fieldElement == null) continue;

            String fieldTypeName = fieldElement.getQualifiedName().toString();

            if (isCollectionType(fieldType)) {
                if (fieldType instanceof javax.lang.model.type.DeclaredType declaredType) {
                    for (TypeMirror arg : declaredType.getTypeArguments()) {
                        TypeElement argElement = (TypeElement) types.asElement(arg);
                        if (argElement != null) {
                            String argName = argElement.getQualifiedName().toString();
                            if (visited.contains(argName)) {
                                error("Cyclic reference detected: " + String.join(" -> ", path)
                                    + " -> " + typeName + " -> (collection of) " + argName
                                    + " - field: " + field.getSimpleName(), field);
                            }
                        }
                    }
                }
                continue;
            }

            if (visited.contains(fieldTypeName)) {
                error("Cyclic reference detected in class '" + typeName + "': field '"
                    + field.getSimpleName() + "' references parent type '" + fieldTypeName
                    + "'. Consider using @JsonIgnore or @JsonManagedReference/@JsonBackReference",
                    field);
                continue;
            }

            if (!isPrimitiveOrWrapper(fieldType) && !isCollectionType(fieldType)) {
                path.add(typeName);
                Set<String> newVisited = new HashSet<>(visited);
                newVisited.add(fieldTypeName);
                checkCyclicGraph(fieldElement, newVisited, path, context);
                path.remove(path.size() - 1);
            }
        }
    }

    private void checkDefaultConstructor(TypeElement typeElement, Element context) {
        boolean hasDefaultCtor = false;
        boolean hasAnyCtor = false;

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;

            ExecutableElement ctor = (ExecutableElement) enclosed;
            hasAnyCtor = true;

            if (ctor.getParameters().isEmpty()) {
                hasDefaultCtor = true;
                break;
            }
        }

        if (!hasAnyCtor) return;

        if (!hasDefaultCtor) {
            boolean hasJsonCreator = hasAnnotationByName(typeElement, "JsonCreator", "AllArgsConstructor");
            if (!hasJsonCreator) {
                warning("Class '" + typeElement.getSimpleName()
                    + "' lacks a default constructor - may cause deserialization issues. "
                    + "Add default constructor or @JsonCreator/@AllArgsConstructor", typeElement);
            }
        }
    }

    private void checkAccessors(TypeElement typeElement, Element context) {
        Map<String, Boolean> fieldHasGetter = new HashMap<>();
        Map<String, Boolean> fieldHasSetter = new HashMap<>();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            VariableElement field = (VariableElement) enclosed;
            if (field.getModifiers().contains(Modifier.STATIC)) continue;

            String fieldName = field.getSimpleName().toString();

            fieldHasGetter.put(fieldName, false);
            fieldHasSetter.put(fieldName, false);
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) enclosed;
            String methodName = method.getSimpleName().toString();

            if (methodName.startsWith("get") && methodName.length() > 3) {
                String fieldName = decapitalize(methodName.substring(3));
                if (fieldHasGetter.containsKey(fieldName)) {
                    fieldHasGetter.put(fieldName, true);
                }
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                String fieldName = decapitalize(methodName.substring(2));
                if (fieldHasGetter.containsKey(fieldName)) {
                    fieldHasGetter.put(fieldName, true);
                }
            }

            if (methodName.startsWith("set") && methodName.length() > 3 && method.getParameters().size() == 1) {
                String fieldName = decapitalize(methodName.substring(3));
                if (fieldHasSetter.containsKey(fieldName)) {
                    fieldHasSetter.put(fieldName, true);
                }
            }
        }

        for (Map.Entry<String, Boolean> entry : fieldHasGetter.entrySet()) {
            String fieldName = entry.getKey();
            boolean hasGetter = entry.getValue();
            boolean hasSetter = fieldHasSetter.getOrDefault(fieldName, false);

            if (!hasGetter) {
                VariableElement field = findField(typeElement, fieldName);
                if (field != null && !field.getModifiers().contains(Modifier.PUBLIC)) {
                    warning("Field '" + fieldName + "' in class '"
                        + typeElement.getSimpleName() + "' has no getter - may not serialize correctly",
                        field);
                }
            }

            if (!hasSetter) {
                VariableElement field = findField(typeElement, fieldName);
                if (field != null) {
                    boolean isFinal = field.getModifiers().contains(Modifier.FINAL);
                    boolean hasCtorParam = hasConstructorParameter(typeElement, fieldName);

                    if (!isFinal && !hasCtorParam) {
                        warning("Field '" + fieldName + "' in class '"
                            + typeElement.getSimpleName()
                            + "' has no setter and is not final - deserialization may fail",
                            field);
                    }
                }
            }
        }
    }

    private VariableElement findField(TypeElement typeElement, String fieldName) {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD &&
                enclosed.getSimpleName().toString().equals(fieldName)) {
                return (VariableElement) enclosed;
            }
        }
        return null;
    }

    private boolean hasConstructorParameter(TypeElement typeElement, String fieldName) {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            ExecutableElement ctor = (ExecutableElement) enclosed;
            for (VariableElement param : ctor.getParameters()) {
                if (param.getSimpleName().toString().equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final Map<String, List<ExceptionHandlerInfo>> exceptionHandlersByClass = new HashMap<>();
    private final Set<String> handledExceptionTypes = new HashSet<>();

    /**
     * Registers an exception handler for validation.
     */
    public void registerExceptionHandler(TypeElement handlerClass, ExecutableElement method,
                                          TypeMirror exceptionType) {
        String exceptionTypeName = exceptionType.toString();
        String handlerClassName = handlerClass.getQualifiedName().toString();

        ExceptionHandlerInfo info = new ExceptionHandlerInfo(
            handlerClassName, method.getSimpleName().toString(), exceptionTypeName
        );

        exceptionHandlersByClass.computeIfAbsent(handlerClassName, k -> new ArrayList<>()).add(info);
        handledExceptionTypes.add(exceptionTypeName);

        checkShadowedExceptionHandlers(handlerClass, method, exceptionType);
    }

    /**
     * Validates that a method's declared exceptions are handled.
     */
    public void validateMethodExceptions(ExecutableElement method, TypeElement controllerClass) {
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        if (thrownTypes.isEmpty()) return;

        String methodName = controllerClass.getQualifiedName() + "." + method.getSimpleName();

        for (TypeMirror thrownType : thrownTypes) {
            String thrownTypeName = thrownType.toString();

            boolean isHandled = isExceptionHandled(thrownTypeName);

            TypeElement thrownElement = (TypeElement) types.asElement(thrownType);
            boolean isRuntime = isRuntimeException(thrownElement);

            if (!isHandled && !isRuntime) {
                warning("Checked exception '" + thrownTypeName + "' thrown by method '"
                    + methodName + "' is not handled by any @ExceptionHandler", method);
            }

            if (thrownTypeName.equals("java.lang.Exception") || thrownTypeName.equals("java.lang.Throwable")) {
                warning("Method '" + methodName + "' declares overly broad exception type: "
                    + thrownTypeName + " - consider using specific exception types", method);
            }
        }
    }

    /**
     * Checks for conflicting exception handlers across all handlers.
     */
    public void validateConflictingExceptionHandlers() {
        Map<String, Set<String>> exceptionHierarchy = new HashMap<>();

        for (List<ExceptionHandlerInfo> handlers : exceptionHandlersByClass.values()) {
            for (ExceptionHandlerInfo handler : handlers) {
                exceptionHierarchy.putIfAbsent(handler.exceptionType(), new HashSet<>());

                for (ExceptionHandlerInfo other : handlers) {
                    if (other == handler) continue;
                    if (isAssignableFrom(other.exceptionType(), handler.exceptionType())) {
                        exceptionHierarchy.get(handler.exceptionType()).add(other.exceptionType());
                    }
                }
            }
        }

        for (List<ExceptionHandlerInfo> handlers : exceptionHandlersByClass.values()) {
            for (ExceptionHandlerInfo handler : handlers) {
                Set<String> parents = exceptionHierarchy.getOrDefault(handler.exceptionType(), Set.of());
                if (!parents.isEmpty()) {
                    for (String parent : parents) {
                        warning("Exception handler for '" + handler.exceptionType() + "' in "
                            + handler.className + " may be shadowed by handler for '" + parent + "'",
                            null);
                    }
                }
            }
        }
    }

    private void checkShadowedExceptionHandlers(TypeElement handlerClass, ExecutableElement method,
                                                 TypeMirror exceptionType) {
        String newExceptionType = exceptionType.toString();

        for (Element enclosed : handlerClass.getEnclosedElements()) {
            if (enclosed == method) continue;
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExceptionHandler ann = enclosed.getAnnotation(ExceptionHandler.class);
            if (ann == null) continue;

            List<? extends VariableElement> params = ((ExecutableElement) enclosed).getParameters();
            if (params.isEmpty()) continue;

            TypeMirror handledTypeMirror = params.getFirst().asType();
            String handledType = handledTypeMirror.toString();

            if (isAssignableFrom(handledType, newExceptionType)) {
                error("Exception handler method '" + method.getSimpleName()
                    + "' is shadowed by existing handler for '" + handledType + "'",
                    method);
            }
        }
    }

    private boolean isExceptionHandled(String exceptionTypeName) {
        if (handledExceptionTypes.contains(exceptionTypeName)) return true;

        TypeElement exceptionElement = elements.getTypeElement(exceptionTypeName);
        if (exceptionElement == null) return false;

        TypeMirror superclass = exceptionElement.getSuperclass();
        while (superclass != null && !superclass.toString().equals("java.lang.Object")) {
            String superName = superclass.toString();
            if (handledExceptionTypes.contains(superName)) return true;

            TypeElement superElement = (TypeElement) types.asElement(superclass);
            if (superElement == null) break;
            superclass = superElement.getSuperclass();
        }

        return false;
    }

    private boolean isRuntimeException(TypeElement exceptionElement) {
        if (exceptionElement == null) return false;

        String name = exceptionElement.getQualifiedName().toString();
        if (name.equals("java.lang.RuntimeException")) return true;

        TypeMirror superclass = exceptionElement.getSuperclass();
        while (superclass != null && !superclass.toString().equals("java.lang.Object")) {
            if (superclass.toString().equals("java.lang.RuntimeException")) return true;

            TypeElement superElement = (TypeElement) types.asElement(superclass);
            if (superElement == null) break;
            superclass = superElement.getSuperclass();
        }

        return false;
    }

    private boolean isAssignableFrom(String parentType, String childType) {
        TypeElement parentElement = elements.getTypeElement(parentType);
        TypeElement childElement = elements.getTypeElement(childType);
        if (parentElement == null || childElement == null) return false;

        return types.isAssignable(childElement.asType(), parentElement.asType());
    }

    private final Map<String, Set<String>> filterDependencies = new HashMap<>();
    private final Map<String, Set<String>> filtersByRoute = new HashMap<>();

    /**
     * Validates a filter class for proper chain participation.
     */
    public void validateFilterClass(TypeElement filterClass) {
        boolean hasNoArgCtor = false;
        for (Element enclosed : filterClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            ExecutableElement ctor = (ExecutableElement) enclosed;
            if (ctor.getParameters().isEmpty()) {
                hasNoArgCtor = true;
                break;
            }
        }

        if (!hasNoArgCtor) {
            error("Filter class '" + filterClass.getSimpleName()
                + "' must have a public no-arg constructor for instantiation", filterClass);
        }

        boolean hasOrder = hasAnnotationByName(filterClass, "Order", "Priority");
        boolean implementsFilter = false;

        for (TypeMirror iface : filterClass.getInterfaces()) {
            if (iface.toString().contains("Filter")) {
                implementsFilter = true;
                break;
            }
        }

        for (AnnotationMirror ann : filterClass.getAnnotationMirrors()) {
            String annName = ann.getAnnotationType().toString();
            if (annName.contains("DependsOn") || annName.contains("Requires")) {
                Set<String> deps = extractFilterDependencies(ann);
                filterDependencies.put(filterClass.getQualifiedName().toString(), deps);
            }
        }
    }

    /**
     * Validates filter chain for a route.
     */
    public void validateFilterChain(String route, List<String> filters, Element context) {
        Set<String> seen = new HashSet<>();
        for (String filter : filters) {
            if (!seen.add(filter)) {
                warning("Filter '" + filter + "' appears multiple times in chain for route: " + route, context);
            }
        }

        String routeKey = route;
        filtersByRoute.put(routeKey, new LinkedHashSet<>(filters));

        for (String filter : filters) {
            Set<String> deps = filterDependencies.get(filter);
            if (deps != null) {
                for (String dep : deps) {
                    if (!filters.contains(dep)) {
                        error("Filter '" + filter + "' requires missing dependency '" + dep
                            + "' in chain for route: " + route, context);
                    }
                }
            }
        }
    }

    /**
     * Performs final cross-route filter validation.
     */
    public void validateFilterChains() {
        for (String filter : filterDependencies.keySet()) {
            Set<String> visited = new HashSet<>();
            List<String> path = new ArrayList<>();
            if (hasCircularDependency(filter, visited, path)) {
                error("Circular filter dependency detected: " + String.join(" -> ", path), null);
            }
        }

        Map<String, Integer> globalOrder = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : filtersByRoute.entrySet()) {
            int pos = 0;
            for (String filter : entry.getValue()) {
                Integer existingPos = globalOrder.get(filter);
                if (existingPos != null && existingPos != pos) {
                    warning("Inconsistent filter ordering: '" + filter + "' appears at position "
                        + existingPos + " in some routes and " + pos + " in others", null);
                }
                globalOrder.putIfAbsent(filter, pos);
                pos++;
            }
        }
    }

    private boolean hasCircularDependency(String filter, Set<String> visited, List<String> path) {
        if (!visited.add(filter)) {
            path.add(filter);
            return true;
        }

        path.add(filter);
        Set<String> deps = filterDependencies.get(filter);
        if (deps != null) {
            for (String dep : deps) {
                if (hasCircularDependency(dep, new HashSet<>(visited), new ArrayList<>(path))) {
                    return true;
                }
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    private Set<String> extractFilterDependencies(AnnotationMirror ann) {
        Set<String> deps = new HashSet<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : ann.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals("value")) {
                Object value = entry.getValue().getValue();
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof AnnotationValue av) {
                            deps.add(av.getValue().toString());
                        } else if (item instanceof String s) {
                            deps.add(s);
                        }
                    }
                } else if (value instanceof String s) {
                    deps.add(s);
                }
            }
        }
        return deps;
    }

    private boolean hasAuthentication(ExecutableElement methodElement, TypeElement controllerClass) {
        if (methodElement != null) {
            if (hasAnnotation(methodElement, Requires.class)) return true;
            if (hasAnnotation(methodElement, Authenticated.class)) return true;
        }

        if (hasAnnotation(controllerClass, Requires.class)) return true;
        return hasAnnotation(controllerClass, Authenticated.class);
    }

    private boolean isMutation(String method) {
        return method.equals("POST") || method.equals("PUT") || method.equals("DELETE")
            || method.equals("PATCH") || method.equals("CONNECT");
    }

    private boolean returnsSensitiveData(ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        String typeName = returnType.toString();

        // Direct sensitive types
        for (String sensitive : sensitiveTypes) {
            if (typeName.contains(sensitive)) return true;
            if (typeName.toLowerCase().contains("password")) return true;
            if (typeName.toLowerCase().contains("secret")) return true;
            if (typeName.toLowerCase().contains("token")) return true;
            if (typeName.toLowerCase().contains("credential")) return true;
        }

        if (returnType instanceof javax.lang.model.type.DeclaredType declaredType) {
            for (TypeMirror typeArg : declaredType.getTypeArguments()) {
                String argName = typeArg.toString();
                if (argName.toLowerCase().contains("password")) return true;
                if (argName.toLowerCase().contains("secret")) return true;
                if (argName.toLowerCase().contains("token")) return true;
            }
        }

        return false;
    }

    private boolean pathsCollide(String path1, String path2) {
        String[] segments1 = path1.split("/");
        String[] segments2 = path2.split("/");

        int length = segments1.length;
        if (length != segments2.length) return false;

        for (int i = 0; i < length; i++) {
            String s1 = segments1[i];
            String s2 = segments2[i];

            if (!s1.isEmpty() && s1.charAt(0) == '{' && s2.charAt(0) == '{' && !s1.equals(s2)) {
                return false;
            }
        }

        return true;
    }

    private Set<String> extractPathVariables(String path) {
        Set<String> vars = new HashSet<>();
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }

    private String extractPrefix(String path, int endIndex) {
        return path.substring(0, endIndex);
    }

    private String normalizePath(String path) {
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        return path.toLowerCase();
    }

    private boolean isValidConfigKey(String key) {
        if (key == null || key.isEmpty()) return false;
        return VALIDATION_PATTERN.matcher(key).matches();
    }

    private boolean hasAnnotation(Element element, Class<? extends Annotation> annotationClass) {
        return element.getAnnotation(annotationClass) != null;
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, "[Magnesium] " + message, element);
    }

    private void warning(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.WARNING, "[Magnesium] " + message, element);
    }

    private record RouteKey(String method, String path) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RouteKey(String method1, String path1))) return false;
            return method.equals(method1) && path.equals(path1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, path);
        }
    }

    private record ExceptionHandlerInfo(String className, String methodName, String exceptionType) {}

    private boolean isPrimitiveOrWrapper(TypeMirror type) {
        String name = type.toString();
        return type.getKind().isPrimitive() ||
            name.equals("java.lang.Boolean") ||
            name.equals("java.lang.Byte") ||
            name.equals("java.lang.Character") ||
            name.equals("java.lang.Double") ||
            name.equals("java.lang.Float") ||
            name.equals("java.lang.Integer") ||
            name.equals("java.lang.Long") ||
            name.equals("java.lang.Short");
    }

    private boolean isSerializableType(TypeMirror type) {
        if (isPrimitiveOrWrapper(type)) return true;
        String name = type.toString();
        if (name.startsWith("java.lang.") || name.startsWith("java.util.") ||
            name.startsWith("java.time.")) return true;
        return false;
    }

    private boolean isCollectionType(TypeMirror type) {
        String name = type.toString();
        return name.startsWith("java.util.List") ||
            name.startsWith("java.util.Set") ||
            name.startsWith("java.util.Map") ||
            name.startsWith("java.util.Collection") ||
            name.startsWith("java.util.ArrayList") ||
            name.startsWith("java.util.HashSet") ||
            name.startsWith("java.util.HashMap");
    }

    private boolean hasAnnotationByName(Element element, String... annotationNames) {
        for (AnnotationMirror ann : element.getAnnotationMirrors()) {
            String annName = ann.getAnnotationType().toString();
            for (String name : annotationNames) {
                if (annName.contains(name)) return true;
            }
        }
        return false;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
