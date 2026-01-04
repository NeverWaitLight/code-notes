package org.waitlight.codememo.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    private ZipUtil() {
        throw new UnsupportedOperationException("This is a util class");
    }

    public static void unzip(String zipPath, Charset charset, String destDir) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get(zipPath))) {
            unzip(in, charset, entry -> {
                String path = destDir + File.separator + entry.getFilename();
                try (FileOutputStream fos = new FileOutputStream(path)) {
                    fos.write(entry.getOutputStream().toByteArray());
                } catch (IOException e) {
                    throw new ZipException(e);
                }
            });
        }
    }

    public static void unzip(InputStream in, Charset charset, Consumer<ZipStreamEntry> consumer) throws IOException {
        if (Objects.isNull(charset)) charset = StandardCharsets.UTF_8;

        try (ZipInputStream zin = new ZipInputStream(in, charset)) {
            ZipEntry ze;
            while (Objects.nonNull(ze = zin.getNextEntry())) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = zin.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    consumer.accept(new ZipStreamEntry(ze.getName(), out));
                }
            }
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ZipStreamEntry {
        private String filename;
        private ByteArrayOutputStream outputStream;
    }

    public static class ZipException extends RuntimeException {
        public ZipException() {
        }

        public ZipException(String message) {
            super(message);
        }

        public ZipException(String message, Throwable cause) {
            super(message, cause);
        }

        public ZipException(Throwable cause) {
            super(cause);
        }

        public ZipException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}