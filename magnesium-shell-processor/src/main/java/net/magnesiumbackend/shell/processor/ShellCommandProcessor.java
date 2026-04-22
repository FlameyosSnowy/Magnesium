package net.magnesiumbackend.shell.processor;

import net.magnesiumbackend.shell.annotation.Arg;
import net.magnesiumbackend.shell.annotation.Command;
import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Compile-time annotation processor for shell commands.
 *
 * <p>Processes @Command-annotated classes and generates:</p>
 * <ul>
 *   <li>Typed argument records</li>
 *   <li>Command handlers</li>
 *   <li>Static command registry</li>
 *   <li>Completion tree</li>
 * </ul>
 *
 * <p>Zero runtime reflection is required.</p>
 */
@SupportedAnnotationTypes("net.magnesiumbackend.shell.annotation.Command")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ShellCommandProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    private final List<CommandIR> commands = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.messager = env.getMessager();
        this.filer = env.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateRegistry();
            return true;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@Command can only be applied to classes", element);
                continue;
            }

            TypeElement classElement = (TypeElement) element;
            CommandIR ir = processCommand(classElement);
            if (ir != null) {
                commands.add(ir);
                generateHandler(classElement, ir);
            }
        }

        return true;
    }

    private CommandIR processCommand(TypeElement classElement) {
        Command annotation = classElement.getAnnotation(Command.class);

        String className = classElement.getQualifiedName().toString();
        String handlerClass = className + "Handler";

        ArgumentSchema.Builder schemaBuilder = ArgumentSchema.builder();

        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                Arg argAnnotation = field.getAnnotation(Arg.class);
                if (argAnnotation != null) {
                    processArgument(field, argAnnotation, schemaBuilder);
                }
            }
        }

        return CommandIR.builder(annotation.value())
            .description(annotation.description())
            .arguments(schemaBuilder.build())
            .handlerClass(handlerClass)
            .handlerMethod("execute")
            .executionMode(annotation.mode())
            .amqpBinding(annotation.amqpBinding().isEmpty() ? null : annotation.amqpBinding())
            .build();
    }

    private void processArgument(VariableElement field, Arg annotation, ArgumentSchema.Builder builder) {
        String name = annotation.value().isEmpty() ? field.getSimpleName().toString() : annotation.value();
        Class<?> type = getBoxedType(field.asType().toString());

        Object defaultValue = null;
        if (!annotation.defaultValue().isEmpty()) {
            defaultValue = parseDefaultValue(type, annotation.defaultValue());
        }

        ArgumentSchema.ArgumentDef def = new ArgumentSchema.ArgumentDef(
            name,
            type,
            annotation.required(),
            defaultValue,
            annotation.description(),
            annotation.positional(),
            annotation.flag()
        );

        builder.addArgument(def);
    }

    private Class<?> getBoxedType(String typeName) {
        return switch (typeName) {
            case "int", "java.lang.Integer" -> Integer.class;
            case "long", "java.lang.Long" -> Long.class;
            case "boolean", "java.lang.Boolean" -> Boolean.class;
            case "double", "java.lang.Double" -> Double.class;
            case "float", "java.lang.Float" -> Float.class;
            default -> String.class;
        };
    }

    private Object parseDefaultValue(Class<?> type, String value) {
        try {
            if (type == Integer.class) return Integer.parseInt(value);
            if (type == Long.class) return Long.parseLong(value);
            if (type == Boolean.class) return Boolean.parseBoolean(value);
            if (type == Double.class) return Double.parseDouble(value);
            if (type == Float.class) return Float.parseFloat(value);
            return value;
        } catch (Exception e) {
            return value;
        }
    }

    private void generateHandler(TypeElement classElement, CommandIR ir) {
        String className = classElement.getQualifiedName().toString();
        String handlerClass = ir.handlerClass();
        String packageName = className.substring(0, className.lastIndexOf('.'));
        String simpleName = handlerClass.substring(handlerClass.lastIndexOf('.') + 1);

        try {
            JavaFileObject file = filer.createSourceFile(handlerClass, classElement);
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("import net.magnesiumbackend.shell.dsl.CommandContext;");
                out.println("import net.magnesiumbackend.shell.engine.GeneratedHandler;");
                out.println("import java.util.Map;");
                out.println();
                out.println("public class " + simpleName + " implements GeneratedHandler {");
                out.println();
                out.println("    @Override");
                out.println("    public void execute(CommandContext ctx) {");
                out.println("        // Instantiate original command class");
                out.println("        " + classElement.getSimpleName() + " command = new " + classElement.getSimpleName() + "();");
                out.println();
                out.println("        // Bind arguments");

                for (ArgumentSchema.ArgumentDef arg : ir.arguments().all().values()) {
                    String fieldName = arg.name();
                    out.println("        command." + fieldName + " = ctx.arg(\"" + fieldName + "\");");
                }

                out.println();
                out.println("        // Execute");
                out.println("        command.run();");
                out.println("    }");
                out.println("}");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate handler: " + e.getMessage(), classElement);
        }
    }

    private void generateRegistry() {
        if (commands.isEmpty()) return;

        String packageName = "net.magnesiumbackend.shell.generated";
        String className = "GeneratedCommandRegistry";

        try {
            JavaFileObject file = filer.createSourceFile(packageName + "." + className);
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("import net.magnesiumbackend.shell.ir.CommandIR;");
                out.println("import net.magnesiumbackend.shell.ir.ArgumentSchema;");
                out.println("import net.magnesiumbackend.shell.ir.ExecutionMode;");
                out.println("import net.magnesiumbackend.shell.dsl.CommandContext;");
                out.println("import net.magnesiumbackend.shell.engine.GeneratedHandler;");
                out.println("import java.util.Map;");
                out.println("import java.util.HashMap;");
                out.println();
                out.println("public final class " + className + " {");
                out.println();
                out.println("    private static final Map<String, CommandIR> COMMANDS = new HashMap<>();");
                out.println("    private static final Map<String, GeneratedHandler> HANDLERS = new HashMap<>();");
                out.println();
                out.println("    static {");

                for (CommandIR cmd : commands) {
                    out.println("        // " + cmd.name());
                    out.println("        COMMANDS.put(\"" + cmd.name() + "\", ");
                    out.println("            CommandIR.builder(\"" + cmd.name() + "\")");
                    out.println("                .description(\"" + escape(cmd.description()) + "\")");
                    out.println("                .executionMode(ExecutionMode." + cmd.executionMode() + ")");
                    if (cmd.amqpBinding() != null) {
                        out.println("                .amqpBinding(\"" + cmd.amqpBinding() + "\")");
                    }
                    out.println("                .handlerClass(\"" + cmd.handlerClass() + "\")");
                    out.println("                .handlerMethod(\"" + cmd.handlerMethod() + "\")");
                    out.println("                .arguments(build" + cmd.name().replace(":", "_") + "Args())");
                    out.println("                .build());");
                    out.println();

                    // Handler instantiation
                    String handlerSimple = cmd.handlerClass().substring(cmd.handlerClass().lastIndexOf('.') + 1);
                    out.println("        HANDLERS.put(\"" + cmd.name() + "\", new " + handlerSimple + "());");
                    out.println();
                }

                out.println("    }");
                out.println();
                out.println("    public static CommandIR getCommand(String name) {");
                out.println("        return COMMANDS.get(name);");
                out.println("    }");
                out.println();
                out.println("    public static GeneratedHandler getHandler(String name) {");
                out.println("        return HANDLERS.get(name);");
                out.println("    }");
                out.println();
                out.println("    public static Map<String, CommandIR> getAllCommands() {");
                out.println("        return Map.copyOf(COMMANDS);");
                out.println("    }");
                out.println();

                // Argument builders
                for (CommandIR cmd : commands) {
                    out.println("    private static ArgumentSchema build" + cmd.name().replace(":", "_") + "Args() {");
                    out.println("        ArgumentSchema.Builder builder = ArgumentSchema.builder();");

                    for (ArgumentSchema.ArgumentDef arg : cmd.arguments().all().values()) {
                        out.println("        builder.addArgument(new ArgumentSchema.ArgumentDef(");
                        out.println("            \"" + arg.name() + "\",");
                        out.println("            " + arg.type().getSimpleName() + ".class,");
                        out.println("            " + arg.required() + ",");
                        if (arg.defaultValue() != null) {
                            out.println("            \"" + arg.defaultValue() + "\",");
                        } else {
                            out.println("            null,");
                        }
                        out.println("            \"" + escape(arg.description()) + "\",");
                        out.println("            " + arg.positional() + ",");
                        out.println("            " + arg.flag() + "));");
                    }

                    out.println("        return builder.build();");
                    out.println("    }");
                    out.println();
                }

                out.println("}");
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate registry: " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
