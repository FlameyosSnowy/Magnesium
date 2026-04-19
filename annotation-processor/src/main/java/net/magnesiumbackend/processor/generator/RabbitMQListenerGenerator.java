package net.magnesiumbackend.processor.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import net.magnesiumbackend.amqp.QueueListenerInvoker;
import net.magnesiumbackend.amqp.RabbitMQService;
import net.magnesiumbackend.amqp.RabbitMQWiring;
import net.magnesiumbackend.amqp.QueueListenerRegistry;
import net.magnesiumbackend.amqp.annotations.QueueListener;
import net.magnesiumbackend.amqp.annotations.Exchange;
import net.magnesiumbackend.amqp.annotations.Binding;
import net.magnesiumbackend.amqp.annotations.Bindings;
import net.magnesiumbackend.amqp.annotations.DeadLetterQueue;
import net.magnesiumbackend.amqp.annotations.RabbitPublisher;
import net.magnesiumbackend.amqp.annotations.QueueArgument;
import net.magnesiumbackend.amqp.MessagePublisher;
import net.magnesiumbackend.core.json.JsonProvider;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates compile-time RabbitMQ listener wiring code.
 *
 * <p>Eliminates runtime reflection by generating explicit listener
 * registration code at compile time.</p>
 */
public final class RabbitMQListenerGenerator {

    private final Types types;
    private final Filer filer;
    private final Elements elements;
    private final Messager messager;
    private final Set<String> wiringRegistrations;

    public RabbitMQListenerGenerator(Types types, Filer filer, Elements elements, Messager messager, Set<String> wiringRegistrations) {
        this.types = types;
        this.filer = filer;
        this.elements = elements;
        this.messager = messager;
        this.wiringRegistrations = wiringRegistrations;
    }

    /**
     * Generates listener wiring for a class containing @QueueListener methods.
     *
     * @param containerClass the class with listener methods
     * @return generated class name or null
     */
    public String generate(TypeElement containerClass) {
        List<ListenerMethod> listeners = findListenerMethods(containerClass);
        List<PublisherField> publishers = findPublisherFields(containerClass);

        if (listeners.isEmpty() && publishers.isEmpty()) {
            return null;
        }

        String pkg = elements.getPackageOf(containerClass).getQualifiedName().toString();
        ClassName containerType = ClassName.get(containerClass);

        // Generate invoker classes for each listener method
        for (ListenerMethod listener : listeners) {
            TypeSpec invokerClass = generateInvokerClass(listener, containerType);
            String invokerName = containerClass.getSimpleName() + "_" + listener.method().getSimpleName() + "_Invoker";

            try {
                JavaFile.builder(pkg, invokerClass).skipJavaLangImports(true).build().writeTo(filer);
            } catch (IOException e) {
                error("Failed to write invoker class: " + e.getMessage(), containerClass);
                return null;
            }
        }

        TypeSpec wiringClass = buildWiringClass(containerClass, listeners, publishers);

        String generatedName = containerClass.getSimpleName() + "_RabbitMQWiring";

        try {
            JavaFile.builder(pkg, wiringClass).skipJavaLangImports(true).build().writeTo(filer);
        } catch (IOException e) {
            error("Failed to write RabbitMQ wiring: " + e.getMessage(), containerClass);
            return null;
        }

        String fullName = pkg + "." + generatedName;
        wiringRegistrations.add(fullName);
        return fullName;
    }

    private TypeSpec buildWiringClass(TypeElement containerClass, List<ListenerMethod> listeners, List<PublisherField> publishers) {
        String generatedName = containerClass.getSimpleName() + "_RabbitMQWiring";
        ClassName containerType = ClassName.get(containerClass);

        // Build the wire method (instance method implementing RabbitMQWiring)
        MethodSpec.Builder wireMethod = MethodSpec.methodBuilder("wire")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(RabbitMQService.class, "rabbitMQService")
            .addParameter(QueueListenerRegistry.class, "listenerRegistry")
            .addParameter(JsonProvider.class, "jsonProvider");

        // Create instance of the target class
        wireMethod.addStatement("$T instance = new $T()", containerType, containerType);

        // Add exchange declarations from class
        Exchange classExchange = containerClass.getAnnotation(Exchange.class);
        if (classExchange != null) {
            wireMethod.addStatement("rabbitMQService.declareExchange(new $T() {", net.magnesiumbackend.amqp.annotations.Exchange.class);
            wireMethod.addStatement("    public String name() { return $S; }", classExchange.name());
            wireMethod.addStatement("    public $T type() { return $T.$L; }", 
                net.magnesiumbackend.amqp.annotations.ExchangeType.class,
                net.magnesiumbackend.amqp.annotations.ExchangeType.class,
                classExchange.type().name());
            wireMethod.addStatement("    public boolean durable() { return $L; }", classExchange.durable());
            wireMethod.addStatement("    public boolean autoDelete() { return $L; }", classExchange.autoDelete());
            wireMethod.addStatement("    public boolean internal() { return $L; }", classExchange.internal());
            wireMethod.addStatement("    public String alternateExchange() { return $S; }", classExchange.alternateExchange());
            wireMethod.addStatement("    public Class<? extends java.lang.annotation.Annotation> annotationType() { return $T.class; }",
                net.magnesiumbackend.amqp.annotations.Exchange.class);
            wireMethod.addStatement("}");
        }

        // Process each listener method
        for (ListenerMethod listener : listeners) {
            generateListenerWiring(wireMethod, listener, containerType);
        }

        // Process each publisher field
        for (PublisherField publisher : publishers) {
            generatePublisherInjection(wireMethod, publisher, containerType);
        }

        return TypeSpec.classBuilder(generatedName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(RabbitMQWiring.class)
            .addMethod(wireMethod.build())
            .build();
    }

    private void generateListenerWiring(MethodSpec.Builder wireMethod, ListenerMethod listener, ClassName containerType) {
        ExecutableElement method = listener.method();
        QueueListener annotation = listener.annotation();

        String methodName = method.getSimpleName().toString();
        String queue = annotation.queue();

        // Build the listener lambda
        wireMethod.addStatement("");
        wireMethod.addStatement("// Register listener for queue: $S", queue);

        // Create adapter that deserializes and invokes the method
        wireMethod.addStatement("listenerRegistry.registerListener(");
        wireMethod.addStatement("    new $T() {", QueueListener.class);
        wireMethod.addStatement("        public String queue() { return $S; }", queue);
        wireMethod.addStatement("        public int concurrency() { return $L; }", annotation.concurrency());
        wireMethod.addStatement("        public boolean autoAck() { return $L; }", annotation.autoAck());
        wireMethod.addStatement("        public int maxRetries() { return $L; }", annotation.maxRetries());
        wireMethod.addStatement("        public long retryDelay() { return $LL; }", annotation.retryDelay());
        wireMethod.addStatement("        public boolean durable() { return $L; }", annotation.durable());
        wireMethod.addStatement("        public boolean exclusive() { return $L; }", annotation.exclusive());
        wireMethod.addStatement("        public boolean autoDelete() { return $L; }", annotation.autoDelete());
        wireMethod.addStatement("        public $T[] arguments() { return new $T[0]; }", QueueArgument.class, QueueArgument.class);
        wireMethod.addStatement("        public Class<? extends java.lang.annotation.Annotation> annotationType() { return $T.class; }", QueueListener.class);
        wireMethod.addStatement("    },");

        // DLQ if present
        DeadLetterQueue dlq = listener.dlq();
        if (dlq != null) {
            wireMethod.addStatement("    new $T() {", DeadLetterQueue.class);
            wireMethod.addStatement("        public String queue() { return $S; }", dlq.queue());
            wireMethod.addStatement("        public String exchange() { return $S; }", dlq.exchange());
            wireMethod.addStatement("        public String routingKey() { return $S; }", dlq.routingKey());
            wireMethod.addStatement("        public boolean durable() { return $L; }", dlq.durable());
            wireMethod.addStatement("        public long messageTtl() { return $LL; }", dlq.messageTtl());
            wireMethod.addStatement("        public long maxLength() { return $LL; }", dlq.maxLength());
            wireMethod.addStatement("        public Class<? extends java.lang.annotation.Annotation> annotationType() { return $T.class; }", DeadLetterQueue.class);
            wireMethod.addStatement("    },");
        } else {
            wireMethod.addStatement("    null,");
        }

        // Generate and use compile-time invoker instead of reflection
        wireMethod.addStatement("    new $L()", containerType.simpleName() + "_" + methodName + "_Invoker");
        wireMethod.addStatement(");");

        // Handle bindings
        Bindings bindings = method.getAnnotation(Bindings.class);
        Binding singleBinding = method.getAnnotation(Binding.class);

        if (bindings != null) {
            for (Binding binding : bindings.value()) {
                wireMethod.addStatement("rabbitMQService.bindQueue($S, $S, $S);",
                    queue, binding.exchange(), binding.routingKey());
            }
        } else if (singleBinding != null) {
            wireMethod.addStatement("rabbitMQService.bindQueue($S, $S, $S);",
                queue, singleBinding.exchange(), singleBinding.routingKey());
        }
    }

    private void generatePublisherInjection(MethodSpec.Builder wireMethod, PublisherField publisher, ClassName containerType) {
        String fieldName = publisher.field().getSimpleName().toString();
        RabbitPublisher annotation = publisher.annotation();

        // Get the message type from the generic parameter
        TypeName messageType = getPublisherMessageType(publisher.field());

        wireMethod.addStatement("");
        wireMethod.addStatement("// Inject publisher for field: $L", fieldName);

        wireMethod.addStatement("$T $L = rabbitMQService.createPublisher($S, $S, $T.class);",
            ParameterizedTypeName.get(ClassName.get(MessagePublisher.class), messageType),
            fieldName + "Publisher",
            annotation.exchange(),
            annotation.routingKey(),
            messageType);

        wireMethod.addStatement("instance.$L = $LPublisher;", fieldName, fieldName);
    }

    private String buildParameterTypes(ExecutableElement method) {
        StringBuilder sb = new StringBuilder();
        List<? extends VariableElement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            TypeMirror type = params.get(i).asType();
            sb.append(type.toString()).append(".class");
        }
        return sb.toString();
    }

    private TypeName getPublisherMessageType(VariableElement field) {
        try {
            DeclaredType declaredType = (DeclaredType) field.asType();
            List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return TypeName.get(typeArgs.getFirst());
            }
        } catch (Exception e) {
            // Fall through to Object
        }
        return ClassName.get(Object.class);
    }

    private List<ListenerMethod> findListenerMethods(TypeElement classElement) {
        List<ListenerMethod> listeners = new ArrayList<>();

        for (Element element : classElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                QueueListener listener = method.getAnnotation(QueueListener.class);
                if (listener != null) {
                    DeadLetterQueue dlq = method.getAnnotation(DeadLetterQueue.class);
                    listeners.add(new ListenerMethod(method, listener, dlq));
                }
            }
        }

        return listeners;
    }

    private List<PublisherField> findPublisherFields(TypeElement classElement) {
        List<PublisherField> publishers = new ArrayList<>();

        for (Element element : classElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) element;
                RabbitPublisher publisher = field.getAnnotation(RabbitPublisher.class);
                if (publisher != null) {
                    publishers.add(new PublisherField(field, publisher));
                }
            }
        }

        return publishers;
    }

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Generates a compile-time invoker class for a listener method.
     * This eliminates runtime reflection by generating direct method calls.
     */
    private TypeSpec generateInvokerClass(ListenerMethod listener, ClassName containerType) {
        ExecutableElement method = listener.method();
        String methodName = method.getSimpleName().toString();
        String queue = listener.annotation().queue();

        String invokerName = containerType.simpleName() + "_" + methodName + "_Invoker";

        // Create instance factory
        MethodSpec createInstance = MethodSpec.methodBuilder("createInstance")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(Object.class)
            .addParameter(javax.lang.model.element.Modifier.class.getDeclaringClass(), "ctx")
            .addStatement("return new $T()", containerType)
            .build();

        // Build invoke method with compile-time parameter resolution
        MethodSpec.Builder invokeBuilder = MethodSpec.methodBuilder("invoke")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addException(Exception.class)
            .addParameter(Object.class, "instance")
            .addParameter(com.rabbitmq.client.Delivery.class, "delivery")
            .addParameter(com.rabbitmq.client.Channel.class, "channel")
            .addParameter(net.magnesiumbackend.core.json.JsonProvider.class, "jsonProvider");

        // Cast instance to target type
        invokeBuilder.addStatement("$T target = ($T) instance", containerType, containerType);

        // Build parameter resolution code
        List<? extends VariableElement> params = method.getParameters();
        if (params.isEmpty()) {
            invokeBuilder.addStatement("target.$L()", methodName);
        } else {
            // Generate parameter resolution code
            for (int i = 0; i < params.size(); i++) {
                VariableElement param = params.get(i);
                TypeMirror paramType = param.asType();
                String paramName = "arg" + i;

                invokeBuilder.addCode(generateParameterResolution(param, paramType, paramName, i));
            }

            // Build the method call
            StringBuilder callBuilder = new StringBuilder();
            callBuilder.append("target.$L(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) callBuilder.append(", ");
                callBuilder.append("arg").append(i);
            }
            callBuilder.append(")");
            invokeBuilder.addStatement(callBuilder.toString(), methodName);
        }

        // Get queue
        MethodSpec getQueue = MethodSpec.methodBuilder("getQueue")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S", queue)
            .build();

        // Get target class name
        MethodSpec getTargetClass = MethodSpec.methodBuilder("getTargetClassName")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S", containerType.toString())
            .build();

        // Get method name
        MethodSpec getMethodName = MethodSpec.methodBuilder("getMethodName")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S", methodName)
            .build();

        return TypeSpec.classBuilder(invokerName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(QueueListenerInvoker.class)
            .addMethod(createInstance)
            .addMethod(invokeBuilder.build())
            .addMethod(getQueue)
            .addMethod(getTargetClass)
            .addMethod(getMethodName)
            .build();
    }

    /**
     * Generates parameter resolution code for a single parameter.
     */
    private com.palantir.javapoet.CodeBlock generateParameterResolution(
        VariableElement param,
        TypeMirror paramType,
        String paramName,
        int index
    ) {
        String typeName = paramType.toString();

        // Check for @RoutingKey annotation
        boolean hasRoutingKey = param.getAnnotation(
            net.magnesiumbackend.amqp.annotations.RoutingKey.class) != null;

        return switch (typeName) {
            case "com.rabbitmq.client.Delivery" ->
                com.palantir.javapoet.CodeBlock.of("$T $L = delivery;\n", com.rabbitmq.client.Delivery.class, paramName);
            case "com.rabbitmq.client.Envelope" ->
                com.palantir.javapoet.CodeBlock.of("$T $L = delivery.getEnvelope();\n", com.rabbitmq.client.Envelope.class, paramName);
            case "com.rabbitmq.client.AMQP.BasicProperties" ->
                com.palantir.javapoet.CodeBlock.of("$T $L = delivery.getProperties();\n", com.rabbitmq.client.AMQP.BasicProperties.class, paramName);
            case "com.rabbitmq.client.Channel" ->
                com.palantir.javapoet.CodeBlock.of("$T $L = channel;\n", com.rabbitmq.client.Channel.class, paramName);
            case "java.lang.String" -> {
                if (hasRoutingKey) {
                    yield com.palantir.javapoet.CodeBlock.of("$T $L = delivery.getEnvelope().getRoutingKey();\n", String.class, paramName);
                } else {
                    yield com.palantir.javapoet.CodeBlock.of("$T $L = new $T(delivery.getBody(), $T.UTF_8);\n",
                        String.class, paramName, String.class, java.nio.charset.StandardCharsets.class);
                }
            }
            case "byte[]" ->
                com.palantir.javapoet.CodeBlock.of("$T $L = delivery.getBody();\n", byte[].class, paramName);
            default -> // Complex type - deserialize from JSON
                com.palantir.javapoet.CodeBlock.builder()
                    .addStatement("$T $L = jsonProvider.fromJson(new $T(delivery.getBody(), $T.UTF_8), $T.class)",
                        Object.class, paramName, String.class, java.nio.charset.StandardCharsets.class, paramType)
                    .build();
        };
    }

    private record ListenerMethod(ExecutableElement method, QueueListener annotation, DeadLetterQueue dlq) {}
    private record PublisherField(VariableElement field, RabbitPublisher annotation) {}
}
