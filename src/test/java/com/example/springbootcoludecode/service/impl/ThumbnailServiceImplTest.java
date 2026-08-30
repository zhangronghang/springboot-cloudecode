package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.service.ThumbnailService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ThumbnailServiceImplTest {
    private final ThumbnailService service = new ThumbnailServiceImpl();

    @Test
    void acceptsRealJpegAndPngButRejectsForgedOrDamagedFiles() throws Exception {
        ThumbnailService.ProcessedImage jpeg = service.process(new MockMultipartFile("file", "a.jpg", "image/jpeg", image("jpg", 640, 320, false)));
        ThumbnailService.ProcessedImage png = service.process(new MockMultipartFile("file", "a.png", "image/png", image("png", 10, 20, true)));
        assertEquals("image/jpeg", jpeg.getContentType());
        assertEquals("image/png", png.getContentType());
        assertThrows(IllegalArgumentException.class, () -> service.process(new MockMultipartFile("file", "fake.png", "image/png", image("jpg", 10, 10, false))));
        assertThrows(IllegalArgumentException.class, () -> service.process(new MockMultipartFile("file", "bad.jpg", "image/jpeg", new byte[]{1, 2, 3})));
    }

    @Test
    void scalesWithinBoundsWithoutUpscalingAndPreservesPngAlpha() throws Exception {
        ThumbnailService.ProcessedImage large = service.process(new MockMultipartFile("file", "large.png", "image/png", image("png", 640, 160, false)));
        BufferedImage largeThumbnail = ImageIO.read(new java.io.ByteArrayInputStream(large.getThumbnailBytes()));
        assertEquals(640, large.getWidth()); assertEquals(160, large.getHeight());
        assertEquals(320, largeThumbnail.getWidth()); assertEquals(80, largeThumbnail.getHeight());
        ThumbnailService.ProcessedImage small = service.process(new MockMultipartFile("file", "small.png", "image/png", image("png", 12, 20, true)));
        BufferedImage smallThumbnail = ImageIO.read(new java.io.ByteArrayInputStream(small.getThumbnailBytes()));
        assertEquals(12, smallThumbnail.getWidth()); assertEquals(20, smallThumbnail.getHeight()); assertTrue(smallThumbnail.getColorModel().hasAlpha());
    }

    @Test
    void correctsJpegExifOrientationForTheThumbnailOnly() throws Exception {
        byte[] original = jpegWithOrientationSix(image("jpg", 40, 80, false));
        ThumbnailService.ProcessedImage processed = service.process(new MockMultipartFile("file", "rotated.jpg", "image/jpeg", original));
        BufferedImage thumbnail = ImageIO.read(new java.io.ByteArrayInputStream(processed.getThumbnailBytes()));
        assertEquals(80, thumbnail.getWidth());
        assertEquals(40, thumbnail.getHeight());
        assertArrayEquals(original, processed.getOriginalBytes());
    }

    private byte[] image(String format, int width, int height, boolean alpha) throws Exception {
        BufferedImage image = new BufferedImage(width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, alpha ? 0x00000000 : Color.RED.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, format, output); return output.toByteArray();
    }

    private byte[] jpegWithOrientationSix(byte[] jpeg) throws Exception {
        byte[] exif = new byte[]{'E','x','i','f',0,0,'M','M',0,42,0,0,0,8,0,1,1,18,0,3,0,0,0,1,0,6,0,0,0,0,0,0,0,0};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(jpeg, 0, 2); output.write(0xff); output.write(0xe1); output.write(0); output.write(exif.length + 2); output.write(exif); output.write(jpeg, 2, jpeg.length - 2);
        return output.toByteArray();
    }
}
