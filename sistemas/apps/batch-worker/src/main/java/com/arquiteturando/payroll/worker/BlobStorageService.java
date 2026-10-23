package com.arquiteturando.payroll.worker;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class BlobStorageService {
    private final BlobContainerClient container;
    private final Timer uploadDuration;
    private final io.micrometer.core.instrument.Counter retries;

    public BlobStorageService(@Value("${payroll.storage.connection-string}") String connectionString,
                              @Value("${payroll.storage.container}") String containerName, MeterRegistry metrics) {
        this.container = new BlobContainerClientBuilder()
                .connectionString(connectionString).containerName(containerName).buildClient();
        this.container.createIfNotExists();
        this.uploadDuration=Timer.builder("payroll.batch.blob.duration").publishPercentileHistogram().register(metrics);
        this.retries=metrics.counter("payroll.batch.blob.retries");
    }

    public String blobName(UUID requestId, long paymentId) {
        return "exports/" + requestId + "/" + paymentId + ".pdf";
    }

    public void upload(String blobName, byte[] bytes) {
        try { uploadDuration.record(() -> container.getBlobClient(blobName).upload(BinaryData.fromBytes(bytes), true)); }
        catch(RuntimeException exception){ retries.increment(); throw exception; }
    }

    public byte[] download(String blobName) {
        return container.getBlobClient(blobName).downloadContent().toBytes();
    }

    public boolean exists(String blobName) {
        return container.getBlobClient(blobName).exists();
    }
}
