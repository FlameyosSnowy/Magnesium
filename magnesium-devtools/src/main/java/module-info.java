module magnesium.devtools {
    requires core;
    requires java.compiler;
    requires org.jetbrains.annotations;
    requires org.slf4j;

    exports net.magnesiumbackend.devtools.debug;
    exports net.magnesiumbackend.devtools;

    provides net.magnesiumbackend.core.json.JsonProvider
        with net.magnesiumbackend.devtools.debug.ProfilingJsonProvider;
}