package com.financial.cloud.util;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;

/** Captures an existing HTTP exporter without duplicating its workbook generation path. */
public final class HttpExportCapture {

    private HttpExportCapture() {
    }

    @FunctionalInterface
    public interface ExportAction {
        void write(HttpServletResponse response) throws IOException;
    }

    public static byte[] capture(ExportAction action) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ServletOutputStream output = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // In-memory output is always ready and requires no callback.
            }

            @Override
            public void write(int value) {
                bytes.write(value);
            }

            @Override
            public void write(byte[] value, int offset, int length) {
                bytes.write(value, offset, length);
            }
        };
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("getOutputStream".equals(method.getName())) {
                        return output;
                    }
                    Class<?> type = method.getReturnType();
                    if (!type.isPrimitive()) return null;
                    if (type == boolean.class) return false;
                    if (type == char.class) return '\0';
                    if (type == byte.class) return (byte) 0;
                    if (type == short.class) return (short) 0;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == float.class) return 0F;
                    if (type == double.class) return 0D;
                    return null;
                });
        action.write(response);
        return bytes.toByteArray();
    }
}
