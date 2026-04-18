package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.*;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.OnWebSocketClose;
import net.magnesiumbackend.core.annotations.OnWebSocketException;
import net.magnesiumbackend.core.annotations.OnWebSocketMessage;
import net.magnesiumbackend.core.annotations.OnWebSocketOpen;
import net.magnesiumbackend.core.annotations.service.GeneratedWebSocketRegistrationClass;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketMessage;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import net.magnesiumbackend.core.services.ServiceRegistry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

public class WebSocketRegistrationGenerator {

    private final Types types;
    private final Filer filer;
    private final Elements elements;
    private final Messager messager;

    private final TypeElement sessionTypeElement;
    private final TypeElement messageTypeElement;
    private final TypeElement throwableTypeElement;

    public WebSocketRegistrationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types = types;
        this.filer = filer;
        this.elements = elements;
        this.messager = messager;

        this.sessionTypeElement = elements.getTypeElement("net.magnesiumbackend.core.http.websocket.WebSocketSession");
        this.messageTypeElement = elements.getTypeElement("net.magnesiumbackend.core.http.websocket.WebSocketMessage");
        this.throwableTypeElement = elements.getTypeElement("java.lang.Throwable");
    }

    private enum WsLifecycle {
        OPEN, MESSAGE, CLOSE, ERROR
    }

    @Nullable
    public String generate(TypeElement controllerClass) {
        String proxyName = controllerClass.getSimpleName() + "_magnesium_WebSocketRegistration";
        String pkg = elements.getPackageOf(controllerClass).getQualifiedName().toString();
        String varName = "__magnesium_ws_" + controllerClass.getSimpleName();

        MethodSpec.Builder register = MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumRuntime.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .addParameter(WebSocketRouteRegistry.class, "routeRegistry")
            .returns(void.class);

        register.addStatement(
            "$T wsRegistry = application.httpServer().webSocketRouteRegistry()",
            WebSocketRouteRegistry.class
        );

        CodeBlock instantiation = buildInstantiation(controllerClass, varName);
        if (instantiation == null) return null;
        register.addCode(instantiation);

        Map<String, Map<WsLifecycle, ExecutableElement>> grouped = new HashMap<>();

        // Process @OnOpen, @OnMessage, @OnClose, @OnException annotations
        processStandaloneAnnotations(controllerClass, varName, grouped);

        if (grouped.isEmpty()) return null;

        for (var entry : grouped.entrySet()) {
            String path = entry.getKey();
            Map<WsLifecycle, ExecutableElement> methods = entry.getValue();

            CodeBlock handler = buildMergedHandler(methods, varName);

            register.addStatement("wsRegistry.register($S, $L)", path, handler);
        }

        TypeSpec typeSpec = TypeSpec.classBuilder(proxyName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedWebSocketRegistrationClass.class)
            .addMethod(register.build())
            .build();

        JavaFile javaFile = JavaFile.builder(pkg, typeSpec)
            .skipJavaLangImports(true)
            .build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "Failed to write WebSocket registration class: " + e.getMessage(), controllerClass);
            return null;
        }

        return pkg + "." + proxyName;
    }

    /**
     * Processes standalone WebSocket annotations (@OnOpen, @OnMessage, @OnClose, @OnException)
     * that work without @WebSocketMapping.
     */
    private void processStandaloneAnnotations(TypeElement controllerClass, String varName,
                                               Map<String, Map<WsLifecycle, ExecutableElement>> grouped) {
        String defaultPath = null;

        // Find if there's a path specified at class level (we'll use a convention)
        // For now, we require methods to have matching path context

        for (Element element : controllerClass.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) element;

            OnWebSocketOpen onWebSocketOpen = method.getAnnotation(OnWebSocketOpen.class);
            OnWebSocketMessage onWebSocketMessage = method.getAnnotation(OnWebSocketMessage.class);
            OnWebSocketClose onWebSocketClose = method.getAnnotation(OnWebSocketClose.class);
            OnWebSocketException onError = method.getAnnotation(OnWebSocketException.class);

            if (onWebSocketOpen == null && onWebSocketMessage == null && onWebSocketClose == null && onError == null) {
                continue;
            }

            if (!validateWebSocketMethod(method)) continue;

            // Determine lifecycle type and path
            WsLifecycle lifecycle = null;
            String path = null;

            if (onWebSocketOpen != null) {
                lifecycle = WsLifecycle.OPEN;
                path = onWebSocketOpen.path().isEmpty() ? "/" : onWebSocketOpen.path();
            } else if (onWebSocketMessage != null) {
                lifecycle = WsLifecycle.MESSAGE;
                path = onWebSocketMessage.path().isEmpty() ? "/" : onWebSocketMessage.path();
            } else if (onWebSocketClose != null) {
                lifecycle = WsLifecycle.CLOSE;
                path = onWebSocketClose.path().isEmpty() ? "/" : onWebSocketClose.path();
            } else {
                lifecycle = WsLifecycle.ERROR;
                path = onError.path().isEmpty() ? "/" : onError.path();
            }

            // Ensure path starts with /
            if (path.charAt(0) != '/') {
                path = "/" + path;
            }

            Map<WsLifecycle, ExecutableElement> map =
                grouped.computeIfAbsent(path, k -> new EnumMap<>(WsLifecycle.class));

            if (map.containsKey(lifecycle)) {
                error("Duplicate WebSocket lifecycle '" + lifecycle + "' for path: " + path, method);
                continue;
            }

            map.put(lifecycle, method);
        }
    }

    private WsLifecycle resolveLifecycle(List<? extends VariableElement> params) {
        if (params.size() == 1) return WsLifecycle.OPEN;

        TypeMirror second = params.get(1).asType();

        if (types.isAssignable(second, messageTypeElement.asType())) {
            return WsLifecycle.MESSAGE;
        }

        if (types.isAssignable(second, throwableTypeElement.asType())) {
            return WsLifecycle.ERROR;
        }

        return WsLifecycle.CLOSE;
    }

    private boolean validateWebSocketMethod(ExecutableElement method) {
        List<? extends VariableElement> params = method.getParameters();

        if (params.isEmpty()) {
            error("@WebSocket method must have at least WebSocketSession.", method);
            return false;
        }

        if (!types.isAssignable(params.get(0).asType(), sessionTypeElement.asType())) {
            error("First parameter must be WebSocketSession.", method);
            return false;
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            error("@WebSocket methods must return void.", method);
            return false;
        }

        return true;
    }

    private CodeBlock buildMergedHandler(
        Map<WsLifecycle, ExecutableElement> methods,
        String varName
    ) {
        TypeSpec.Builder anon = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(WebSocketHandler.class);

        if (methods.containsKey(WsLifecycle.OPEN)) {
            String name = methods.get(WsLifecycle.OPEN).getSimpleName().toString();

            anon.addMethod(MethodSpec.methodBuilder("onOpen")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebSocketSession.class, "session")
                .addStatement("$L.$L(session)", varName, name)
                .build());
        }

        if (methods.containsKey(WsLifecycle.MESSAGE)) {
            String name = methods.get(WsLifecycle.MESSAGE).getSimpleName().toString();

            anon.addMethod(MethodSpec.methodBuilder("onMessage")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebSocketSession.class, "session")
                .addParameter(WebSocketMessage.class, "message")
                .addStatement("$L.$L(session, message)", varName, name)
                .build());
        }

        if (methods.containsKey(WsLifecycle.CLOSE)) {
            String name = methods.get(WsLifecycle.CLOSE).getSimpleName().toString();

            anon.addMethod(MethodSpec.methodBuilder("onClose")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebSocketSession.class, "session")
                .addParameter(int.class, "statusCode")
                .addParameter(String.class, "reason")
                .addStatement("$L.$L(session, statusCode, reason)", varName, name)
                .build());
        }

        if (methods.containsKey(WsLifecycle.ERROR)) {
            String name = methods.get(WsLifecycle.ERROR).getSimpleName().toString();

            anon.addMethod(MethodSpec.methodBuilder("onError")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebSocketSession.class, "session")
                .addParameter(Throwable.class, "error")
                .addStatement("$L.$L(session, error)", varName, name)
                .build());
        }

        return CodeBlock.of("$L", anon.build());
    }

    @Nullable
    private CodeBlock buildInstantiation(TypeElement controllerClass, String varName) {
        List<ExecutableElement> constructors = new ArrayList<>();

        for (Element e : controllerClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add((ExecutableElement) e);
            }
        }

        ExecutableElement chosen = null;

        for (ExecutableElement ctor : constructors) {
            if (ctor.getParameters().isEmpty()) {
                chosen = ctor;
                continue;
            }

            if (allParamsAreServices(ctor)) {
                chosen = ctor;
                break;
            }
        }

        if (chosen == null) {
            error("No valid constructor found.", controllerClass);
            return null;
        }

        ClassName className = ClassName.get(controllerClass);

        if (chosen.getParameters().isEmpty()) {
            return CodeBlock.of("$T $N = new $T();\n", className, varName, className);
        }

        CodeBlock.Builder args = CodeBlock.builder();

        List<? extends VariableElement> params = chosen.getParameters();
        for (int i = 0; i < params.size(); i++) {
            args.add("serviceRegistry.get($T.class)", TypeName.get(params.get(i).asType()));
            if (i < params.size() - 1) args.add(", ");
        }

        return CodeBlock.of("$T $N = new $T($L);\n", className, varName, className, args.build());
    }

    private boolean allParamsAreServices(ExecutableElement ctor) {
        for (VariableElement param : ctor.getParameters()) {
            TypeMirror t = param.asType();

            switch (t.getKind()) {
                case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, ARRAY -> {
                    return false;
                }
            }

            if (types.isAssignable(t, throwableTypeElement.asType())) return false;
            if (types.isAssignable(t, sessionTypeElement.asType())) return false;
            if (types.isAssignable(t, messageTypeElement.asType())) return false;
        }

        return true;
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}