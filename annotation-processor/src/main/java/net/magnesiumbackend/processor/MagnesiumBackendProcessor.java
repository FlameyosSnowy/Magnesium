package net.magnesiumbackend.processor;

import net.magnesiumbackend.core.annotations.*;

import net.magnesiumbackend.processor.event.EventInformation;
import net.magnesiumbackend.processor.generator.ApplicationPropertiesGenerator;
import net.magnesiumbackend.processor.generator.EmitRegistrationGenerator;
import net.magnesiumbackend.processor.generator.ApplicationConfigurationGenerator;
import net.magnesiumbackend.processor.generator.ExceptionHandlerRegistrationGenerator;
import net.magnesiumbackend.processor.generator.LifecycleRegistrationGenerator;
import net.magnesiumbackend.processor.generator.RabbitMQListenerGenerator;
import net.magnesiumbackend.processor.generator.RouteManifestGenerator;
import net.magnesiumbackend.processor.generator.RouteRegistrationGenerator;
import net.magnesiumbackend.processor.generator.SubscribeRegistrationGenerator;

import net.magnesiumbackend.processor.generator.WebSocketRegistrationGenerator;
import net.magnesiumbackend.processor.generator.ServiceAutoWireGenerator;
import net.magnesiumbackend.processor.validation.CompileTimeValidator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagnesiumBackendProcessor extends AbstractProcessor {
    private Messager messager;

    private RouteRegistrationGenerator            routeRegistrationGenerator;
    private ExceptionHandlerRegistrationGenerator exceptionHandlerRegistrationGenerator;
    private EmitRegistrationGenerator             emitRegistrationGenerator;
    private SubscribeRegistrationGenerator        subscribeRegistrationGenerator;
    private WebSocketRegistrationGenerator        webSocketRegistrationGenerator;
    private ApplicationConfigurationGenerator     applicationConfigurationGenerator;
    private ApplicationPropertiesGenerator applicationPropertiesGenerator;
    private LifecycleRegistrationGenerator        lifecycleRegistrationGenerator;
    private ServiceAutoWireGenerator serviceAutoWireGenerator;
    private RouteManifestGenerator manifestGenerator;
    private RabbitMQListenerGenerator rabbitMQListenerGenerator;
    private CompileTimeValidator validator;

    private final Set<String> wiringRegistrations = new HashSet<>(32);
    private final Set<String> routeRegistrations = new HashSet<>(32);
    private final Set<String> emitRegistrations = new HashSet<>(32);
    private final Set<String> subscribeRegistrations = new HashSet<>(32);
    private final Set<String> exceptionHandlerRegistrations = new HashSet<>(32);
    private final Set<String> webSocketRegistrations = new HashSet<>(32);
    private final Set<String> applicationConfigurationRegistrations = new HashSet<>(32);
    private final Set<String> applicationPropertiesRegistrations = new HashSet<>(32);
    private final Set<String> lifecycleRegistrations = new HashSet<>(32);
    private final Set<String> rabbitMQRegistrations = new HashSet<>(32);
    private final java.util.Map<String, String> customReturnResolvers = new java.util.HashMap<>(16);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();

        Types types = processingEnv.getTypeUtils();
        Elements elements = processingEnv.getElementUtils();
        Filer    filer    = processingEnv.getFiler();

        this.routeRegistrationGenerator           = new RouteRegistrationGenerator(types, filer, elements, messager);
        this.exceptionHandlerRegistrationGenerator = new ExceptionHandlerRegistrationGenerator(types, filer, elements, messager);
        this.emitRegistrationGenerator            = new EmitRegistrationGenerator(types, filer, elements, messager);
        this.webSocketRegistrationGenerator = new WebSocketRegistrationGenerator(types, filer, elements, messager);
        this.subscribeRegistrationGenerator       = new SubscribeRegistrationGenerator(types, filer, elements, messager);
        this.validator                            = new CompileTimeValidator(types, elements, messager);
        this.applicationConfigurationGenerator    = new ApplicationConfigurationGenerator(types, filer, elements, messager, validator);
        this.applicationPropertiesGenerator       = new ApplicationPropertiesGenerator(types, filer, elements, messager);
        this.lifecycleRegistrationGenerator       = new LifecycleRegistrationGenerator(types, filer, elements, messager);
        this.rabbitMQListenerGenerator            = new RabbitMQListenerGenerator(types, filer, elements, messager, wiringRegistrations);
        this.serviceAutoWireGenerator = new ServiceAutoWireGenerator(processingEnv);
        this.manifestGenerator = new RouteManifestGenerator(filer, elements, types, messager);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supported = new HashSet<>(64);
        // Core annotations
        supported.add(GetMapping.class.getCanonicalName());
        supported.add(PostMapping.class.getCanonicalName());
        supported.add(PutMapping.class.getCanonicalName());
        supported.add(PatchMappings.class.getCanonicalName());
        supported.add(DeleteMapping.class.getCanonicalName());
        supported.add(HeadMapping.class.getCanonicalName());
        supported.add(TracesMapping.class.getCanonicalName());
        supported.add(OptionsMapping.class.getCanonicalName());
        supported.add(ConnectMapping.class.getCanonicalName());
        supported.add(Emit.class.getCanonicalName());
        supported.add(Subscribe.class.getCanonicalName());
        supported.add(RestController.class.getCanonicalName());
        supported.add(ExceptionHandler.class.getCanonicalName());
        supported.add(Requires.class.getCanonicalName());
        supported.add(Authenticated.class.getCanonicalName());
        supported.add(ApplicationConfiguration.class.getCanonicalName());
        supported.add(Filters.class.getCanonicalName());
        supported.add(Filter.class.getCanonicalName());
        supported.add(VerifySignature.class.getCanonicalName());
        supported.add(Anonymous.class.getCanonicalName());
        supported.add(PathParam.class.getCanonicalName());
        supported.add(QueryParam.class.getCanonicalName());
        supported.add(Lifecycle.class.getCanonicalName());
        supported.add(RestService.class.getCanonicalName());
        supported.add(DependsOn.class.getCanonicalName());
        supported.add(Inject.class.getCanonicalName());
        supported.add(OnInitialize.class.getCanonicalName());
        supported.add(PreDestroy.class.getCanonicalName());
        supported.add(OnWebSocketOpen.class.getCanonicalName());
        supported.add(OnWebSocketMessage.class.getCanonicalName());
        supported.add(OnWebSocketClose.class.getCanonicalName());
        supported.add(OnWebSocketException.class.getCanonicalName());
        supported.add(ApplicationProperties.class.getCanonicalName());
        supported.add(ApplicationProperty.class.getCanonicalName());
        supported.add(ReturnResolverType.class.getCanonicalName());
        // RabbitMQ annotations
        supported.add(net.magnesiumbackend.amqp.annotations.QueueListener.class.getCanonicalName());
        supported.add(net.magnesiumbackend.amqp.annotations.Exchange.class.getCanonicalName());
        supported.add(net.magnesiumbackend.amqp.annotations.Binding.class.getCanonicalName());
        supported.add(net.magnesiumbackend.amqp.annotations.DeadLetterQueue.class.getCanonicalName());
        supported.add(net.magnesiumbackend.amqp.annotations.RabbitPublisher.class.getCanonicalName());
        return supported;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedRouteRegistrationClass",
                routeRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedEmitProxyClass",
                emitRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedExceptionHandlerClass",
                exceptionHandlerRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedSubscriberClass",
                subscribeRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedWebSocketRegistrationClass",
                webSocketRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.config.GeneratedConfigClass",
                applicationConfigurationRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.config.GeneratedConfigClass",
                applicationPropertiesRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.lifecycle.generated.GeneratedLifecycleClass",
                lifecycleRegistrations,
                processingEnv
            );
            writeServiceFile(
                "net.magnesiumbackend.core.lifecycle.generated.GeneratedLifecycleClass",
                wiringRegistrations,
                processingEnv
            );

            // Generate lifecycle metadata after all classes are processed
            String lifecycleGenerated = lifecycleRegistrationGenerator.generate();
            if (lifecycleGenerated != null) {
                lifecycleRegistrations.add(lifecycleGenerated);
            }

            validator.validateConflictingExceptionHandlers();
            validator.validateFilterChains();
        }

        processApplicationConfigurations(
            roundEnvironment.getElementsAnnotatedWith(ApplicationConfiguration.class)
        );

        processControllers(
            roundEnvironment.getElementsAnnotatedWith(RestController.class)
        );

        processExceptionHandlers(
            roundEnvironment.getElementsAnnotatedWith(ExceptionHandler.class)
        );

        processLifecycles(
            roundEnvironment.getElementsAnnotatedWith(Lifecycle.class)
        );

        processRestServices(
            roundEnvironment.getElementsAnnotatedWith(RestService.class)
        );

        processApplicationProperties(
            roundEnvironment.getElementsAnnotatedWith(ApplicationProperties.class)
        );

        processRabbitMQListeners(
            roundEnvironment.getElementsAnnotatedWith(net.magnesiumbackend.amqp.annotations.QueueListener.class)
        );

        processReturnResolvers(
            roundEnvironment.getElementsAnnotatedWith(ReturnResolverType.class)
        );

        return true;
    }

    private void processReturnResolvers(Set<? extends Element> resolvers) {
        for (Element resolver : resolvers) {
            if (!(resolver instanceof TypeElement typeElement)) continue;

            ReturnResolverType annotation = typeElement.getAnnotation(ReturnResolverType.class);
            if (annotation == null) continue;

            try {
                Class<?> resolvedType = annotation.value();
                customReturnResolvers.put(resolvedType.getCanonicalName(), typeElement.getQualifiedName().toString());
            } catch (MirroredTypeException e) {
                TypeMirror typeMirror = e.getTypeMirror();
                if (typeMirror instanceof javax.lang.model.type.DeclaredType declared) {
                    TypeElement typeEl = (TypeElement) declared.asElement();
                    customReturnResolvers.put(typeEl.getQualifiedName().toString(), typeElement.getQualifiedName().toString());
                }
            }
        }

        this.routeRegistrationGenerator.setCustomResolvers(customReturnResolvers);
    }

    private void processRestServices(Set<? extends Element> services) {
        for (Element service : services) {
            if (!(service instanceof TypeElement typeElement)) continue;

            // Validate RestService configuration
            validateRestService(typeElement);

            try {
                this.serviceAutoWireGenerator.generateServiceWiring(typeElement);
            } catch (IOException e) {
                this.messager.printMessage(
                    javax.tools.Diagnostic.Kind.ERROR,
                    "Failed to generate service wiring: " + e.getMessage(),
                    service
                );
            }
        }
    }

    /**
     * Validates @RestService configuration including:
     * - as() attribute points to an implemented interface
     * - @PreDestroy methods have correct signature
     * - Constructor dependencies are resolvable
     */
    private void validateRestService(TypeElement serviceElement) {
        RestService restService = serviceElement.getAnnotation(RestService.class);
        if (restService == null) return;

        // Validate as() attribute - class must implement the interface
        try {
            Class<?> asClass = restService.as();
            if (asClass != void.class) {
                // Check if service implements the specified interface
                boolean implementsInterface = false;
                for (TypeMirror iface : serviceElement.getInterfaces()) {
                    if (iface.toString().equals(asClass.getCanonicalName())) {
                        implementsInterface = true;
                        break;
                    }
                }
                if (!implementsInterface) {
                    error("Service does not implement interface specified in as(): " + asClass.getCanonicalName(),
                        serviceElement);
                }
            }
        } catch (MirroredTypeException e) {
            // Type mirror available at compile time - validate
            TypeMirror typeMirror = e.getTypeMirror();
            if (typeMirror.getKind() != TypeKind.VOID) {
                boolean implementsInterface = false;
                String targetType = typeMirror.toString();
                for (TypeMirror iface : serviceElement.getInterfaces()) {
                    if (iface.toString().equals(targetType)) {
                        implementsInterface = true;
                        break;
                    }
                }
                if (!implementsInterface) {
                    error("Service does not implement interface specified in as(): " + targetType,
                        serviceElement);
                }
            }
        }

        // Validate @PreDestroy methods
        for (Element element : serviceElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                PreDestroy preDestroy = element.getAnnotation(PreDestroy.class);
                if (preDestroy != null) {
                    ExecutableElement method = (ExecutableElement) element;

                    // Must be void return type
                    if (method.getReturnType().getKind() != TypeKind.VOID) {
                        error("@PreDestroy method must have void return type", method);
                    }

                    // Must have no parameters
                    if (!method.getParameters().isEmpty()) {
                        error("@PreDestroy method must have no parameters", method);
                    }

                    // Must not be private
                    if (method.getModifiers().contains(Modifier.PRIVATE)) {
                        error("@PreDestroy method must not be private", method);
                    }
                }

                // Validate @OnInitialize methods
                OnInitialize onInit = element.getAnnotation(OnInitialize.class);
                if (onInit != null) {
                    ExecutableElement method = (ExecutableElement) element;

                    if (method.getReturnType().getKind() != TypeKind.VOID) {
                        error("@OnInitialize method must have void return type", method);
                    }

                    if (!method.getParameters().isEmpty()) {
                        error("@OnInitialize method must have no parameters", method);
                    }

                    if (method.getModifiers().contains(Modifier.PRIVATE)) {
                        error("@OnInitialize method must not be private", method);
                    }
                }
            }
        }
    }

    private void processApplicationConfigurations(Set<? extends Element> configs) {
        for (Element config : configs) {
            if (!(config instanceof TypeElement typeElement)) continue;
            String generated = this.applicationConfigurationGenerator.generate(typeElement);
            if (generated != null) {
                this.applicationConfigurationRegistrations.add(generated);
            }
        }
    }

    private void processApplicationProperties(Set<? extends Element> properties) {
        for (Element props : properties) {
            if (!(props instanceof TypeElement typeElement)) continue;

            // Validate first
            this.applicationPropertiesGenerator.validate(typeElement);

            // Then generate
            String generated = this.applicationPropertiesGenerator.generate(typeElement);
            if (generated != null) {
                this.applicationPropertiesRegistrations.add(generated);
            }
        }
    }

    private void processRabbitMQListeners(Set<? extends Element> listeners) {
        for (Element listener : listeners) {
            // Get the enclosing class
            Element enclosing = listener.getEnclosingElement();
            if (!(enclosing instanceof TypeElement typeElement)) continue;

            String generated = this.rabbitMQListenerGenerator.generate(typeElement);
            if (generated != null) {
                this.rabbitMQRegistrations.add(generated);
            }
        }
    }

    private void processExceptionHandlers(Set<? extends Element> exceptionHandlers) {
        for (Element exceptionHandler : exceptionHandlers) {
            if (!(exceptionHandler instanceof TypeElement typeElement)) continue;
            String generatedExceptionHandler = this.exceptionHandlerRegistrationGenerator.generate(typeElement);
            if (generatedExceptionHandler != null) {
                this.exceptionHandlerRegistrations.add(generatedExceptionHandler);
            }
        }
    }

    private void processLifecycles(Set<? extends Element> lifecycles) {
        for (Element lifecycle : lifecycles) {
            if (!(lifecycle instanceof TypeElement typeElement)) continue;
            lifecycleRegistrationGenerator.processLifecycle(typeElement);
        }
    }

    private void processControllers(Set<? extends Element> controllers) {
        for (Element controller : controllers) {
            if (controller.getKind() != ElementKind.CLASS) continue;

            TypeElement typeElement = (TypeElement) controller;

            List<EventInformation> generatedSubscribes = this.subscribeRegistrationGenerator.generate(typeElement);
            generatedSubscribes.forEach((info) -> {
                subscribeRegistrations.add(info.fqn());
                manifestGenerator.collectEvent(info, false);
            });

            List<EventInformation> generatedEmits = this.emitRegistrationGenerator.generate(typeElement);
            generatedEmits.forEach((info) -> {
                emitRegistrations.add(info.fqn());
                manifestGenerator.collectEvent(info, true);
            });

            String generatedRoute = this.routeRegistrationGenerator.generate(typeElement);
            if (generatedRoute != null) {
                routeRegistrations.add(generatedRoute);
            }

            String generatedWebSocket = this.webSocketRegistrationGenerator.generate(typeElement);
            if (generatedWebSocket != null) {
                webSocketRegistrations.add(generatedWebSocket);
            }

            for (Element element : typeElement.getEnclosedElements()) {
                if (element.getKind() != ElementKind.METHOD) continue;
                ExecutableElement method = (ExecutableElement) element;
                validateNoConflictingAnnotations(method);
                validateRoutesAndWebSockets(typeElement, method);
                validateSerializationAndExceptions(typeElement, method);
            }

            manifestGenerator.collectRoutes(typeElement);
        }
    }

    /**
     * Validates serialization of request/response bodies and exception handling.
     */
    private void validateSerializationAndExceptions(TypeElement controllerClass, ExecutableElement method) {
        // Validate serialization of request body types
        for (VariableElement param : method.getParameters()) {
            // Skip request context, headers, etc.
            if (isRequestContext(param.asType())) continue;
            if (hasAnnotation(param, PathParam.class)) continue;
            if (hasAnnotation(param, RequestHeader.class)) continue;
            if (hasAnnotation(param, QueryParam.class)) continue;

            // Validate the body type for serialization
            validator.validateSerialization(param.asType(), param);
        }

        // Validate serialization of return type
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.VOID) {
            // Unwrap ResponseEntity if present
            TypeMirror actualReturnType = unwrapResponseEntity(returnType);
            if (actualReturnType != null) {
                validator.validateSerialization(actualReturnType, method);
            }
        }

        // Validate declared exceptions
        validator.validateMethodExceptions(method, controllerClass);
    }

    private boolean isRequestContext(TypeMirror type) {
        return type.toString().contains("RequestContext") ||
            type.toString().contains("HttpContext");
    }

    private boolean hasAnnotation(Element element, Class<? extends Annotation> annClass) {
        return element.getAnnotation(annClass) != null;
    }

    private TypeMirror unwrapResponseEntity(TypeMirror type) {
        String name = type.toString();
        if (name.contains("ResponseEntity") && type instanceof javax.lang.model.type.DeclaredType declared) {
            List<? extends TypeMirror> args = declared.getTypeArguments();
            if (!args.isEmpty()) {
                return args.getFirst();
            }
        }
        return type;
    }

    /**
     * Performs advanced compile-time validations for routes and WebSocket handlers.
     */
    private void validateRoutesAndWebSockets(TypeElement controllerClass, ExecutableElement method) {
        // Check HTTP route annotations
        validateHttpRouteAnnotation(GetMapping.class, method, controllerClass, "GET");
        validateHttpRouteAnnotation(PostMapping.class, method, controllerClass, "POST");
        validateHttpRouteAnnotation(PutMapping.class, method, controllerClass, "PUT");
        validateHttpRouteAnnotation(DeleteMapping.class, method, controllerClass, "DELETE");
        validateHttpRouteAnnotation(PatchMappings.class, method, controllerClass, "PATCH");
        validateHttpRouteAnnotation(HeadMapping.class, method, controllerClass, "HEAD");
        validateHttpRouteAnnotation(OptionsMapping.class, method, controllerClass, "OPTIONS");
        validateHttpRouteAnnotation(TracesMapping.class, method, controllerClass, "TRACES");
        validateHttpRouteAnnotation(ConnectMapping.class, method, controllerClass, "CONNECT");

        // Check WebSocket annotations
        validateWebSocketAnnotation(OnWebSocketOpen.class, method, controllerClass);
        validateWebSocketAnnotation(OnWebSocketMessage.class, method, controllerClass);
        validateWebSocketAnnotation(OnWebSocketClose.class, method, controllerClass);
        validateWebSocketAnnotation(OnWebSocketException.class, method, controllerClass);

        // Validate @Emit event schema
        Emit emit = method.getAnnotation(Emit.class);
        if (emit != null) {
            TypeMirror returnType = method.getReturnType();
            validator.validateEventSchema(returnType, method);
        }
    }

    private <T extends Annotation> void validateHttpRouteAnnotation(Class<T> annotationClass,
                                                                     ExecutableElement method,
                                                                     TypeElement controllerClass,
                                                                     String httpMethod) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) return;

        try {
            String path = (String) annotationClass.getMethod("path").invoke(annotation);
            validator.registerRoute(httpMethod, path, method, controllerClass);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    private List<String> extractFilterChain(ExecutableElement method, TypeElement controllerClass) {
        List<String> filters = new ArrayList<>(32);

        // Check method-level @Filter and @Filters
        Filter methodFilter = method.getAnnotation(Filter.class);
        if (methodFilter != null) {
            filters.add(methodFilter.value().getCanonicalName());
        }

        Filters methodFilters = method.getAnnotation(Filters.class);
        if (methodFilters != null) {
            for (Filter f : methodFilters.value()) {
                filters.add(f.value().getCanonicalName());
            }
        }

        // Check class-level @Filter and @Filters (for all methods)
        Filter classFilter = controllerClass.getAnnotation(Filter.class);
        if (classFilter != null) {
            filters.add(classFilter.value().getCanonicalName());
        }

        Filters classFilters = controllerClass.getAnnotation(Filters.class);
        if (classFilters != null) {
            for (Filter f : classFilters.value()) {
                filters.add(f.value().getCanonicalName());
            }
        }

        return filters;
    }

    private <T extends Annotation> void validateWebSocketAnnotation(Class<T> annotationClass,
                                                                     ExecutableElement method,
                                                                     TypeElement controllerClass) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) return;

        try {
            String path = (String) annotationClass.getMethod("path").invoke(annotation);
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            validator.registerWebSocketPath(path, method);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    // -------------------------------------------------------------------------

    private void validateNoConflictingAnnotations(ExecutableElement method) {
        int routeCount = countPresent(method,
            GetMapping.class, PostMapping.class, PutMapping.class,
            DeleteMapping.class, PatchMappings.class, HeadMapping.class, OptionsMapping.class, TracesMapping.class, ConnectMapping.class);

        boolean subscribe           = method.getAnnotation(Subscribe.class) != null;
        boolean hasExceptionHandler = method.getAnnotation(ExceptionHandler.class) != null;
        boolean hasOnOpen           = method.getAnnotation(OnWebSocketOpen.class) != null;
        boolean hasOnMessage        = method.getAnnotation(OnWebSocketMessage.class) != null;
        boolean hasOnClose          = method.getAnnotation(OnWebSocketClose.class) != null;
        boolean hasOnError          = method.getAnnotation(OnWebSocketException.class) != null;
        boolean hasWebSocket        = hasOnOpen || hasOnMessage || hasOnClose || hasOnError;
        boolean hasOnInitialize     = method.getAnnotation(OnInitialize.class) != null;
        boolean hasPreDestroy         = method.getAnnotation(PreDestroy.class) != null;

        if (routeCount > 1) {
            error("A method may only carry one @*Route annotation.", method);
        }

        if (routeCount > 0 && subscribe) {
            error("A method cannot mix @*Route and @Subscribe/@Emit annotations.", method);
        }

        if (hasWebSocket && (routeCount > 0 || subscribe)) {
            error("@WebSocket cannot be combined with @*Route or @Subscribe/@Emit annotations.", method);
        }

        if (hasWebSocket && (routeCount > 0 || subscribe)) {
            error("WebSocket annotations cannot be combined with @*Route or @Subscribe/@Emit annotations.", method);
        }

        if (hasOnInitialize && (routeCount > 0 || subscribe || hasExceptionHandler || hasPreDestroy)) {
            error("@OnInitialize cannot be combined with @*Route, @Subscribe/@Emit, @ExceptionHandler, or @PreDestroy annotations.", method);
        }

        if (hasPreDestroy && (routeCount > 0 || subscribe || hasExceptionHandler || hasOnInitialize || hasWebSocket)) {
            error("@PreDestroy cannot be combined with @*Route, @Subscribe/@Emit, @ExceptionHandler, @OnInitialize, or WebSocket annotations.", method);
        }

        if (hasExceptionHandler && (routeCount > 0 || subscribe || hasWebSocket)) {
            error("@ExceptionHandler cannot be combined with @*Route, @WebSocket, @Subscribe/@Emit, "
                + "or lifecycle annotations or be in a class not annotated with @ExceptionHandler.", method);
        }
    }

    public static void writeServiceFile(String service, Set<String> impls, ProcessingEnvironment processingEnv) {
        try {
            FileObject file = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/" + service
            );

            try (Writer writer = file.openWriter()) {
                for (String impl : impls) {
                    writer.write(impl);
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    private static int countPresent(ExecutableElement method, Class<? extends Annotation> @NotNull ... types) {
        int count = 0;
        for (Class<? extends Annotation> type : types) {
            if (method.getAnnotation(type) != null) count++;
        }
        return count;
    }

    private void error(String message, Element element) {
        this.messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, message, element);
    }
}