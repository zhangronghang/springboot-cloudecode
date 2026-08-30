package com.example.springbootcoludecode.service.impl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.example.springbootcoludecode.service.ThumbnailService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ThumbnailServiceImpl implements ThumbnailService {
    private static final int MAX_SIZE = 320;

    @Override public ProcessedImage process(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("图片文件不能为空");
            byte[] original = file.getBytes(); String contentType = contentType(original);
            if (!contentType.equals(file.getContentType())) throw new IllegalArgumentException("图片声明类型与实际内容不一致");
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(original));
            if (decoded == null) throw new IllegalArgumentException("图片文件无法解码");
            BufferedImage displayed = "image/jpeg".equals(contentType) ? orient(decoded, orientation(original)) : decoded;
            BufferedImage thumbnail = scale(displayed, "image/png".equals(contentType));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(thumbnail, "image/png".equals(contentType) ? "png" : "jpg", output)) throw new IllegalArgumentException("缩略图生成失败");
            return new ProcessedImage(original, output.toByteArray(), contentType, decoded.getWidth(), decoded.getHeight());
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("图片文件无法解码", e); }
    }
    private String contentType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        throw new IllegalArgumentException("仅支持 JPEG 或 PNG 图片");
    }
    private BufferedImage scale(BufferedImage source, boolean png) {
        double ratio = Math.min(1d, Math.min((double) MAX_SIZE / source.getWidth(), (double) MAX_SIZE / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio)), height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        BufferedImage target = new BufferedImage(width, height, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics(); graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); graphics.drawImage(source, 0, 0, width, height, null); graphics.dispose(); return target;
    }
    private int orientation(byte[] bytes) {
        try { Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes)); ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class); return directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION) ? directory.getInt(ExifIFD0Directory.TAG_ORIENTATION) : 1; } catch (Exception ignored) { return 1; }
    }
    private BufferedImage orient(BufferedImage source, int orientation) {
        if (orientation == 1) return source;
        int width = source.getWidth(), height = source.getHeight(); boolean swap = orientation >= 5 && orientation <= 8;
        BufferedImage target = new BufferedImage(swap ? height : width, swap ? width : height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics(); AffineTransform transform = new AffineTransform();
        if (orientation == 3) { transform.translate(width, height); transform.rotate(Math.PI); }
        else if (orientation == 6) { transform.translate(height, 0); transform.rotate(Math.PI / 2); }
        else if (orientation == 8) { transform.translate(0, width); transform.rotate(-Math.PI / 2); }
        else { return source; }
        g.drawImage(source, transform, null); g.dispose(); return target;
    }
}
