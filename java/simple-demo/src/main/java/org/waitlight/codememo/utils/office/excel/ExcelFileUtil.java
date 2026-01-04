package org.waitlight.codememo.utils.office.excel;

import feign.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

public class ExcelFileUtil {
    private static final Logger log = LoggerFactory.getLogger(ExcelFileUtil.class);

    private ExcelFileUtil() {
        throw new UnsupportedOperationException("This is a util class");
    }

    public static void download(Supplier<Workbook> workbookSupplier,
                                HttpServletResponse response,
                                String filename) {

        if (ObjectUtils.anyNull(response, workbookSupplier, filename)) {
            throw new UnsupportedOperationException("Missing necessary information");
        }

        setHeader(response, filename);

        try (Workbook workbook = workbookSupplier.get();
             ServletOutputStream outputStream = response.getOutputStream()
        ) {
            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Export data has error ", e);
        }
    }

    /**
     * Feign client 文件转发
     */
    public static void fileForwarding(HttpServletResponse response, Supplier<Response> feignRequest, String filename) {
        Objects.requireNonNull(response, "response is required");
        Objects.requireNonNull(feignRequest, "supplier is required");
        Objects.requireNonNull(filename, "filename is required");

        Response resp = feignRequest.get();
        Response.Body body = resp.body();
        if (Objects.isNull(body)) return;

        setHeader(response, filename);
        try (InputStream inputStream = body.asInputStream();
             OutputStream outputStream = response.getOutputStream()
        ) {
            FileCopyUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setHeader(HttpServletResponse response, String filename) {
        if (StringUtils.isBlank(filename)) {
            filename = "data.xlsx";
        }

        String[] filenames = filename.split("\\.");
        filename = filenames[0] + DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd-HH-mm-ss-SSS") + "." + filenames[1];
        String charset = StandardCharsets.UTF_8.name();
        try {
            filename = URLEncoder.encode(filename, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Wrong encoding" + charset);
        }

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;" + " filename=" + filename);
    }

}