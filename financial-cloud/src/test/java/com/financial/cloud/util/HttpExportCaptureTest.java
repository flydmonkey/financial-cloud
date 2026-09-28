package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExportCaptureTest {

    @Test
    void capturesTheExactBytesWrittenByAnExistingExporter() throws Exception {
        byte[] expected = "same-workbook-content".getBytes(StandardCharsets.UTF_8);

        byte[] captured = HttpExportCapture.capture(response -> {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.getOutputStream().write(expected);
        });

        assertThat(captured).isEqualTo(expected);
    }
}
