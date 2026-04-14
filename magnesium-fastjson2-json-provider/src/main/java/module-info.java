module magnesium.fastjson2.json.provider {
    requires com.alibaba.fastjson2;
    requires core;

    exports net.magnesiumbackend.json.fastjson2;

    provides net.magnesiumbackend.core.json.JsonProvider
        with net.magnesiumbackend.json.fastjson2.FastJson2Provider;
}