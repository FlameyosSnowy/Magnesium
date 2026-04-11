package net.magnesiumbackend.transport.netty;

import net.magnesiumbackend.core.annotations.RequestHeader;
import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.annotations.GetMapping;
import net.magnesiumbackend.core.annotations.PostMapping;

import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

@RestController
public class OrderController {
    @PostMapping(path = "/orders")
    public ResponseEntity<String> placeOrder(RequestContext req) {
        System.out.println(req);
        return ResponseEntity.ok("Real order made wow!");
    }

    @GetMapping(path = "/ordersget")
    public ResponseEntity<String> getOrder(RequestContext req) {
        System.out.println(req);
        return ResponseEntity.ok("Real order wow!");
    }
}