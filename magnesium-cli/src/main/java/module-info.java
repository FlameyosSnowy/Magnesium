module magnesium.cli {
    requires org.tomlj;
    requires org.fusesource.jansi;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires java.net.http;
    requires java.xml;
    requires org.slf4j;

    exports net.magnesiumbackend.cli;
    exports net.magnesiumbackend.cli.commands;
    exports net.magnesiumbackend.cli.interactive;
    exports net.magnesiumbackend.cli.config;
}
