package com.company.oa.common.service;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

/**
 * 水印服务
 * 下载文件自动加水印，追溯泄露源头
 */
@Service
public class WatermarkService {
    private final AuthService authService;

    public WatermarkService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 生成水印图片
     */
    public BufferedImage generateWatermark(BufferedImage originalImage, String text) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);

        // 设置水印样式
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(new Color(255, 255, 255, 128)); // 半透明白色

        // 绘制水印文字
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int x = width - fontMetrics.stringWidth(text) - 20;
        int y = height - 20;
        g2d.drawString(text, x, y);

        g2d.dispose();
        return watermarkedImage;
    }

    /**
     * 生成带水印的文件
     */
    public byte[] addWatermark(byte[] fileContent, String fileName, String fileType) throws Exception {
        AuthUser user = authService.currentUser();

        // 生成水印文本
        String watermarkText = String.format("%s | %s | %s",
            user.realName(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            "机密文件"
        );

        // 对于图片文件，添加可见水印
        if (fileType.equals("image")) {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(fileContent));
            BufferedImage watermarkedImage = generateWatermark(originalImage, watermarkText);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(watermarkedImage, "png", baos);
            return baos.toByteArray();
        }

        // 对于其他文件，返回原内容（实际项目中可以添加元数据水印）
        return fileContent;
    }

    /**
     * 生成PDF水印文本
     */
    public String generatePdfWatermarkText() {
        AuthUser user = authService.currentUser();
        return String.format("下载者：%s | 时间：%s | 仅供内部使用",
            user.realName(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}
