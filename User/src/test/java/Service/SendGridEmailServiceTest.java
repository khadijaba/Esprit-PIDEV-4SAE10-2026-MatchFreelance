package Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SendGridEmailServiceTest {

    private SendGridEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new SendGridEmailService(null); // SendGrid client not needed for JSON test
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Team");
    }

    @Test
    void testBuildEmailJson() throws Exception {
        String toEmail = "recipient@example.com";
        String subject = "Test Subject";
        String htmlContent = "<html><body><h1>Hello World</h1></body></html>";

        // Use reflection to access the private method
        String json = (String) ReflectionTestUtils.invokeMethod(
            emailService, 
            "buildEmailJson", 
            toEmail, 
            "Test Team", 
            toEmail, 
            subject, 
            htmlContent
        );

        assertNotNull(json);
        assertTrue(json.contains("\"email\":\"recipient@example.com\""));
        assertTrue(json.contains("\"subject\":\"Test Subject\""));
        assertTrue(json.contains("\"type\":\"text/html\""));
        assertTrue(json.contains("\"value\":"));
        assertTrue(json.contains("<h1>Hello World</h1>"));
        
        // Verify it's valid JSON
        assertDoesNotThrow(() -> {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        });
    }

    @Test
    void testBuildEmailJsonWithSpecialCharacters() throws Exception {
        String toEmail = "test+tag@example.com";
        String subject = "Subject with \"quotes\" and 'apostrophes'";
        String htmlContent = "<html><body>Hello & welcome to \"test\"!</body></html>";

        String json = (String) ReflectionTestUtils.invokeMethod(
            emailService, 
            "buildEmailJson", 
            toEmail, 
            "Test Team", 
            toEmail, 
            subject, 
            htmlContent
        );

        assertNotNull(json);
        
        // Verify it's valid JSON even with special characters
        assertDoesNotThrow(() -> {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        });
    }
}
