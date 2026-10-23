package com.arquiteturando.payroll.worker.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class BatchMonitorContractTest {
    private static final Path CONTRACT = Path.of("../../packages/contracts/openapi/batch-monitor-api.yaml");

    @Test
    void openApiIsReadOnlyAndAllLocalReferencesResolve() throws IOException {
        Map<String, Object> document = new Yaml().load(Files.readString(CONTRACT));

        assertThat(document.get("openapi")).isEqualTo("3.1.0");
        Map<String, Object> paths = map(document.get("paths"));
        assertThat(paths).hasSize(10);
        assertThat(paths.keySet()).allMatch(path -> path.startsWith("/internal/v1/batch"));
        assertThat(paths.values()).allSatisfy(value -> {
            Map<String, Object> operations = map(value);
            assertThat(operations).containsKey("get");
            assertThat(operations).doesNotContainKeys("post", "put", "patch", "delete");
        });

        List<String> references = new ArrayList<>();
        collectReferences(document, references);
        assertThat(references).isNotEmpty().allSatisfy(reference ->
                assertThat(resolve(document, reference)).as(reference).isNotNull());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static void collectReferences(Object node, List<String> references) {
        if (node instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if ("$ref".equals(key)) references.add(value.toString());
                collectReferences(value, references);
            });
        } else if (node instanceof Iterable<?> iterable) {
            iterable.forEach(value -> collectReferences(value, references));
        }
    }

    private static Object resolve(Map<String, Object> document, String reference) {
        if (!reference.startsWith("#/")) return null;
        Object current = document;
        for (String segment : reference.substring(2).split("/")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(segment.replace("~1", "/").replace("~0", "~"));
        }
        return current;
    }
}
