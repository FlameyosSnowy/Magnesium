module net.magnesiumbackend.jdbc {
    requires core;
    requires org.jetbrains.annotations;
    requires java.sql;
    requires com.zaxxer.hikari;

    exports net.magnesiumbackend.jdbc;
    exports net.magnesiumbackend.jdbc.streaming;

    // Service interfaces that extensions can implement
    uses net.magnesiumbackend.jdbc.DataSourceAware;

    provides net.magnesiumbackend.core.extensions.MagnesiumExtension
        with net.magnesiumbackend.jdbc.JdbcExtension;
}
