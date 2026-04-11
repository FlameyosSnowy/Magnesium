import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.json.DslJsonProvider;

void main() {
    AtomicInteger integer = new AtomicInteger(1);
    MagnesiumApplication.builder()
        .http(http -> http.get("/health", (_) -> ResponseEntity.ok(integer.getAndIncrement() + "\n")))
        .json(new DslJsonProvider())
        .build()
        .run(8080);
}