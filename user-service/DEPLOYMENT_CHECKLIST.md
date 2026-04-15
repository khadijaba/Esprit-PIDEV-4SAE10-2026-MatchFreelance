## ✅ DEPLOYMENT CHECKLIST: SendGrid Email Integration

> **Complete this checklist before deploying to production**

---

## **IMMEDIATE ACTIONS (Complete BEFORE Running App)**

### **Phase 1: SendGrid Account Setup** ✅
- [ ] Create SendGrid account: https://signup.sendgrid.com/
- [ ] Generate API key in Settings → API Keys
- [ ] Copy API key to secure location (shows only once)
- [ ] Verify sender email in Settings → Sender Authentication
- [ ] Record verified email (e.g., noreply@example.com)

**Output:**
```
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
VERIFIED_EMAIL=noreply@yourdomain.com
```

---

### **Phase 2: Code Changes** ✅
- [ ] Updated `pom.xml` with sendgrid-java v4.10.1
- [ ] Created `SendGridConfig.java` with Bean initialization
- [ ] Created `SendGridEmailService.java` (replaces old EmailService)
- [ ] Updated `UserService.java` to use SendGridEmailService
- [ ] Updated `application.properties` with SendGrid config
- [ ] Run `mvn clean install`

**Files Modified:**
```
✅ pom.xml
✅ Config/SendGridConfig.java (NEW)
✅ Service/SendGridEmailService.java (NEW)
✅ Service/UserService.java (UPDATED)
✅ resources/application.properties (UPDATED)
```

---

### **Phase 3: Environment Configuration** ✅
- [ ] Set environment variable: `SENDGRID_API_KEY`
- [ ] Verify from email in `application.properties`
- [ ] Add logging configuration (DEBUG level for development)
- [ ] Restart IDE/Terminal after setting env vars

**Windows Setup:**
```cmd
REM Administrator Command Prompt
setx SENDGRID_API_KEY "SG.YOUR_KEY_HERE"
REM Restart IDE after this
```

**Linux/Docker Setup:**
```bash
export SENDGRID_API_KEY="SG.YOUR_KEY_HERE"
# Or in .env file
SENDGRID_API_KEY=SG.YOUR_KEY_HERE
```

---

### **Phase 4: Verification** ✅
- [ ] Start Spring Boot application
- [ ] Check logs for: `✓ SendGrid initialized with API key`
- [ ] If NOT present, see SENDGRID_DEBUG_GUIDE.md
- [ ] No errors during startup

**Expected Logs:**
```
✓ SendGrid initialized with API key: SG.aaabbb...
Application started successfully
```

---

## **LOCAL TESTING (Before Production)**

### **Test 1: Signup with Email Verification**
- [ ] Use Postman to POST `/api/auth/signup`
- [ ] Check Spring Boot logs for email sending
- [ ] Verify email received in inbox/spam
- [ ] Check for "Bienvenue" welcome email
- [ ] Check for verification email with link

**Expected Logs:**
```
📧 Envoi des emails de vérification
✓ Email de bienvenue envoyé
✓ Email de vérification envoyé
📨 Réponse SendGrid: Status Code = 202
```

### **Test 2: Password Reset Flow**
- [ ] POST `/api/auth/reset-password/request`
- [ ] Check email received with reset link
- [ ] Click link or use token in confirm endpoint
- [ ] Verify password changed successfully

### **Test 3: Verify Email Endpoint**
- [ ] Use token from verification email
- [ ] POST `/api/auth/verify-email`
- [ ] Account should be enabled
- [ ] Should be able to login

### **Test 4: Signin with Verified Account**
- [ ] POST `/api/auth/signin`
- [ ] Receive JWT token
- [ ] Token should contain correct role

---

## **TROUBLESHOOTING GUIDE**

### **Problem: API KEY NOT CONFIGURED** ❌
```
❌ SENDGRID API KEY NOT CONFIGURED
Set one of these:
  1. Environment variable: SENDGRID_API_KEY
  2. Application property: sendgrid.api.key
```

**Solution:**
1. Set `SENDGRID_API_KEY` environment variable
2. Restart IDE/Terminal
3. Verify with: `echo %SENDGRID_API_KEY%` (Windows)
4. See SENDGRID_DEBUG_GUIDE.md → Configure API Key

---

### **Problem: HTTP 400 Invalid Email Address** ❌
```
✗ Erreur SendGrid - Status: 400
Body: {"errors": [{"message": "Invalid email address"}]}
```

**Solution:**
1. Verify sender email is registered in SendGrid
2. Check `sendgrid.from.email` in properties
3. Make sure email format is valid (email@example.com)
4. See SENDGRID_DEBUG_GUIDE.md → Verify Sender Email

---

### **Problem: Email Not Received** ❌
```
✓ Email sent successfully (Code 202)
BUT email never arrives
```

**Solution:**
1. Check spam/junk folder
2. Wait 2-5 minutes (delivery delay)
3. Verify recipient email is correct
4. Check SendGrid Mail Activity dashboard
5. Look for bounce/drop notifications

---

### **Problem: Connection Timeout** ❌
```
IOException: Connection timeout
```

**Solution:**
1. Check internet connection
2. Verify SendGrid API endpoint is reachable
3. Check firewall/VPN settings
4. SendGrid status: https://status.sendgrid.com/

---

## **PRODUCTION DEPLOYMENT STEPS**

### **Environment Setup**
```bash
# Set in deployment platform (Docker, Kubernetes, AWS, Heroku, etc.)
SENDGRID_API_KEY=SG.prod_key_xxx
SENDGRID_FROM_EMAIL=noreply@yourdomain.com
SENDGRID_FROM_NAME=Your Company
```

### **Configuration**
```properties
# application-prod.properties
sendgrid.from.email=noreply@yourdomain.com
sendgrid.from.name=Your Company Name

# Update email links from localhost to production
# In SendGridEmailService, update URLs:
# http://localhost:3000 → https://yourfrontend.com
# http://localhost:8080 → https://yourapi.com
```

### **Pre-Production Checklist**
- [ ] Test with actual production domain email
- [ ] Verify DKIM/SPF records set for domain
- [ ] Update email links to production frontend URLs
- [ ] Enable webhooks for delivery tracking
- [ ] Set up monitoring/alerting for failed emails
- [ ] Plan email template versioning strategy
- [ ] Load test: simulate spike in email sends
- [ ] Backup: have alternative email provider ready

---

## **MONITORING & MAINTENANCE**

### **Daily Checks**
- [ ] Monitor email delivery rates via SendGrid dashboard
- [ ] Check application logs for email errors
- [ ] Review bounced/invalid emails
- [ ] Check rate limiting status

### **Weekly Checks**
- [ ] Review failed email logs
- [ ] Verify all email templates rendering correctly
- [ ] Check SendGrid account health
- [ ] Review user complaints about emails

### **Monthly Checks**
- [ ] Analyze email delivery patterns
- [ ] Rotate API keys (security best practice)
- [ ] Update email templates as needed
- [ ] Review and update sender verification

---

## **SECURITY CHECKLIST**

- [ ] API key stored in environment variables ONLY
- [ ] Never commit API key to Git
- [ ] Use separate API keys for dev/prod
- [ ] Rotate API keys every 90 days
- [ ] Limit API key permissions (Mail Send only)
- [ ] Monitor SendGrid access logs
- [ ] Validate all email inputs before processing
- [ ] Never log full email body in production
- [ ] Use HTTPS for all communications
- [ ] Implement rate limiting on email endpoints

---

## **FILES UPDATED/CREATED**

```
Modified:
├── pom.xml                              (Added sendgrid-java dependency)
├── src/main/resources/application.properties  (SendGrid config)
├── src/main/java/Service/UserService.java    (Use SendGridEmailService)
└── src/main/java/Config/WebConfig.java       (No change needed)

Created:
├── src/main/java/Config/SendGridConfig.java  (SendGrid Bean)
├── src/main/java/Service/SendGridEmailService.java  (Main service)
├── SENDGRID_QUICK_SETUP.md             (5-minute setup)
├── SENDGRID_DEBUG_GUIDE.md             (Comprehensive debugging)
├── SENDGRID_TECHNICAL_ANALYSIS.md      (Why HTTP 400 happens)
├── POSTMAN_TESTING_GUIDE.md            (Test cases)
└── DEPLOYMENT_CHECKLIST.md             (This file)

Unchanged (can remove if not used):
└── src/main/java/Service/EmailService.java   (OLD - No longer used)
```

---

## **ROLLBACK PROCEDURE (If Issues)**

If you need to revert to old SimpleMailMessage approach:

1. **Comment out SendGrid config:**
   ```java
   // In SendGridConfig.java
   // @Configuration
   // public class SendGridConfig { ... }
   ```

2. **Switch back to EmailService:**
   ```java
   // In UserService.java
   private final EmailService emailService;  // Instead of SendGridEmailService
   ```

3. **Remove SendGrid from pom.xml:**
   ```xml
   <!-- <dependency>
      <groupId>com.sendgrid</groupId>
      <artifactId>sendgrid-java</artifactId>
   </dependency> -->
   ```

4. **Clean and rebuild:**
   ```bash
   mvn clean install
   ```

**Note:** You'll lose better error logging if rolling back.

---

## **SUPPORT & RESOURCES**

- **SendGrid Docs:** https://docs.sendgrid.com/
- **SendGrid Status:** https://status.sendgrid.com/
- **Java SDK Repo:** https://github.com/sendgrid/sendgrid-java
- **API Reference:** https://docs.sendgrid.com/api-reference/
- **Deliverability Guide:** https://docs.sendgrid.com/ui/sending-email/delivery-optimization/

---

## **SIGN-OFF**

- [ ] All checklist items completed
- [ ] Tests passed locally
- [ ] Documentation reviewed
- [ ] Team briefed on email flow
- [ ] Ready for production deployment

**Deployed By:** ________________
**Date:** ________________
**Notes:** ________________

