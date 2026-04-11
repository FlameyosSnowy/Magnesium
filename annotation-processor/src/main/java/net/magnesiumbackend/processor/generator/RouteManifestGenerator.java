package net.magnesiumbackend.processor.generator;

import net.magnesiumbackend.processor.event.EventInformation;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteManifestGenerator {

    private static final Pattern PATH_PARAM = Pattern.compile("\\{(\\w+)}");

    private static final List<String> HTTP_ANNOTATIONS = List.of(
        "net.magnesiumbackend.core.annotations.GetMapping",
        "net.magnesiumbackend.core.annotations.PostMapping",
        "net.magnesiumbackend.core.annotations.PutMapping",
        "net.magnesiumbackend.core.annotations.DeleteMapping",
        "net.magnesiumbackend.core.annotations.PatchMappings",
        "net.magnesiumbackend.core.annotations.HeadMapping",
        "net.magnesiumbackend.core.annotations.OptionsMapping",
        "net.magnesiumbackend.core.annotations.TracesMapping",
        "net.magnesiumbackend.core.annotations.ConnectMapping"
    );

    private static final Map<String, String> ANNOTATION_TO_METHOD = Map.of(
        "GetMapping",     "GET",
        "PostMapping",    "POST",
        "PutMapping",     "PUT",
        "DeleteMapping",  "DELETE",
        "PatchMappings",  "PATCH",
        "HeadMapping",    "HEAD",
        "OptionsMapping", "OPTIONS",
        "TracesMapping",  "TRACES",
        "ConnectMapping", "CONNECT"
    );

    private final Filer    filer;
    private final Elements elements;
    private final Types    types;
    private final Messager messager;

    private final List<RouteEntry>  routes = new ArrayList<>();
    private final List<EventEntry>  events = new ArrayList<>();

    public RouteManifestGenerator(Filer filer, Elements elements, Types types, Messager messager) {
        this.filer    = filer;
        this.elements = elements;
        this.types    = types;
        this.messager = messager;
    }

    public void collectRoutes(TypeElement controllerClass) {
        for (Element enclosed : controllerClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) enclosed;
            collectMethod(controllerClass, method);
        }
    }

    public void collectEvent(EventInformation info, boolean isEmitter) {
        for (EventEntry e : events) {
            if (e.event().equals(info.eventType())) {
                if (isEmitter) e.emitters().add(info.fqn());
                else           e.subscribers().add(info.fqn());
                return;
            }
        }
        EventEntry entry = new EventEntry(info.eventType(), new ArrayList<>(), new ArrayList<>());
        if (isEmitter) entry.emitters().add(info.fqn());
        else           entry.subscribers().add(info.fqn());
        events.add(entry);
    }

    private void collectMethod(TypeElement controller, ExecutableElement method) {
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            String simpleName = mirror.getAnnotationType()
                .asElement().getSimpleName().toString();

            String httpMethod = ANNOTATION_TO_METHOD.get(simpleName);
            if (httpMethod == null) continue;

            String path = extractPath(mirror);
            if (path == null) continue;

            routes.add(buildEntry(controller, method, httpMethod, path));
        }
    }

    private RouteEntry buildEntry(
        TypeElement controller,
        ExecutableElement method,
        String httpMethod,
        String path
    ) {
        String handler = controller.getQualifiedName() + "#" + method.getSimpleName();

        List<String> pathParams  = extractPathParams(path);
        List<QueryParam> queryParams = new ArrayList<>(); // populated if @QueryParam annotation exists
        BodyType requestBody     = resolveRequestBody(method);
        String responseType      = resolveResponseType(method);
        List<String> middleware  = resolveMiddleware(controller, method);
        List<String> tags        = resolveTags(controller, method);
        boolean deprecated       = method.getAnnotation(Deprecated.class) != null
                                || controller.getAnnotation(Deprecated.class) != null;

        return new RouteEntry(
            httpMethod, path, handler,
            pathParams, queryParams,
            requestBody, responseType,
            middleware, tags, deprecated
        );
    }

    public void write(String applicationName) {
        ManifestRoot root = new ManifestRoot(
            "1.0",
            Instant.now().toString(),
            applicationName,
            Collections.unmodifiableList(routes),
            Collections.unmodifiableList(events)
        );

        ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

        try {
            FileObject file = filer.createResource(
                StandardLocation.CLASS_OUTPUT, "", "routes.json"
            );
            try (Writer writer = file.openWriter()) {
                mapper.writeValue(writer, root);
            }
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Magnesium] routes.json written, " + routes.size() + " route(s), "
                + events.size() + " event type(s)");
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "[Magnesium] Failed to write routes.json: " + e.getMessage());
        }
    }

    private String extractPath(AnnotationMirror mirror) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e
                : mirror.getElementValues().entrySet()) {
            if (e.getKey().getSimpleName().contentEquals("path")) {
                return e.getValue().getValue().toString();
            }
        }
        return null;
    }

    private List<String> extractPathParams(String path) {
        Matcher match = PATH_PARAM.matcher(path);
        List<String> params = new ArrayList<>(match.groupCount());
        while (match.find()) params.add(match.group(1));
        return params;
    }

    private BodyType resolveRequestBody(ExecutableElement method) {
        TypeElement requestCtx = elements.getTypeElement(
            "net.magnesiumbackend.core.route.RequestContext");
        TypeElement responseEl = elements.getTypeElement(
            "net.magnesiumbackend.core.http.response.ResponseEntity");

        for (VariableElement param : method.getParameters()) {
            TypeMirror t = param.asType();
            if (t.getKind().isPrimitive()) continue;
            if (types.isAssignable(t, requestCtx.asType())) continue;
            if (types.isAssignable(t, responseEl.asType())) continue;
            if (param.getAnnotation(
                    loadAnnotation("net.magnesiumbackend.core.annotations.RequestHeader")) != null)
                continue;

            // Remaining param = request body
            return new BodyType(t.toString(), true);
        }
        return null;
    }

    private String resolveResponseType(ExecutableElement method) {
        TypeMirror ret = method.getReturnType();
        if (ret.getKind() == TypeKind.VOID) return "void";
        return ret.toString();
    }

    private List<String> resolveMiddleware(TypeElement controller, ExecutableElement method) {
        List<String> result = new ArrayList<>();

        collectFilterNames(controller.getAnnotationMirrors(), result);
        collectFilterNames(method.getAnnotationMirrors(), result);

        if (hasAnnotation(method, "net.magnesiumbackend.core.annotations.Authenticated")
         || hasAnnotation(controller, "net.magnesiumbackend.core.annotations.Authenticated")) {
            result.add("AuthorizationFilter");
        }
        if (hasAnnotation(method, "net.magnesiumbackend.core.annotations.VerifySignature")) {
            result.add("RequestSigningFilter");
        }

        return result;
    }

    private void collectFilterNames(
        List<? extends AnnotationMirror> mirrors,
        List<String> target
    ) {
        for (AnnotationMirror mirror : mirrors) {
            String name = mirror.getAnnotationType()
                .asElement().getSimpleName().toString();
            if (!name.equals("Filter") && !name.equals("Filters")) continue;

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e
                    : mirror.getElementValues().entrySet()) {
                if (!e.getKey().getSimpleName().contentEquals("value")) continue;

                Object val = e.getValue().getValue();
                if (val instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof AnnotationValue av) {
                            String fqn = av.getValue().toString();
                            target.add(simpleClassName(fqn));
                        }
                    }
                } else {
                    target.add(simpleClassName(val.toString()));
                }
            }
        }
    }

    private List<String> resolveTags(TypeElement controller, ExecutableElement method) {
        List<String> tags = new ArrayList<>();
        tags.add(controller.getSimpleName().toString()
            .replace("Controller", "")
            .replace("Handler", "")
            .toLowerCase());
        return tags;
    }

    private boolean hasAnnotation(Element element, String fqn) {
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(fqn)) return true;
        }
        return false;
    }

    private Class<? extends java.lang.annotation.Annotation> loadAnnotation(String fqn) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> cls =
                (Class<? extends java.lang.annotation.Annotation>) Class.forName(fqn);
            return cls;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private String simpleClassName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    public record ManifestRoot(
        String version,
        String generatedAt,
        String application,
        List<RouteEntry> routes,
        List<EventEntry> events
    ) {}

    public record RouteEntry(
        String         method,
        String         path,
        String         handler,
        List<String>   pathParams,
        List<QueryParam> queryParams,
        BodyType       requestBody,
        String         responseType,
        List<String>   middleware,
        List<String>   tags,
        boolean        deprecated
    ) {}

    public record QueryParam(
        String  name,
        String  type,
        boolean required
    ) {}

    public record BodyType(
        String  type,
        boolean required
    ) {}

    public record EventEntry(
        String       event,
        List<String> emitters,
        List<String> subscribers
    ) {}
}