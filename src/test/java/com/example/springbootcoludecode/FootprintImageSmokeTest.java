package com.example.springbootcoludecode;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.ImageBatchDeleteRequest;
import com.example.springbootcoludecode.dto.ImageDeleteRequest;
import com.example.springbootcoludecode.dto.ImageDetailRequest;
import com.example.springbootcoludecode.dto.ImageListRequest;
import com.example.springbootcoludecode.dto.ImagePageRequest;
import com.example.springbootcoludecode.dto.InformationResponse;
import com.example.springbootcoludecode.dto.PublicImageResponse;
import com.example.springbootcoludecode.service.ImageService;
import com.example.springbootcoludecode.service.InformationImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FootprintImageSmokeTest {
    @Autowired private ImageService imageService;
    @Autowired private InformationImageService informationImageService;

    @Test
    void exercisesTheFootprintGalleryAgainstLocalMongoAndGridFs() {
        String marker = "gallery-smoke-" + UUID.randomUUID();
        String informationId = null;
        try {
            ApiResponse created = imageService.upload(file("first.png"), marker, null, null, null, "smoke", "smoke", marker);
            assertEquals(200, created.getCode());
            InformationResponse createdRecord = (InformationResponse) created.getData();
            informationId = createdRecord.getId();
            assertEquals(1, createdRecord.getImageCount());

            ImageListRequest listRequest = new ImageListRequest(); listRequest.setUploader(marker);
            assertEquals(1L, ((Map<?, ?>) imageService.list(listRequest).getData()).get("total"));
            ImageDetailRequest detailRequest = new ImageDetailRequest(); detailRequest.setId(informationId);
            assertEquals(1, ((InformationResponse) imageService.detail(detailRequest).getData()).getImageCount());

            ApiResponse added = informationImageService.addImage(informationId, file("second.png"));
            assertEquals(200, added.getCode());
            PublicImageResponse second = (PublicImageResponse) added.getData();
            ImagePageRequest pageRequest = new ImagePageRequest(); pageRequest.setInformationId(informationId); pageRequest.setPage(1); pageRequest.setSize(10);
            Map<?, ?> page = (Map<?, ?>) informationImageService.listImages(pageRequest).getData();
            assertEquals(2, page.get("total"));
            List<?> records = (List<?>) page.get("records");
            PublicImageResponse first = (PublicImageResponse) records.get(0);
            assertNotNull(informationImageService.readThumbnail(informationId, first.getImageId()));
            assertNotNull(informationImageService.readOriginal(informationId, first.getImageId()));

            ImageBatchDeleteRequest deleteImages = new ImageBatchDeleteRequest(); deleteImages.setInformationId(informationId); deleteImages.setImageIds(Arrays.asList(second.getImageId()));
            assertEquals(1, ((Map<?, ?>) informationImageService.deleteImages(deleteImages).getData()).get("deletedCount"));
            ImageDeleteRequest deleteRecord = new ImageDeleteRequest(); deleteRecord.setId(informationId);
            assertEquals(200, imageService.delete(deleteRecord).getCode());
            informationId = null;
        } finally {
            if (informationId != null) {
                ImageDeleteRequest cleanup = new ImageDeleteRequest(); cleanup.setId(informationId); imageService.delete(cleanup);
            }
        }
    }

    private MockMultipartFile file(String fileName) {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output);
            return new MockMultipartFile("file", fileName, "image/png", output.toByteArray());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
