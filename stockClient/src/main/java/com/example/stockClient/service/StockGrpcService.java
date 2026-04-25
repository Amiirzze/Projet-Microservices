package com.example.stockClient.service;

import com.example.lib.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StockGrpcService {

    private static final Logger logger = LoggerFactory.getLogger(StockGrpcService.class);

    @GrpcClient("stockService")
    private StockServiceGrpc.StockServiceStub stockServiceStub;

    @GrpcClient("stockService")
    private StockServiceGrpc.StockServiceBlockingStub stockServiceBlockingStub;

    public ProductReply getProduct(String productId) {
        logger.info("getProduct => {}", productId);
        return stockServiceBlockingStub.getProduct(
                ProductRequest.newBuilder().setProductId(productId).build()
        );
    }

    public void manageStock(String productId, String operationType, int quantity) {
        logger.info("manageStock => {} {} {}", productId, operationType, quantity);

        StreamObserver<StockResult> resultObserver = new StreamObserver<StockResult>() {
            @Override
            public void onNext(StockResult result) {
                logger.info("StockResult => produit={} newQty={} msg={}",
                        result.getProductId(), result.getNewQuantity(), result.getMessage());
            }
            @Override
            public void onError(Throwable t) { logger.error("Erreur gRPC: {}", t.getMessage()); }
            @Override
            public void onCompleted() { logger.info("ManageStock terminé"); }
        };

        StreamObserver<StockOperation> operationObserver = stockServiceStub.manageStock(resultObserver);
        operationObserver.onNext(StockOperation.newBuilder()
                .setProductId(productId)
                .setOperationType(operationType)
                .setQuantity(quantity)
                .build());
        operationObserver.onCompleted();
    }
}
