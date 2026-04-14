module magnesium.dsljson.json.provider {
    requires core;
    requires dsl.json;

    exports net.magnesiumbackend.json.dsljson;

    provides net.magnesiumbackend.core.json.JsonProvider
        with net.magnesiumbackend.json.dsljson.DslJsonProvider;
}