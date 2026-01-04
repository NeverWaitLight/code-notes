package org.waitlight.codememo.utils.office.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ExcelPictureDemo {
    public void run() {
        try {
            // 打开现有的 Excel 文件
            Workbook workbook = new XSSFWorkbook();

            // 获取要插入图片的工作表和单元格
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            sheet.setColumnWidth(row.getRowNum(), 12 * 256);
            Cell cell = row.createCell(0);
            row.setHeight((short) (100 * 20));

            // 加载图片
            InputStream imageStream = new FileInputStream("C:/Users/10963/Pictures/1024.png");
            byte[] bytes = IOUtils.toByteArray(imageStream);
            int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

            // 创建绘图对象
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            // 创建锚点并设置图片位置
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(cell.getColumnIndex());
            anchor.setRow1(cell.getRowIndex());
            anchor.setCol2(cell.getColumnIndex() + 1);
            anchor.setRow2(cell.getRowIndex() + 1);

            // 插入图片
            Picture picture = drawing.createPicture(anchor, pictureIdx);
            int widthUnits = 1; // 设置图片宽度为320个单位
            int heightUnits = 1; // 设置图片高度为120个单位
            picture.resize(widthUnits, heightUnits);

            // 保存修改后的 Excel 文件
            FileOutputStream outputStream = new FileOutputStream("C:/Users/10963/Desktop/excel/modified_excel.xlsx");
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            System.out.println("图片插入成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
