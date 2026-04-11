package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.annotations.Emit;
import net.magnesiumbackend.core.annotations.service.GeneratedEmitProxyClass;
import net.magnesiumbackend.core.event.EmitRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a {@code <OriginalClass>_magnesium_EmitProxy} for every class that
 * contains at least one {@code @Emit}-annotated method.
 *
 * <h2>Design</h2>
 * The proxy holds a reference to the original (delegate) instance and an
 * {@link EmitRegistry}.  It overrides every {@code @Emit} method by:
 * <ol>
 *   <li>Delegating to the original body (capturing its return value).</li>
 *   <li>Publishing the returned {@link net.magnesiumbackend.core.event.Event}
 *       to the {@code EmitRegistry}.</li>
 *   <li>Returning the event to the original caller.</li>
 * </ol>
 *
 * <p>Non-{@code @Emit} public methods are forwarded transparently to the delegate.
 *
 * <h2>Example output</h2>
 * Given:
 * <pre>{@code
 * public class OrderService {
 *     @Emit
 *     public OrderCreatedEvent createOrder(String item) { ... }
 *
 *     public void cancelOrder(long id) { ... }
 * }
 * }</pre>
 *
 * The processor emits:
 * <pre>{@code
 * public final class OrderService_magnesium_EmitProxy
 *         extends OrderService
 *         implements GeneratedEmitProxyClass {
 *
 *     private final OrderService __delegate;
 *     private final EmitRegistry __emitRegistry;
 *
 *     public OrderService_magnesium_EmitProxy(OrderService __delegate, EmitRegistry __emitRegistry) {
 *         this.__delegate    = __delegate;
 *         this.__emitRegistry = __emitRegistry;
 *     }
 *
 *     @Override
 *     public Object create(MagnesiumApplication application,
 *                          ServiceRegistry serviceRegistry,
 *                          EmitRegistry emitRegistry) {
 *         OrderService __instance = serviceRegistry.get(OrderService.class);
 *         return new OrderService_magnesium_EmitProxy(__instance, emitRegistry);
 *     }
 *
 *     @Override
 *     public OrderCreatedEvent createOrder(String item) {
 *         OrderCreatedEvent __event = __delegate.createOrder(item);
 *         __emitRegistry.publish(__event);
 *         return __event;
 *     }
 *
 *     @Override
 *     public void cancelOrder(long id) {
 *         __delegate.cancelOrder(id);
 *     }
 * }
 * }</pre>
 */
public class EmitRegistrationGenerator {

    private final Types    types;
    private final Filer    filer;
    private final Elements elements;
    private final Messager messager;

    private final TypeElement eventTypeElement;

    @Contract(pure = true)
    public EmitRegistrationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types    = types;
        this.filer    = filer;
        this.elements = elements;
        this.messager = messager;

        this.eventTypeElement = elements.getTypeElement("net.magnesiumbackend.core.event.Event");
    }

    public List<EventInformation> generate(TypeElement serviceClass) {
        TypeSpec.Builder classBuilder = generateClass(serviceClass);
        if (classBuilder == null) return List.of();

        writeClass(serviceClass, classBuilder);

        return collectEventInformation(serviceClass);
    }

    private List<EventInformation> collectEventInformation(TypeElement serviceClass) {
        String emitterFqn = elements.getPackageOf(serviceClass).getQualifiedName()
            + "." + serviceClass.getSimpleName() + "_magnesium_EmitProxy";

        List<EventInformation> result = new ArrayList<>();

        for (Element enclosed : serviceClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            ExecutableElement method = (ExecutableElement) enclosed;
            if (method.getAnnotation(Emit.class) == null) continue;

            TypeMirror returnType = method.getReturnType();
            if (returnType.getKind() == TypeKind.VOID) continue; // already validated

            result.add(new EventInformation(emitterFqn, returnType.toString()));
        }

        return result;
    }

    @Nullable
    private TypeSpec.Builder generateClass(TypeElement serviceClass) {
        ClassName serviceClassName = ClassName.get(serviceClass);
        String    proxyName        = serviceClass.getSimpleName() + "_magnesium_EmitProxy";

        // Fields
        FieldSpec delegateField = FieldSpec.builder(serviceClassName, "__delegate",
                Modifier.PRIVATE, Modifier.FINAL)
            .build();

        FieldSpec registryField = FieldSpec.builder(EmitRegistry.class, "__emitRegistry",
                Modifier.PRIVATE, Modifier.FINAL)
            .build();

        // Constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(serviceClassName, "__delegate")
            .addParameter(EmitRegistry.class, "__emitRegistry")
            .addStatement("this.__delegate = __delegate")
            .addStatement("this.__emitRegistry = __emitRegistry")
            .build();

        // GeneratedEmitProxyClass.create(...)
        MethodSpec create = buildCreateMethod(serviceClass, proxyName);

        TypeSpec.Builder builder = TypeSpec.classBuilder(proxyName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(serviceClassName)                       // extends OriginalClass
            .addSuperinterface(GeneratedEmitProxyClass.class)
            .addField(delegateField)
            .addField(registryField)
            .addMethod(constructor)
            .addMethod(create);

        boolean anyEmit = false;

        for (Element enclosed : serviceClass.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;

            if (method.getModifiers().contains(Modifier.PRIVATE) ||
                method.getModifiers().contains(Modifier.STATIC)) continue;

            Emit emitAnnotation = method.getAnnotation(Emit.class);

            MethodSpec override = emitAnnotation != null
                ? buildEmitOverride(method)
                : buildPassthroughOverride(method);

            if (override == null) continue;

            builder.addMethod(override);
            if (emitAnnotation != null) anyEmit = true;
        }

        if (!anyEmit) {
            return null;
        }

        MethodSpec serviceType = MethodSpec.methodBuilder("serviceType")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(Class.class))
            .addStatement("return $T.class", serviceClassName)
            .build();

        builder.addMethod(serviceType);

        return builder;
    }

    private MethodSpec buildCreateMethod(TypeElement serviceClass, String proxyName) {
        ClassName serviceClassName = ClassName.get(serviceClass);
        ClassName proxyClassName   = ClassName.bestGuess(
            elements.getPackageOf(serviceClass).getQualifiedName() + "." + proxyName);

        return MethodSpec.methodBuilder("create")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(MagnesiumApplication.class, "application")
            .addParameter(ServiceRegistry.class, "serviceRegistry")
            .addParameter(EmitRegistry.class, "emitRegistry")
            .returns(Object.class)
            .addStatement("$T __instance = serviceRegistry.get($T.class)",
                serviceClassName, serviceClassName)
            .addStatement("return new $T(__instance, emitRegistry)", proxyClassName)
            .build();
    }

    /**
     * Builds the override that captures the return value and publishes it:
     *
     * <pre>{@code
     * @Override
     * public OrderCreatedEvent createOrder(String item) {
     *     OrderCreatedEvent __event = __delegate.createOrder(item);
     *     __emitRegistry.publish(__event);
     *     return __event;
     * }
     * }</pre>
     */
    @Nullable
    private MethodSpec buildEmitOverride(ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();

        // @Emit only makes sense when the method returns a Event subtype
        if (returnType.getKind() == TypeKind.VOID) {
            error("@Emit method must return a Event subtype, not void.", method);
            return null;
        }
        if (eventTypeElement != null && !types.isAssignable(returnType, eventTypeElement.asType())) {
            error("@Emit method return type must extend Event.", method);
            return null;
        }

        MethodSpec.Builder mb = beginOverride(method);

        // Build the delegate call argument list
        CodeBlock delegateCall = buildDelegateCall(method);

        mb.addStatement("$T __event = $L", TypeName.get(returnType), delegateCall)
            .addStatement("__emitRegistry.publish(__event)")
            .addStatement("return __event");

        return mb.build();
    }

    /**
     * Builds a transparent delegation:
     *
     * <pre>{@code
     * @Override public void cancelOrder(long id) { __delegate.cancelOrder(id); }
     * // or
     * @Override public String getName() { return __delegate.getName(); }
     * }</pre>
     */
    private MethodSpec buildPassthroughOverride(ExecutableElement method) {
        MethodSpec.Builder mb = beginOverride(method);

        CodeBlock delegateCall = buildDelegateCall(method);

        if (method.getReturnType().getKind() == TypeKind.VOID) {
            mb.addStatement("$L", delegateCall);
        } else {
            mb.addStatement("return $L", delegateCall);
        }

        return mb.build();
    }

    /** Starts building a public @Override method mirroring the original signature. */
    private static MethodSpec.Builder beginOverride(ExecutableElement method) {
        MethodSpec.Builder mb = MethodSpec.methodBuilder(method.getSimpleName().toString())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.get(method.getReturnType()));

        for (VariableElement param : method.getParameters()) {
            mb.addParameter(TypeName.get(param.asType()), param.getSimpleName().toString());
        }

        return mb;
    }

    /**
     * Builds {@code __delegate.methodName(param1, param2, ...)} as a
     * {@link CodeBlock}
     */
    private static CodeBlock buildDelegateCall(ExecutableElement method) {
        CodeBlock.Builder args = CodeBlock.builder();
        List<? extends VariableElement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            args.add(params.get(i).getSimpleName().toString());
            if (i < params.size() - 1) args.add(", ");
        }
        return CodeBlock.of("__delegate.$N($L)", method.getSimpleName(), args.build());
    }

    private void writeClass(TypeElement originatingElement, TypeSpec.Builder classBuilder) {
        String   pkg      = elements.getPackageOf(originatingElement).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(pkg, classBuilder.build())
            .skipJavaLangImports(true)
            .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            error("Failed to write generated emit proxy class: " + e.getMessage(), originatingElement);
        }
    }

    private void error(String message, Element element) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}