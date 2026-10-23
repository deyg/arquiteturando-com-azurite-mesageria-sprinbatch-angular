package com.arquiteturando.payroll.query;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueryBlobStorage {
    private final BlobContainerClient container;
    public QueryBlobStorage(@Value("${payroll.storage.connection-string}") String connection,
                            @Value("${payroll.storage.container}") String containerName) {
        this.container = new BlobContainerClientBuilder().connectionString(connection)
                .containerName(containerName).buildClient();
    }
    public InputStream open(String blobName) { return container.getBlobClient(blobName).openInputStream(); }
}
