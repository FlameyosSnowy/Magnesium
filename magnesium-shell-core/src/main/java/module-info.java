module net.magnesiumbackend.shell.core {
    requires org.jetbrains.annotations;
    requires java.logging;
    requires core;

    exports net.magnesiumbackend.shell.annotation;
    exports net.magnesiumbackend.shell.dsl;
    exports net.magnesiumbackend.shell.engine;
    exports net.magnesiumbackend.shell.ir;
    exports net.magnesiumbackend.shell.completion;
    exports net.magnesiumbackend.shell.help;
    exports net.magnesiumbackend.shell.commands;
    exports net.magnesiumbackend.shell.input;

    opens net.magnesiumbackend.shell.ir;
    opens net.magnesiumbackend.shell.dsl;
    opens net.magnesiumbackend.shell.engine;
    opens net.magnesiumbackend.shell.completion;
    opens net.magnesiumbackend.shell.help;
    opens net.magnesiumbackend.shell.commands;
    opens net.magnesiumbackend.shell.input;
}
