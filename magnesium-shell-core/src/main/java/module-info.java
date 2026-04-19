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
}
