module magnesium.jackson.json.provider {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires core;

    exports net.magnesiumbackend.json.jackson;

    provides net.magnesiumbackend.core.json.JsonProvider
        with net.magnesiumbackend.json.jackson.JacksonJsonProvider;
}