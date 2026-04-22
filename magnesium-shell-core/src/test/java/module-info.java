module net.magnesiumbackend.shell.core.test {
    requires net.magnesiumbackend.shell.core;
    requires org.junit.jupiter.api;

    exports net.magnesiumbackend.shell.test.engine;
    exports net.magnesiumbackend.shell.test.ir;
    exports net.magnesiumbackend.shell.test.dsl;

    opens net.magnesiumbackend.shell.test.engine;
    opens net.magnesiumbackend.shell.test.ir;
    opens net.magnesiumbackend.shell.test.dsl;
    opens net.magnesiumbackend.shell.test.completion;
    opens net.magnesiumbackend.shell.test.input;
}