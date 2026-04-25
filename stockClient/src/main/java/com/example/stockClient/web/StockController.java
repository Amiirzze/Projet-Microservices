package com.example.stockClient.web;

import com.example.lib.ProductReply;
import com.example.stockClient.service.StockGrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class StockController {

    @Value("${stockServerURL}")
    String stockServerURL;

    private final StockGrpcService stockGrpcService;

    @Autowired
    public StockController(StockGrpcService stockGrpcService) {
        this.stockGrpcService = stockGrpcService;
    }

    
    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String index() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(stockServerURL, String.class);
            return "Stock Client (Service 1) => Stock Server (Service 2) repond : " + response;
        } catch (Exception e) {
            return "Stock Client actif. Erreur connexion Service 2 : " + e.getLocalizedMessage();
        }
    }

    
    @GetMapping("/api/stock/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        ProductReply reply = stockGrpcService.getProduct(productId);
        return ResponseEntity.ok(Map.of(
                "productId", reply.getProductId(),
                "name",      reply.getName(),
                "quantity",  reply.getQuantity(),
                "status",    reply.getStatus()
        ));
    }

    
    @PostMapping("/api/stock/{productId}/add")
    public ResponseEntity<String> addStock(@PathVariable String productId,
                                           @RequestParam int quantity) {
        stockGrpcService.manageStock(productId, "ADD", quantity);
        return ResponseEntity.ok("Operation ADD envoyee pour " + productId + " (" + quantity + " unites)");
    }

    
    @PostMapping("/api/stock/{productId}/remove")
    public ResponseEntity<String> removeStock(@PathVariable String productId,
                                              @RequestParam int quantity) {
        stockGrpcService.manageStock(productId, "REMOVE", quantity);
        return ResponseEntity.ok("Operation REMOVE envoyee pour " + productId + " (" + quantity + " unites)");
    }
}
