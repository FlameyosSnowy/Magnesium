package net.magnesiumbackend.processor;

import net.magnesiumbackend.core.annotations.*;

import net.magnesiumbackend.processor.event.EventInformation;
import net.magnesiumbackend.processor.generator.EmitRegistrationGenerator;
import net.magnesiumbackend.processor.generator.ApplicationConfigurationGenerator;
import net.magnesiumbackend.processor.generator.ExceptionHandlerRegistrationGenerator;
import net.magnesiumbackend.processor.generator.RouteManifestGenerator;
import net.magnesiumbackend.processor.generator.RouteRegistrationGenerator;
import net.magnesiumbackend.processor.generator.SubscribeRegistrationGenerator;

import net.magnesiumbackend.processor.generator.WebSocketRegistrationGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
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
    private RouteManifestGenerator manifestGenerator;

    private final Set<String> routeRegistrations = new HashSet<>(32);
    private final Set<String> emitRegistrations = new HashSet<>(32);
    private final Set<String> subscribeRegistrations = new HashSet<>(32);
    private final Set<String> exceptionHandlerRegistrations = new HashSet<>(32);
    private final Set<String> webSocketRegistrations = new HashSet<>(32);
    private final Set<String> applicationConfigurationRegistrations = new HashSet<>(32);

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
        this.applicationConfigurationGenerator    = new ApplicationConfigurationGenerator(types, filer, elements, messager);
        this.manifestGenerator = new RouteManifestGenerator(filer, elements, types, messager);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supported = new HashSet<>(8);
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
        supported.add(Secured.class.getCanonicalName());
        supported.add(Requires.class.getCanonicalName());
        supported.add(WebSocketMapping.class.getCanonicalName());
        supported.add(Authenticated.class.getCanonicalName());
        supported.add(ApplicationConfiguration.class.getCanonicalName());
        supported.add(Filters.class.getCanonicalName());
        supported.add(Filter.class.getCanonicalName());
        supported.add(VerifySignature.class.getCanonicalName());
        supported.add(Anonymous.class.getCanonicalName());
        supported.add(Async.class.getCanonicalName());
        supported.add(PathParam.class.getCanonicalName());
        return supported;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedRouteRegistrationClass",
                routeRegistrations
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedEmitProxyClass",
                emitRegistrations
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedExceptionHandlerClass",
                exceptionHandlerRegistrations
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedSubscriberClass",
                subscribeRegistrations
            );
            writeServiceFile(
                "net.magnesiumbackend.core.annotations.service.GeneratedWebSocketRegistrationClass",
                webSocketRegistrations
            );
            writeServiceFile(
                "net.magnesiumbackend.core.config.GeneratedConfigClass",
                applicationConfigurationRegistrations
            );
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

        return true;
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

    private void processExceptionHandlers(Set<? extends Element> exceptionHandlers) {
        for (Element exceptionHandler : exceptionHandlers) {
            if (!(exceptionHandler instanceof TypeElement typeElement)) continue;
            String generatedExceptionHandler = this.exceptionHandlerRegistrationGenerator.generate(typeElement);
            if (generatedExceptionHandler != null) {
                this.exceptionHandlerRegistrations.add(generatedExceptionHandler);
            }
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
                validateNoConflictingAnnotations((ExecutableElement) element);
            }

            manifestGenerator.collectRoutes(typeElement);
        }
    }

    // -------------------------------------------------------------------------

    private void validateNoConflictingAnnotations(ExecutableElement method) {
        int routeCount = countPresent(method,
            GetMapping.class, PostMapping.class, PutMapping.class,
            DeleteMapping.class, PatchMappings.class, HeadMapping.class, OptionsMapping.class, TracesMapping.class, ConnectMapping.class);

        boolean subscribe           = method.getAnnotation(Subscribe.class) != null;
        boolean hasExceptionHandler = method.getAnnotation(ExceptionHandler.class) != null;
        boolean hasWebSocket        = method.getAnnotation(WebSocketMapping.class) != null;

        if (routeCount > 1) {
            error("A method may only carry one @*Route annotation.", method);
        }

        if (routeCount > 0 && subscribe) {
            error("A method cannot mix @*Route and @Subscribe/@Emit annotations.", method);
        }

        if (hasWebSocket && (routeCount > 0 || subscribe)) {
            error("@WebSocket cannot be combined with @*Route or @Subscribe/@Emit annotations.", method);
        }

        if (hasExceptionHandler && (routeCount > 0 || subscribe || hasWebSocket)) {
            error("@ExceptionHandler cannot be combined with @*Route, @WebSocket, or @Subscribe/@Emit "
                + "or be in a class not annotated with @ExceptionHandler.", method);
        }
    }

    private void writeServiceFile(String service, Set<String> impls) {
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