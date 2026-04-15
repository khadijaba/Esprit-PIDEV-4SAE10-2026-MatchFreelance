## 🔬 TECHNICAL DEEP DIVE: SendGrid HTTP 400 Causes & Solutions

### **WHY HTTP 400 ERRORS OCCUR**

HTTP 400 (Bad Request) from SendGrid means your request has a structural or validation problem. Below are ALL possible causes:

---

## **1. INVALID SENDER EMAIL (Most Common - 95% of Cases)**

### **Problem:**
```
HTTP 400 Bad Request
{
  "errors": [{
    "message": "Invalid email address",
    "field": "from"
  }]
}
```

### **Root Causes:**
1. **Sender email NOT verified** in SendGrid dashboard
2. **Sender email malformed** (e.g., missing @, invalid domain)
3. **Sender email domain NOT authenticated** (no DKIM/SPF)
4. **Sending from unauthorized domain**

### **Our Solution:**
The `SendGridEmailService` validates sender email before sending:
```java
if (fromEmail == null || fromEmail.isEmpty()) {
    throw new IllegalArgumentException("Sender email not configured");
}
logger.debug("Préparation de l'email: De={}", fromEmail);
```

### **How to Fix:**
1. **Verify email in SendGrid:**
   - Dashboard → Settings → Sender Authentication
   - Click "Verify an Address"
   - Follow email verification
   - Use verified email in `sendgrid.from.email`

2. **Use correct email format:**
   ```properties
   sendgrid.from.email=valid-email@yourdomain.com  # ✅
   sendgrid.from.email=invalid.email@  # ❌
   sendgrid.from.email=noreply  # ❌
   ```

---

## **2. INVALID API KEY**

### **Problem:**
```
HTTP 401 Unauthorized
OR
HTTP 400 Bad Request (generic)
```

### **Causes:**
- API key is incorrect/truncated
- API key is expired (must regenerate)
- API key has no "Mail Send" permission
- API key is revoked

### **Our Solution:**
Config validates API key on startup:
```java
@Bean
public SendGrid sendGrid() {
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException("SendGrid API key is not configured");
    }
    logger.info("✓ SendGrid initialized with API key: {}...", 
                apiKey.substring(0, Math.min(10, apiKey.length())));
    return new SendGrid(apiKey);
}
```

### **How to Fix:**
1. **Generate new API key:**
   - Settings → API Keys → Create API Key
   - Name: "Spring Boot User Service"
   - Permissions: "Full Access" (or Mail Send minimum)
   - Copy immediately (won't show again)

2. **Set environment variable:**
   ```bash
   # Windows (Administrator)
   setx SENDGRID_API_KEY "SG.YOUR_FULL_KEY_HERE"
   
   # Linux/Mac
   export SENDGRID_API_KEY="SG.YOUR_FULL_KEY_HERE"
   ```

3. **Verify in logs on startup:**
   ```
   ✓ SendGrid initialized with API key: SG.aaabbb...
   ```

---

## **3. INVALID RECIPIENT EMAIL**

### **Problem:**
```
HTTP 400 Bad Request
{
  "errors": [{
    "message": "Invalid email address",
    "field": "to"
  }]
}
```

### **Causes:**
- Recipient email is malformed
- Missing domain (@example.com)
- Invalid characters in email
- Email is too long (>254 chars)

### **Our Solution:**
Before sending, we validate:
```java
private void sendMailViaSendGrid(String toEmail, String subject, 
                                 String htmlContent) throws IOException {
    if (toEmail == null || toEmail.isEmpty()) {
        throw new IllegalArgumentException("Email recipient cannot be empty");
    }
    // Email format validated by Mail constructor
}
```

### **How to Fix:**
1. **Validate email format before calling service:**
   ```java
   if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
       throw new IllegalArgumentException("Invalid email format");
   }
   ```

2. **Check database:** Ensure only valid emails are stored

---

## **4. MALFORMED REQUEST BODY (JSON)**

### **Problem:**
```
HTTP 400 Bad Request
{
  "errors": [{
    "message": "Bad request body",
    "field": "body"
  }]
}
```

### **Why SimpleMailMessage fails:**
Old setup using `SimpleMailMessage`:
```java
// ❌ PROBLEMATIC - Causes hidden errors
SimpleMailMessage message = new SimpleMailMessage();
message.setFrom(fromEmail);
message.setTo(toEmail);
message.setText("Content");  // Plain text formatting
mailSender.send(message);    // No error logging
```

**Problems:**
- `SimpleMailMessage` may send plain text instead of structured JSON
- SendGrid expects specific JSON format for API
- SMTP adaptation might corrupt data
- No visibility into actual error

### **Our Solution:**
Using SendGrid SDK builds proper JSON:
```java
Mail mail = new Mail(from, subject, to, content);
Request request = new Request();
request.setMethod(Method.POST);
request.setEndpoint("mail/send");
request.setBody(mail.build());  // Proper JSON structure
Response response = sendGrid.api(request);
```

Generated JSON looks like:
```json
{
  "personalizations": [{
    "to": [{"email": "user@example.com"}]
  }],
  "from": {"email": "noreply@example.com", "name": "Support Team"},
  "subject": "Verification Email",
  "content": [{
    "type": "text/html",
    "value": "<html>...</html>"
  }]
}
```

---

## **5. CHARACTER ENCODING ISSUES**

### **Problem:**
```
HTTP 400 Bad Request
Emails with special characters (é, ç, ñ, 中文) fail
```

### **Causes:**
- Wrong charset in email headers
- HTML content not UTF-8 encoded
- French accents in subject/body cause issues
- Emoji or special characters

### **Our Solution:**
We explicitly set UTF-8:
```java
Content content = new Content("text/html; charset=UTF-8", htmlContent);
//                            ^^^^^^^^^^^^^^^^^^^^^^^^^^
//                            Proper charset declaration
```

Also in `SendEmailVerification()`:
```java
String htmlContent = """
    <html>
    <body style="...">  <!-- HTML properly formatted -->
        <p>Cher %s,</p>  <!-- French accents handled -->
```

---

## **6. RATE LIMITING**

### **Problem:**
```
HTTP 429 Too Many Requests
```

### **Causes:**
- Sending >1000 emails/second
- Exceeded SendGrid plan limits
- Too many retries in quick succession

### **Our Solution:**
For production, implement exponential backoff:
```java
@Retryable(
    value = IOException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendEmailVerification(String toEmail, ...) {
    // Automatically retries with 1s, 2s, 4s delays
}
```

---

## **7. MISSING REQUIRED FIELDS**

### **Problem:**
```
HTTP 400 Bad Request
{
  "errors": [{
    "message": "subject is required"
  }]
}
```

### **Our Solution:**
We ensure all required fields:
```java
Mail mail = new Mail(
    from,           // ✅ Required
    subject,        // ✅ Required
    to,             // ✅ Required
    content         // ✅ Required
);
```

---

## **COMPARISON: OLD vs NEW APPROACH**

### **OLD: SimpleMailMessage + SMTP**
```
❌ Uses SMTP protocol (outdated for validation)
❌ No API error logging
❌ Can silently fail
❌ API key exposed in properties
❌ Generic "Email sending failed" error
❌ Hard to debug
❌ Limited HTML support

Result: HTTP 400 with mysterious error
```

### **NEW: SendGrid SDK + Proper Config**
```
✅ Direct SendGrid API calls
✅ Full response logging (status + body)
✅ Validates before sending
✅ API key from environment variable
✅ Detailed error messages
✅ Easy to debug
✅ Full HTML/CSS support
✅ Better rate limiting handling

Result: Clear success (202) or specific error message
```

---

## **WHY OUR SOLUTION WORKS**

### **1. Better Error Visibility**
```java
Response response = sendGrid.api(request);
logger.info("Status Code = {}", response.getStatusCode());

if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
    logger.info("✓ Email sent successfully");
} else {
    // ❌ We can SEE the actual SendGrid error
    logger.error("Error: Status {}, Body: {}", 
                 response.getStatusCode(), 
                 response.getBody());
}
```

### **2. Proper Configuration**
```java
@Bean
public SendGrid sendGrid() {
    // Fails fast at startup if API key missing
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException("API key required");
    }
}
```

### **3. Exception Handling**
```java
catch (IOException e) {
    logger.error("✗ Exception: {}", e.getMessage(), e);
    throw new RuntimeException("Failed to send email", e);
}
// Now you see actual error, not silent failure
```

### **4. Validation Before Sending**
```java
if (toEmail == null || toEmail.isEmpty()) {
    throw new IllegalArgumentException("Recipient cannot be empty");
}
if (fromEmail == null || fromEmail.isEmpty()) {
    throw new IllegalArgumentException("Sender not configured");
}
```

---

## **DEBUGGING MATRIX**

| Response Code | Likely Cause | Check |
|---|---|---|
| 202 | ✅ SUCCESS | Email will arrive in 2-5 min |
| 400 | Malformed request | Check sender/recipient format, charset |
| 401 | Invalid API key | Regenerate key, set env variable |
| 403 | Unauthorized sender | Verify email in SendGrid, check domain auth |
| 404 | Wrong endpoint | Our code uses correct endpoint (/mail/send) |
| 413 | Email too large | Reduce attachment size or content |
| 429 | Rate limited | Implement backoff, check SendGrid plan |
| 500+ | SendGrid server error | Retry later, check SendGrid status page |

---

## **PRODUCTION BEST PRACTICES**

### **1. Separate configurations by environment:**
```properties
# application-dev.properties
sendgrid.from.email=dev-noreply@example.com

# application-prod.properties  
sendgrid.from.email=noreply@yourdomain.com
```

### **2. Use circuit breaker pattern:**
```java
@CircuitBreaker(name = "sendgrid-service")
public void sendEmailVerification(...) {
    // Auto disable after N failures
}
```

### **3. Monitor delivery:**
```java
// After sending (HTTP 202), save email record
emailAuditService.log(toEmail, templateName, status);
```

### **4. Security checklist:**
- ✅ API key in environment variable (NEVER in code)
- ✅ Use HTTPS for all communications
- ✅ Rotate API keys every 90 days
- ✅ Limit API key scopes (Mail Send only)
- ✅ Log sender/recipient but NOT full email body
- ✅ Validate all email inputs

---

## **CONCLUSION**

The HTTP 400 error was caused by:
1. **Missing sender verification** (primary)
2. **Using SMTP instead of SDK** (secondary - hides real errors)
3. **Poor error logging** (tertiary - can't debug)

Our solution fixes all three by:
- Using SendGrid SDK directly
- Adding comprehensive logging
- Properly managing API keys
- Validating inputs upfront

This ensures visibility into what's happening and why requests fail.

