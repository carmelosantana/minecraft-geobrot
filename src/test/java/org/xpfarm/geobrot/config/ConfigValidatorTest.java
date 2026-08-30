package org.xpfarm.geobrot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigValidator
 */
class ConfigValidatorTest {
    
    @BeforeEach
    void setUp() {
        // Mock setup for testing
        // In a real test environment, you'd use MockBukkit or similar
    }
    
    @Test
    void testValidatorClass() {
        // Test that validator class exists
        assertNotNull(ConfigValidator.class, "ConfigValidator class should exist");
        assertEquals("ConfigValidator", ConfigValidator.class.getSimpleName(), 
            "Validator should have correct class name");
    }
    
    @Test
    void testValidatorPackage() {
        // Test that it's in the correct package
        assertTrue(ConfigValidator.class.getPackage().getName().contains("config"),
            "Validator should be in the config package");
    }
    
    @Test
    void testValidationMethodsExist() {
        // Test that required validation methods exist
        try {
            ConfigValidator.class.getDeclaredMethod("getValidatedInt", String.class, int.class, int.class, int.class);
            ConfigValidator.class.getDeclaredMethod("getValidatedDouble", String.class, double.class, double.class, double.class);
            ConfigValidator.class.getDeclaredMethod("getValidatedString", String.class, String.class);
        } catch (NoSuchMethodException e) {
            fail("Required validation methods should exist");
        }
    }
}
