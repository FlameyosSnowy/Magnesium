import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.response.Response;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.Map;

public class MinimalApplicationExample extends Application {

    @Override
    protected void configure(MagnesiumRuntime runtime) {
        runtime.router()
            .get("/", ctx -> Response.ok("Hello, Magnesium!"))
            .get("/json", ctx -> Response.ok(Map.of("message", "Hello")))
            .get("/hello/{name}", ctx -> {
                String name = ctx.pathParam("name");
                return Response.ok("Hello, " + name + "!");
            })
            .post("/echo", ctx -> Response.ok(ctx.requestBody()));
    }

    public static void main(String[] args) {
        MagnesiumApplication.run(new MinimalApplicationExample(), 8080);
    }
}
