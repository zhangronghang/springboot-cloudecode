package com.example.springbootcoludecode.dto;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankInformationIdAndEmptyImageIds() {
        ImageBatchDeleteRequest request = new ImageBatchDeleteRequest();
        request.setInformationId(" "); request.setImageIds(Collections.<String>emptyList());
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankInformationIdAndInvalidPagination() {
        ImagePageRequest request = new ImagePageRequest();
        request.setInformationId(""); request.setPage(0); request.setSize(0);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsValidImageRequests() {
        ImageBatchDeleteRequest deletion = new ImageBatchDeleteRequest(); deletion.setInformationId("record"); deletion.setImageIds(Collections.singletonList("image"));
        ImagePageRequest page = new ImagePageRequest(); page.setInformationId("record"); page.setPage(1); page.setSize(10);
        assertTrue(validator.validate(deletion).isEmpty());
        assertTrue(validator.validate(page).isEmpty());
    }
}
