package org.waitlight.codememo.utils.office.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExcelWriter<T> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ExcelWriter.class);

    private final Class<T> klass;
    private final Workbook workbook;
    private List<CellHandler> cellHandlers = new ArrayList<>();

    public ExcelWriter(Workbook workbook, Class<T> klass) {
        this.klass = klass;
        this.workbook = workbook;
    }

    public void insert(boolean newSheet, List<T> data) {
        Sheet sheet;
        if (newSheet) {
            sheet = workbook.createSheet();
        } else {
            sheet = workbook.getSheetAt(0);
        }
        insert(sheet, data);
    }

    public void toFile(List<T> data) {

    }

    public void toOutputStream(OutputStream outputStream) {

    }

    private <T> void insert(Sheet sheet, List<T> data) {
        for (int i = 0; i < data.size(); i++) {
            T obj = data.get(i);
            Row row = sheet.createRow(i);
            writeRow(row, obj);
        }
    }

    private <T> void writeRow(Row row, T datum) {
        for (int cellNum = 0; cellNum < cellHandlers.size(); cellNum++) {
            Cell cell = row.createCell(cellNum);
            Method method = cellHandlers.get(cellNum).getMethod();
            Object value = null;
            try {
                value = method.invoke(datum);
            } catch (Exception e) {
                logger.error("Failed to invoke method", e);
            }

            if (Objects.isNull(value)) {
                continue;
            }

            cell.setCellValue(String.valueOf(value));
        }
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(workbook)) {
            workbook.close();
        }
    }
}
