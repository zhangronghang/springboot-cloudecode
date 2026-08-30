package com.example.springbootcoludecode.entity;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Field;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMetadataModelTest {

    @Test
    void persistsOrderedImageItemsWithoutLegacySingleImageFields() throws Exception {
        Class<?> imageItemClass = Class.forName("com.example.springbootcoludecode.entity.ImageItem");
        assertEquals(String.class, imageItemClass.getDeclaredField("imageId").getType());
        assertEquals(String.class, imageItemClass.getDeclaredField("originalGridFsFileId").getType());
        assertEquals(String.class, imageItemClass.getDeclaredField("thumbnailGridFsFileId").getType());
        assertEquals(long.class, imageItemClass.getDeclaredField("fileSize").getType());
        assertEquals(int.class, imageItemClass.getDeclaredField("width").getType());
        assertEquals(int.class, imageItemClass.getDeclaredField("height").getType());

        assertNotNull(ImageMetadata.class.getDeclaredField("images"));
        assertFalse(Arrays.stream(ImageMetadata.class.getDeclaredFields())
                .map(field -> field.getName())
                .anyMatch(name -> name.equals("gridFsFileId") || name.equals("fileName") || name.equals("fileSize") || name.equals("imageCount")));
        assertFalse(Arrays.stream(ImageMetadata.class.getDeclaredMethods())
                .map(method -> method.getName())
                .anyMatch(name -> name.equals("getImageCount") || name.equals("setImageCount")));
    }

    @Test
    void initializesImagesAsAnEmptyMutableList() throws Exception {
        ImageMetadata metadata = new ImageMetadata();
        assertNotNull(metadata.getImages());
        assertTrue(metadata.getImages().isEmpty());
        metadata.getImages().add(new ImageItem());
        assertEquals(1, metadata.getImages().size());
    }
}
