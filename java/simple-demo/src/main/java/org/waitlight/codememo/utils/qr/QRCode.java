package org.waitlight.codememo.utils.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class QRCode {

    public static final int IMG_W = 720;
    public static final int IMG_H = 920;

    public static final int QR_CODE_W = 720;
    public static final int QR_CODE_H = 720;

    public static final int LOGO_X = QR_CODE_W / 5 * 2;
    public static final int LOGO_Y = QR_CODE_H / 5 * 2;
    public static final int LOGO_W = QR_CODE_W / 5;
    public static final int LOGO_H = QR_CODE_H / 5;

    public static final int SUMMARY_BORDER_X = 10;
    public static final int SUMMARY_BORDER_Y = QR_CODE_H + 10;
    public static final int SUMMARY_BORDER_W = 700;
    public static final int SUMMARY_BORDER_H = 180;

    public static final int SUMMARY_BORDER_LINE_WIDTH = 4;

    public static final int SUMMARY_FONT_WEIGHT = 64;
    public static final int ORG_FONT_WEIGHT = 18;

    public static final String DEF_FONT_NAME = "WenQuanYi Zen Hei";


    private final int margin = 50;
    private final Color backgroudColor = new Color(244, 244, 244);

    private final String contents;
    private final String logo;
    private final String summary;
    private final String org;

    private Graphics2D g2d;
    private BufferedImage image;

    private QRCode() {
        this.contents = null;
        this.logo = null;
        this.summary = null;
        this.org = null;
    }

    public QRCode(String contents, String logo) {
        this.contents = Validate.notBlank(contents);
        this.logo = Validate.notBlank(logo);
        this.summary = null;
        this.org = null;
    }

    public QRCode(String contents, String logo, String summary, String org) {
        this.contents = Validate.notBlank(contents);
        this.logo = Validate.notBlank(logo);
        this.summary = summary;
        this.org = org;
    }

    public QRCode draw() throws WriterException, IOException {
        if (StringUtils.isBlank(summary)) {
            image = new BufferedImage(QR_CODE_W, QR_CODE_H, BufferedImage.TYPE_INT_RGB);
        } else {
            image = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_INT_RGB);
        }

        if (Objects.isNull(g2d)) {
            g2d = image.createGraphics();
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(backgroudColor);
        g2d.fillRect(0, 0, IMG_W, IMG_H);

        drawQrCode();
        drawSummary(g2d, summary, org);

        g2d.dispose();
        image.flush();

        return this;
    }

    public void toPng(String outputPath) throws IOException {
        if (StringUtils.isBlank(summary)) {
            ImageIO.write(image, "png", new File(outputPath + "/" + System.currentTimeMillis() + ".png"));
        } else {
            ImageIO.write(image, "png", new File(outputPath + "/" + summary + ".png"));
        }
    }

    public OutputStream toOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return os;
    }

    private void drawQrCode() throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, QR_CODE_W, QR_CODE_H, hints);
        MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(backgroudColor.getRGB(), Color.BLACK.getRGB());
        BufferedImage qrBufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig);
        g2d.drawImage(qrBufferedImage, margin, margin, QR_CODE_W - margin * 2, QR_CODE_H - margin * 2, null);

        drawLogo(QR_CODE_W - margin * 2, QR_CODE_H - margin * 2);
    }

    private void drawLogo(int parentW, int parentH) throws IOException {
        if (StringUtils.isBlank(logo)) {
            return;
        }

        int logoW = parentW;

        BufferedImage logoImage = ImageIO.read(new File(logo));
        RoundRectangle2D roundRect = new RoundRectangle2D.Double(LOGO_X, LOGO_Y, LOGO_W, LOGO_H, 40, 40);
        g2d.setClip(roundRect);
        g2d.drawImage(logoImage, LOGO_X, LOGO_Y, (int) (parentW * 0.2d), (int) (parentH * 0.2d), null);
        g2d.setClip(null);
    }

    private void drawSummary(Graphics2D g2d, String summary, String org) {
        drawBorder(g2d, SUMMARY_BORDER_LINE_WIDTH, SUMMARY_BORDER_X, SUMMARY_BORDER_Y, SUMMARY_BORDER_W, SUMMARY_BORDER_H, 50d, 50d);
        drawSummary(g2d, summary);
        drawOrg(g2d, org);
    }

    private void drawBorder(Graphics2D g2d, float lineWidth,
                            double x, double y,
                            double width, double height,
                            double arcw, double arch) {
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(128, 128, 128));
        g2d.draw(new RoundRectangle2D.Double(x, y, width, height, arcw, arch));
    }

    private void drawSummary(Graphics2D g2d, String summary) {
        if (StringUtils.isBlank(summary)) {
            return;
        }

        int textWidth;
        int loop = 0;
        FontMetrics metrics;
        do {
            Font font = new Font(DEF_FONT_NAME, Font.PLAIN, SUMMARY_FONT_WEIGHT - loop);
            g2d.setFont(font);
            g2d.setColor(Color.BLACK);

            metrics = g2d.getFontMetrics();
            textWidth = metrics.stringWidth(summary);
            loop++;
        } while (textWidth > SUMMARY_BORDER_W - 10);

        int x = (IMG_W - textWidth) / 2;
        int y = QR_CODE_H + SUMMARY_BORDER_H / 2 + metrics.getHeight() / 2 - metrics.getDescent() - 6;
        g2d.drawString(summary, x, y);
    }

    private void drawOrg(Graphics2D g2d, String org) {
        if (StringUtils.isBlank(org)) {
            return;
        }

        Font font = new Font(Font.SERIF, Font.PLAIN, ORG_FONT_WEIGHT);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);

        FontMetrics fontMetrics = g2d.getFontMetrics();
        int contentWidth = fontMetrics.stringWidth(org);
        int contentHeight = fontMetrics.getHeight();

        int x = IMG_W - 20 - SUMMARY_BORDER_LINE_WIDTH - contentWidth;
        int y = IMG_H - 8 - SUMMARY_BORDER_LINE_WIDTH - (contentHeight / 2);
        g2d.drawString(org, x, y);
    }

}