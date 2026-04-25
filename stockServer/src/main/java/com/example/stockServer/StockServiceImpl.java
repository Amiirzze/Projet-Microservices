package com.example.stockServer;

import com.example.lib.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class StockServiceImpl extends StockServiceGrpc.StockServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductReply> responseObserver) {
        logger.info("getProduct => {}", request.getProductId());

        Product product = productRepository.findByProductId(request.getProductId()).orElse(null);

        ProductReply reply;
        if (product == null) {
            reply = ProductReply.newBuilder()
                    .setProductId(request.getProductId())
                    .setName("Inconnu")
                    .setQuantity(0)
                    .setStatus("NOT_FOUND")
                    .build();
        } else {
            String status = product.getQuantity() > 0 ? "AVAILABLE" : "OUT_OF_STOCK";
            reply = ProductReply.newBuilder()
                    .setProductId(product.getProductId())
                    .setName(product.getName())
                    .setQuantity(product.getQuantity())
                    .setStatus(status)
                    .build();
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StockOperation> manageStock(StreamObserver<StockResult> responseObserver) {
        return new StreamObserver<StockOperation>() {
            @Override
            public void onNext(StockOperation operation) {
                logger.info("manageStock => {} {} {}", operation.getProductId(),
                        operation.getOperationType(), operation.getQuantity());

                Product product = productRepository.findByProductId(operation.getProductId())
                        .orElseGet(() -> {
                            Product p = new Product(operation.getProductId(),
                                    "Produit-" + operation.getProductId(), 0, "GENERAL");
                            return productRepository.save(p);
                        });

                int delta = operation.getQuantity();
                if ("REMOVE".equalsIgnoreCase(operation.getOperationType())) delta = -delta;
                int newQty = Math.max(0, product.getQuantity() + delta);
                product.setQuantity(newQty);
                productRepository.save(product);

                String msg = "ADD".equalsIgnoreCase(operation.getOperationType())
                        ? "Stock augmenté de " + operation.getQuantity()
                        : "Stock réduit de " + operation.getQuantity();

                responseObserver.onNext(StockResult.newBuilder()
                        .setProductId(product.getProductId())
                        .setNewQuantity(newQty)
                        .setMessage(msg)
                        .build());
            }

            @Override
            public void onError(Throwable t) { logger.error("Erreur stream: {}", t.getMessage()); }

            @Override
            public void onCompleted() {
                logger.info("ManageStock stream complété");
                responseObserver.onCompleted();
            }
        };
    }
}
