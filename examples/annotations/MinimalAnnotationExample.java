import net.magnesiumbackend.core.Application;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.PathParam;
import net.magnesiumbackend.core.annotations.PostMapping;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.http.response.ResponseEntity;

/**
 * Minimal Annotation-Based Magnesium Application Example
 *
 * This demonstrates the simplest possible Magnesium application using
 * annotations for route definition. No explicit router configuration needed.
 */
@RestController
public class MinimalAnnotationExample extends Application {

    // 1. Simple GET route - returns plain text
    @GetMapping(path = "/")
    public String hello() {
        return "Hello, Magnesium!";
    }

    // 2. JSON response using ResponseEntity
    @GetMapping(path = "/json")
    public ResponseEntity<java.util.Map<String, String>> json() {
        return ResponseEntity.ok(java.util.Map.of("message", "Hello"));
    }

    // 3. Path parameter extraction
    @GetMapping(path = "/hello/{name}")
    public String helloName(@PathParam String name) {
        return "Hello, " + name + "!";
    }

    // 4. POST endpoint returning ResponseEntity
    @PostMapping(path = "/echo")
    public ResponseEntity<String> echo(String body) {
        return ResponseEntity.ok(body);
    }

    public static void main(String[] args) {
        // Controllers are auto-discovered and registered via annotation processing
        MagnesiumApplication.run(new MinimalAnnotationExample(), 8080);
    }
}
