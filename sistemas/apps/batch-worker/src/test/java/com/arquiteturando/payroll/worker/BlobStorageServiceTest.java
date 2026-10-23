package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BlobStorageServiceTest {
    @Autowired BlobStorageService storage;

    @Test
    void uploadsReadsAndFindsDeterministicBlob() {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        String name = storage.blobName(requestId, 501);
        byte[] pdf = "%PDF-test".getBytes();
        storage.upload(name, pdf);
        assertThat(storage.exists(name)).isTrue();
        assertThat(storage.download(name)).isEqualTo(pdf);
        assertThat(name).isEqualTo("exports/00000000-0000-0000-0000-000000000007/501.pdf");
    }
}
