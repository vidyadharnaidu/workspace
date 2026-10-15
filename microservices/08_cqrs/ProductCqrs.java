package cqrs;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PATTERN: CQRS - Command Query Responsibility Segregation
 *
 * PROBLEM: one model serves both "update this product" (normalized, validated, consistent)
 *          and "show the catalogue page" (denormalized, fast, joined across services).
 *          Optimizing for one hurts the other, and reads usually outnumber writes 100:1.
 * SOLUTION: split them. The WRITE side owns the rules and a normalized store. The READ
 *           side owns a separate, denormalized, query-shaped store, kept up to date by events.
 *
 * TRICK TO REMEMBER: CQRS = two MODELS, and often two DATABASES. The read model is
 *                    EVENTUALLY consistent - your UI must tolerate a stale read right
 *                    after a write (return the new value from the command, or poll).
 *
 * TRICK: CQRS is not automatically event sourcing (see 09). You can do CQRS with a plain
 *        JPA write side, as here. Event sourcing usually implies CQRS, not the reverse.
 *
 * DO NOT reach for this by default: it doubles the moving parts. Use it where the read
 * load, or the shape mismatch, is genuinely painful.
 */
public class ProductCqrs {

    // =================== WRITE SIDE ===================
    // Normalized, relational, validated. Commands are imperative and may be rejected.

    public record CreateProduct(String sku, String name, double price, String categoryId) { }
    public record ChangePrice(String sku, double newPrice) { }

    @RestController
    @RequestMapping("/products")
    static class ProductCommandController {

        private final ProductCommandService service;

        ProductCommandController(ProductCommandService service) { this.service = service; }

        /** Commands return an id or 202, NOT the full object - the read side owns that. */
        @PostMapping
        String create(@RequestBody CreateProduct cmd) { return service.handle(cmd); }

        @PutMapping("/{sku}/price")
        void changePrice(@PathVariable String sku, @RequestBody double price) {
            service.handle(new ChangePrice(sku, price));
        }
    }

    @Service
    static class ProductCommandService {

        private final ProductJpaRepository repo;
        private final KafkaTemplate<String, Object> kafka;

        ProductCommandService(ProductJpaRepository repo, KafkaTemplate<String, Object> kafka) {
            this.repo = repo;
            this.kafka = kafka;
        }

        @Transactional
        String handle(CreateProduct cmd) {
            if (repo.existsById(cmd.sku())) throw new IllegalStateException("duplicate sku");
            repo.save(new ProductEntity(cmd.sku(), cmd.name(), cmd.price(), cmd.categoryId()));
            kafka.send("product-events", cmd.sku(), new ProductCreated(cmd.sku(), cmd.name(), cmd.price(), cmd.categoryId()));
            return cmd.sku();
        }

        @Transactional
        void handle(ChangePrice cmd) {
            ProductEntity p = repo.findById(cmd.sku()).orElseThrow();
            p.price = cmd.newPrice();                       // the business rules live HERE
            kafka.send("product-events", cmd.sku(), new PriceChanged(cmd.sku(), cmd.newPrice()));
        }
    }

    static class ProductEntity {
        String sku, name, categoryId;
        double price;
        ProductEntity(String sku, String name, double price, String categoryId) {
            this.sku = sku; this.name = name; this.price = price; this.categoryId = categoryId;
        }
    }

    interface ProductJpaRepository extends JpaRepository<ProductEntity, String> { }

    // =================== EVENTS: the bridge ===================
    public record ProductCreated(String sku, String name, double price, String categoryId) { }
    public record PriceChanged(String sku, double newPrice) { }

    // =================== READ SIDE ===================
    // Denormalized: one document has everything the page needs, so zero joins, zero fan-out calls.

    static class ProductView {
        String sku, name, categoryName, brandName;    // flattened from 3 services
        double price;
        double rating;
        int reviewCount;
        boolean inStock;
    }

    interface ProductViewRepository extends MongoRepository<ProductView, String> {
        List<ProductView> findByCategoryNameAndInStockTrue(String categoryName);
    }

    @RestController
    @RequestMapping("/catalogue")
    static class ProductQueryController {

        private final ProductViewRepository views;

        ProductQueryController(ProductViewRepository views) { this.views = views; }

        /** Reads never touch the write database and never call other services. */
        @GetMapping("/{sku}")
        ProductView get(@PathVariable String sku) { return views.findById(sku).orElseThrow(); }

        @GetMapping
        List<ProductView> byCategory(@RequestParam String category) {
            return views.findByCategoryNameAndInStockTrue(category);
        }
    }

    /** The PROJECTOR keeps the read model in sync. This is where the eventual consistency lives. */
    @Service
    static class ProductProjector {

        private final ProductViewRepository views;

        ProductProjector(ProductViewRepository views) { this.views = views; }

        @KafkaListener(topics = "product-events", groupId = "catalogue-projector")
        void on(ProductCreated e) {
            ProductView v = new ProductView();
            v.sku = e.sku(); v.name = e.name(); v.price = e.price();
            v.categoryName = lookupCategoryName(e.categoryId());   // denormalize ONCE, at write time
            views.save(v);
        }

        @KafkaListener(topics = "product-events", groupId = "catalogue-projector")
        void on(PriceChanged e) {
            views.findById(e.sku()).ifPresent(v -> { v.price = e.newPrice(); views.save(v); });
        }

        /** Stock lives in another service; its events update the same view. */
        @KafkaListener(topics = "inventory-events", groupId = "catalogue-projector")
        void onStock(String sku, boolean inStock) {
            views.findById(sku).ifPresent(v -> { v.inStock = inStock; views.save(v); });
        }

        private String lookupCategoryName(String categoryId) { return "Electronics"; }
    }
}
