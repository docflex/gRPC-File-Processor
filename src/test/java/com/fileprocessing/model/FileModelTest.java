package com.fileprocessing.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileModelTest {

    private static final byte[] SAMPLE_CONTENT = {1, 2, 3, 4};

    // =======================
    // Constructor / Validation
    // =======================

    @Test
    void constructor_ShouldInitializeFieldsCorrectly() {
        FileModel file = new FileModel("id1", "test.txt", SAMPLE_CONTENT, "txt", 4L);
        assertEquals("id1", file.fileId());
        assertEquals("test.txt", file.fileName());
        assertArrayEquals(SAMPLE_CONTENT, file.content());
        assertEquals("txt", file.fileType());
        assertEquals(4L, file.sizeBytes());
    }

    @Test
    void constructor_ShouldThrow_OnNullFileId() {
        assertThrows(NullPointerException.class, () -> new FileModel(null, "test.txt", SAMPLE_CONTENT, "txt", 4));
    }

    @Test
    void constructor_ShouldThrow_OnNullFileName() {
        assertThrows(NullPointerException.class, () -> new FileModel("id", null, SAMPLE_CONTENT, "txt", 4));
    }

    @Test
    void constructor_ShouldThrow_OnNullFileType() {
        assertThrows(NullPointerException.class, () -> new FileModel("id", "test.txt", SAMPLE_CONTENT, null, 4));
    }

    @Test
    void constructor_ShouldThrow_OnNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new FileModel("id", "test.txt", SAMPLE_CONTENT, "txt", -1));
    }

    @Test
    void constructor_ShouldDefaultContentToEmptyArray_WhenNull() {
        FileModel file = new FileModel("id", "file.txt", null, "txt", 0);
        assertNotNull(file.content());
        assertEquals(0, file.content().length);
    }

    // =======================
    // Defensive Copy
    // =======================

    @Test
    void content_ShouldReturnCopy_NotReference() {
        FileModel file = new FileModel("id", "file.txt", SAMPLE_CONTENT, "txt", 4);
        byte[] copy = file.content();
        assertArrayEquals(SAMPLE_CONTENT, copy);

        // Modifying the copy should not affect original
        copy[0] = 99;
        assertEquals(1, file.content()[0]);
    }

    @Test
    void builder_ShouldProduceEquivalentObject() {
        FileModel file = FileModel.builder()
                .fileId("id2")
                .fileName("builder.txt")
                .content(SAMPLE_CONTENT)
                .fileType("pdf")
                .sizeBytes(4)
                .build();

        assertEquals("id2", file.fileId());
        assertEquals("builder.txt", file.fileName());
        assertArrayEquals(SAMPLE_CONTENT, file.content());
        assertEquals("pdf", file.fileType());
        assertEquals(4, file.sizeBytes());
    }

    // =======================
    // isImage / isPdf
    // =======================

    @Test
    void isImage_ShouldReturnTrue_ForSupportedTypes() {
        assertTrue(new FileModel("id", "img.jpg", SAMPLE_CONTENT, "jpg", 1).isImage());
        assertTrue(new FileModel("id", "img.jpeg", SAMPLE_CONTENT, "jpeg", 1).isImage());
        assertTrue(new FileModel("id", "img.png", SAMPLE_CONTENT, "png", 1).isImage());
        assertTrue(new FileModel("id", "img.gif", SAMPLE_CONTENT, "gif", 1).isImage());
    }

    @Test
    void isImage_ShouldReturnFalse_ForUnsupportedTypes() {
        assertFalse(new FileModel("id", "doc.txt", SAMPLE_CONTENT, "txt", 1).isImage());
        assertFalse(new FileModel("id", "pdf", SAMPLE_CONTENT, "pdf", 1).isImage());
    }

    @Test
    void isPdf_ShouldReturnTrue_ForPdf() {
        assertTrue(new FileModel("id", "file.pdf", SAMPLE_CONTENT, "pdf", 1).isPdf());
    }

    @Test
    void isPdf_ShouldReturnFalse_ForNonPdf() {
        assertFalse(new FileModel("id", "file.txt", SAMPLE_CONTENT, "txt", 1).isPdf());
        assertFalse(new FileModel("id", "file.jpg", SAMPLE_CONTENT, "jpg", 1).isPdf());
    }

    // =======================
    // Immutability checks
    // =======================

    @Test
    void builderContent_ShouldBeImmutable() {
        byte[] input = {5, 6, 7};
        FileModel file = FileModel.builder()
                .fileId("id")
                .fileName("test.txt")
                .content(input)
                .fileType("txt")
                .sizeBytes(3)
                .build();

        // Modify original input array
        input[0] = 99;
        assertEquals(5, file.content()[0]);
    }

    @Test
    void recordFields_ShouldBeImmutable() {
        FileModel file = new FileModel("id", "name", SAMPLE_CONTENT, "txt", 3);
        assertEquals("id", file.fileId());
        assertEquals("name", file.fileName());
        assertArrayEquals(SAMPLE_CONTENT, file.content());
        assertEquals("txt", file.fileType());
        assertEquals(3, file.sizeBytes());
    }
}
