package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.Subscribe;
import net.magnesiumbackend.core.annotations.enums.EventPriority;
import net.magnesiumbackend.core.annotations.service.GeneratedSubscriberClass;
import net.magnesiumbackend.core.services.ServiceRegistry;
import net.magnesiumbackend.core.event.SubscribeRegistry;
import net.magnesiumbackend.processor.event.EventInformation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a {@code <ListenerClass>_magnesium_Subscriber} for every class
 * that contains at least one {@code @Subscribe}-annotated method.
 *
 * <h2>Example output</h2>
 * Given:
 * <pre>{@code
 * public class UserEventListeners {
 *     private final AuditService audit;
 *     public UserEventListeners(AuditService audit) { this.audit = audit; }
 *
 *     @Subscribe(priority = EventPriority.HIGH, ignoresCancelled = false)
 *     public void onUserCreated(UserCreatedEvent event) { ... }
 *
 *     @Subscribe(priority = EventPriority.NORMAL, ignoresCancelled = true)
 *     public void onUserDeleted(UserDeletedEvent event) { ... }
 * }
 * }</pre>
 *
 * The processor emits:
 * <pre>{@code
 * public final class UserEventListeners_magnesium_Subscriber
 *         implements GeneratedSubscriberClass {
 *
 *     @Override
 *     public void register(MagnesiumRuntime application,
 *                          ServiceRegistry serviceRegistry,
 *                          SubscribeRegistry subscribeRegistry) {
 *         UserEventListeners __listener =
 *             new UserEventListeners(serviceRegistry.get(AuditService.class));
 *
 *         subscribeRegistry.register(UserCreatedEvent.class,
 *             EventPriority.HIGH, false,
 *             event -> __listener.onUserCreated(event));
 *
 *         subscribeRegistry.register(UserDeletedEvent.class,
 *             EventPriority.NORMAL, true,
 *             event -> __listener.onUserDeleted(event));
 *     }
 * }
 * }</pre>
 */
public class SubscribeRegistrationGenerator {

    private final Types    types;
    private final Filer    filer;
    private final Elements elements;
    private final Messager messager;

    // Pre-resolved framework types used for heuristic service-param detection
    private final TypeElement throwableTypeElement;
    private final TypeElement eventTypeElement;

    @Contract(pure = true)
    public SubscribeRegistrationGenerator(
        Types types, Filer filer, Elements elements, Messager messager
    ) {
        this.types    = types;
        this.filer    = filer;
        this.elements = elements;
        this.messager = messager;

        this.throwableTypeElement = elements.getTypeElement("java.lang.Throwable");
        this.eventTypeElement     = elements.getTypeElement("net.magnesiumbackend.core.event.Event");
    }

    public List<EventInformation> generate(TypeElement listenerClass) {
        TypeSpec.Builder classBuilder = generateClass(listenerClass);
        if (classBuilder == null) return List.of();

        writeClass(listenerClass, classBuilder);

        return collectEventInformation(listenerClass);
    }

    private List<EventInformation> collectEventInformation(TypeElement listenerClass) {
        String subscriberFqn = elements.getPackageOf(listenerClass).getQualifiedName()
            + "." + listenerClass.getSimpleName() + "_magnesium_Subscriber";

        List<EventInformation> result = new ArrayList<>();

        for (Element enclosed : listenerClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) enclosed;
            if (method.getAnnotation(Subscribe.class) == null) continue;

            List<? extends VariableElement> params = method.getParameters();
            if (params.size() != 1) continue; // already validated

            String eventType = params.getFirst().asType().toString();
            result.add(new EventInformation(subscriberFqn, eventType));
        }

        return result;
    }

    @Nullable
    private TypeSpec.Builder generateClass(TypeElement listenerClass) {
        MethodSpec.Builder register = MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumRuntime.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .addParameter(SubscribeRegistry.class, "subscribeRegistry")
            .returns(void.class);

        String listenerVar = "__listener";
        CodeBlock instantiation = buildInstantiation(listenerClass, listenerVar);
        if (instantiation == null) return null;
        register.addCode(instantiation);

        boolean anyMethod = false;

        for (Element enclosed : listenerClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method     = (ExecutableElement) enclosed;
            Subscribe         annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;

            List<? extends VariableElement> params = method.getParameters();
            if (params.size() != 1) {
                error("@Subscribe method must have exactly one parameter (the event type).", method);
                continue;
            }

            TypeMirror eventType = params.getFirst().asType();

            // Validate: the parameter must extend Event
            if (eventTypeElement != null &&
                !types.isAssignable(eventType, eventTypeElement.asType())) {
                error("@Subscribe method parameter must extend Event.", method);
                continue;
            }

            EventPriority priority        = annotation.priority();
            boolean       ignoresCancelled = annotation.ignoresCancelled();

            // subscribeRegistry.register(SomeEvent.class, EventPriority.HIGH, false,
            //     event -> __listener.onSomething((SomeEvent) event));
            register.addStatement(
                "$T.register($T.class, $T.$L, $L,\n    event -> $N.$N(($T) event))",
                ClassName.get(SubscribeRegistry.class),
                TypeName.get(eventType),
                ClassName.get(EventPriority.class),
                priority.name(),
                ignoresCancelled,
                listenerVar,
                method.getSimpleName(),
                TypeName.get(eventType)
            );

            anyMethod = true;
        }

        if (!anyMethod) return null;

        return buildCorrectClass(listenerClass);
    }

    /**
     * Builds the class correctly, using the {@code subscribeRegistry} parameter
     * name rather than the class reference as the invocation receiver.
     */
    @Nullable
    private TypeSpec.Builder buildCorrectClass(TypeElement listenerClass) {
        MethodSpec.Builder register = MethodSpec.methodBuilder("register")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumRuntime.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .addParameter(SubscribeRegistry.class, "subscribeRegistry")
            .returns(void.class);

        String listenerVar = "__listener";
        CodeBlock instantiation = buildInstantiation(listenerClass, listenerVar);
        if (instantiation == null) return null;
        register.addCode(instantiation);

        boolean anyMethod = false;

        for (Element enclosed : listenerClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method     = (ExecutableElement) enclosed;
            Subscribe         annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;

            List<? extends VariableElement> params = method.getParameters();
            if (params.size() != 1) continue; // already validated above

            TypeMirror    eventType       = params.getFirst().asType();
            EventPriority priority        = annotation.priority();
            boolean       ignoresCancelled = annotation.ignoresCancelled();

            // subscribeRegistry.register(SomeEvent.class, EventPriority.HIGH, false,
            //     event -> __listener.onSomething((SomeEvent) event));
            CodeBlock lambda = CodeBlock.of(
                "event -> $N.$N(($T) event)",
                listenerVar,
                method.getSimpleName(),
                TypeName.get(eventType)
            );

            register.addStatement(
                "subscribeRegistry.register($T.class, $T.$L, $L, $L)",
                TypeName.get(eventType),
                ClassName.get(EventPriority.class),
                priority.name(),
                ignoresCancelled,
                lambda
            );

            anyMethod = true;
        }

        if (!anyMethod) return null;

        String generatedName = listenerClass.getSimpleName() + "_magnesium_Subscriber";
        return TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(GeneratedSubscriberClass.class)
            .addMethod(register.build());
    }

    @Nullable
    private CodeBlock buildInstantiation(TypeElement listenerClass, String varName) {
        List<ExecutableElement> constructors = new ArrayList<>();
        for (Element e : listenerClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add((ExecutableElement) e);
            }
        }

        ExecutableElement chosen = null;
        for (ExecutableElement ctor : constructors) {
            if (ctor.getParameters().isEmpty()) {
                if (chosen == null) chosen = ctor;
                continue;
            }
            if (allParamsAreServices(ctor)) {
                chosen = ctor;
                break;
            }
        }

        if (chosen == null) {
            error("@Subscribe listener class must have a no-arg constructor or a constructor "
                    + "whose parameters are all service types resolvable from ServiceRegistry.",
                listenerClass);
            return null;
        }

        ClassName cls = ClassName.get(listenerClass);

        if (chosen.getParameters().isEmpty()) {
            return CodeBlock.builder()
                .addStatement("$T $N = new $T()", cls, varName, cls)
                .build();
        }

        CodeBlock.Builder args   = CodeBlock.builder();
        List<? extends VariableElement> params = chosen.getParameters();
        for (int i = 0; i < params.size(); i++) {
            TypeName paramType = TypeName.get(params.get(i).asType());
            args.add("serviceRegistry.get($T.class)", paramType);
            if (i < params.size() - 1) args.add(", ");
        }

        return CodeBlock.builder()
            .addStatement("$T $N = new $T($L)", cls, varName, cls, args.build())
            .build();
    }

    /**
     * Heuristic: a parameter is treated as a "service" if it is not a primitive,
     * not an array, not a Throwable subtype, and not a Event subtype.
     */
    private boolean allParamsAreServices(ExecutableElement ctor) {
        for (VariableElement param : ctor.getParameters()) {
            TypeMirror t = param.asType();
            switch (t.getKind()) {
                case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, ARRAY -> { return false; }
                default -> {
                    if (throwableTypeElement != null &&
                        types.isAssignable(t, throwableTypeElement.asType())) return false;
                    if (eventTypeElement != null &&
                        types.isAssignable(t, eventTypeElement.asType()))    return false;
                }
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // I/O
    // -------------------------------------------------------------------------

    private void writeClass(TypeElement originatingElement, TypeSpec.Builder classBuilder) {
        String   pkg      = elements.getPackageOf(originatingElement).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(pkg, classBuilder.build())
            .skipJavaLangImports(true)
            .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated subscriber class: " + e.getMessage(), originatingElement);
        }
    }

    private void error(String message, Element element) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}