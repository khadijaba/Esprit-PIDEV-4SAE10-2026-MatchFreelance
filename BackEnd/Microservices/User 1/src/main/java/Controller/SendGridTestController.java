package Controller;

import Service.SendGridEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class SendGridTestController {

    @Autowired
    private SendGridEmailService emailService;

    @GetMapping("/sendgrid")
    public String testSendGrid() {
        return emailService.testSendGridConfiguration();
    }
    
    @GetMapping("/send-test-email")
    public String sendTestEmail() {
        try {
            emailService.sendWelcomeEmail("bejaoui.mohamedamine@esprit.tn", "Test", "User");
            return "✅ Test email sent successfully!";
        } catch (Exception e) {
            return "❌ Error sending test email: " + e.getMessage();
        }
    }
}
