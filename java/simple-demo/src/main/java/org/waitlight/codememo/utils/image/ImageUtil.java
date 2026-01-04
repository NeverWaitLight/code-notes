package org.waitlight.codememo.utils.image;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageUtil {
    public static final double SCALE = 1.0d;
    public static final double QUALITY = 0.6d;

    private InputStream inputStream;

    private Thumbnails.Builder<? extends InputStream> builder;

    public ImageUtil sourcePath(String sourcePath) throws IOException {
        this.inputStream = Files.newInputStream(Paths.get(sourcePath));
        return this;
    }

    public ImageUtil inputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public ImageUtil compress() {
        builder = Thumbnails.of(inputStream).scale(SCALE).outputQuality(QUALITY);
        return this;
    }

    public void toFile(String outputPath) throws IOException {
        toFile(outputPath, null);
    }

    public void toFile(String outputPath, String outputType) throws IOException {
        if (StringUtils.isNotBlank(outputType)) builder.outputFormat(outputType);
        builder.toFile(outputPath);
    }

    public void toOutputStream(OutputStream outputStream) throws IOException {
        toOutputStream(outputStream, null);
    }

    public void toOutputStream(OutputStream outputStream, String outputType) throws IOException {
        if (StringUtils.isNotBlank(outputType)) builder.outputFormat(outputType);
        builder.toOutputStream(outputStream);
    }

}
