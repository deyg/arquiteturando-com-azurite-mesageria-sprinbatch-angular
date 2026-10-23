package com.arquiteturando.payroll.worker;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlobStorageService {
    private final BlobContainerClient container;

    public BlobStorageService(@Value("${payroll.storage.connection-string}") String connectionString,
                              @Value("${payroll.storage.container}") String containerName) {
        this.container = new BlobContainerClientBuilder()
                .connectionString(connectionString).containerName(containerName).buildClient();
        this.container.createIfNotExists();
    }

    public String blobName(UUID requestId, long paymentId) {
        return "exports/" + requestId + "/" + paymentId + ".pdf";
    }

    public void upload(String blobName, byte[] bytes) {
        container.getBlobClient(blobName).upload(BinaryData.fromBytes(bytes), true);
    }

    public byte[] download(String blobName) {
        return container.getBlobClient(blobName).downloadContent().toBytes();
    }

    public boolean exists(String blobName) {
        return container.getBlobClient(blobName).exists();
    }
}
