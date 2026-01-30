package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GrhLibraryManager.
 * Note: This manager is a singleton that loads from grh_library.json,
 * so tests must be careful about state management.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GrhLibraryManagerTest {

    private static final String LIBRARY_FILE = "grh_library.json";
    private static File backupFile;

    @BeforeAll
    static void setUpClass() throws IOException {
        // Backup original library if it exists
        File original = new File(LIBRARY_FILE);
        if (original.exists()) {
            backupFile = new File("grh_library_test_backup.json");
            Files.copy(original.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            original.delete(); // Force recreation of default library
        }
    }

    @AfterAll
    static void tearDownClass() throws IOException {
        // Restore backup
        File current = new File(LIBRARY_FILE);
        if (backupFile != null && backupFile.exists()) {
            if (current.exists()) {
                current.delete();
            }
            Files.move(backupFile.toPath(), current.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            // Clean up test file if no backup existed
            current.delete();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create default library on first load")
    void shouldCreateDefaultLibrary() {
        // Act
        GrhLibraryManager manager = GrhLibraryManager.getInstance();

        // Assert
        assertThat(manager.getCategories()).isNotEmpty();
        assertThat(manager.getCategories()).hasSizeGreaterThanOrEqualTo(3);

        // Verify default categories exist
        boolean hasSurfaces = manager.getCategories().stream()
                .anyMatch(cat -> cat.getName().contains("Superficies"));
        boolean hasWalls = manager.getCategories().stream()
                .anyMatch(cat -> cat.getName().contains("Paredes"));
        boolean hasDecor = manager.getCategories().stream()
                .anyMatch(cat -> cat.getName().contains("Decoracion"));

        assertThat(hasSurfaces).isTrue();
        assertThat(hasWalls).isTrue();
        assertThat(hasDecor).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Should maintain singleton pattern correctly")
    void shouldMaintainSingletonInstance() {
        // Act
        GrhLibraryManager instance1 = GrhLibraryManager.getInstance();
        GrhLibraryManager instance2 = GrhLibraryManager.getInstance();

        // Assert
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    @Order(3)
    @DisplayName("Should return mutable categories list")
    void shouldReturnMutableList() {
        // Arrange
        GrhLibraryManager manager = GrhLibraryManager.getInstance();
        int initialSize = manager.getCategories().size();

        // Act
        GrhCategory tempCategory = new GrhCategory("Temporary Test Category");
        manager.getCategories().add(tempCategory);

        // Assert
        assertThat(manager.getCategories()).hasSize(initialSize + 1);

        // Cleanup
        manager.getCategories().remove(tempCategory);
    }

    @Test
    @Order(4)
    @DisplayName("Default library should have valid structure")
    void defaultLibraryShouldHaveValidStructure() {
        // Arrange
        GrhLibraryManager manager = GrhLibraryManager.getInstance();

        // Assert - check all categories have names
        for (GrhCategory category : manager.getCategories()) {
            assertThat(category.getName()).isNotNull();
            assertThat(category.getName()).isNotEmpty();
        }

        // Assert - check some categories have records
        long categoriesWithRecords = manager.getCategories().stream()
                .filter(cat -> !cat.getRecords().isEmpty())
                .count();
        assertThat(categoriesWithRecords).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("Records should preserve properties")
    void recordsShouldPreserveProperties() {
        // Arrange
        GrhLibraryManager manager = GrhLibraryManager.getInstance();

        // Find walls category (has records with properties)
        GrhCategory walls = manager.getCategories().stream()
                .filter(cat -> cat.getName().contains("Paredes"))
                .findFirst()
                .orElse(null);

        // Assert
        assertThat(walls).isNotNull();
        assertThat(walls.getRecords()).isNotEmpty();

        // Check if any record has autoBlock property
        boolean hasAutoBlock = walls.getRecords().stream()
                .anyMatch(GrhIndexRecord::isAutoBlock);
        assertThat(hasAutoBlock).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Should save and load library correctly")
    void shouldSaveAndLoadLibrary() {
        // Arrange
        GrhLibraryManager manager = GrhLibraryManager.getInstance();

        // Add a custom category
        GrhCategory custom = new GrhCategory("Test Save Category");
        GrhIndexRecord testRecord = new GrhIndexRecord("Test GRH", 9999);
        custom.addRecord(testRecord);
        manager.getCategories().add(custom);
        int sizeAfterAdd = manager.getCategories().size();

        // Act - Save
        manager.save();

        // Verify file exists
        File libraryFile = new File(LIBRARY_FILE);
        assertThat(libraryFile).exists();
        assertThat(libraryFile).isFile();

        // Reload to verify persistence
        manager.load();

        // Assert - category should still be there after reload
        assertThat(manager.getCategories().size()).isEqualTo(sizeAfterAdd);
        boolean hasTestCategory = manager.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals("Test Save Category"));
        assertThat(hasTestCategory).isTrue();

        // Cleanup - remove test category
        manager.getCategories().removeIf(cat -> cat.getName().equals("Test Save Category"));
        manager.save();
    }

    @Test
    @Order(7)
    @DisplayName("Should handle library file creation")
    void shouldHandleFileCreation() {
        // Assert - file should exist after getInstance() and save()
        File libraryFile = new File(LIBRARY_FILE);
        assertThat(libraryFile).exists();
    }
}
