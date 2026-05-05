## 🚨 SendGrid HTTP 400 - Debugging Guide

### **QUICK FIX CHECKLIST**

#### **1. ✅ Verify Sender Email in SendGrid Dashboard (MOST COMMON ISSUE)**
```
1. Go to https://app.sendgrid.com/
2. Click Settings → Sender Authentication
3. Click "Verify an Address"
4. Follow the verification steps
5. Use this verified email in application.properties: sendgrid.from.email=your-verified-email@domain.com
```

⚠️ **ERROR SIGNS**: If sender email is not verified, SendGrid returns:
- `HTTP 400: Invalid email address`
- `HTTP 403: Unauthorized email sender`

---

#### **2. ✅ Configure API Key Correctly**

**BEST PRACTICE: Use Environment Variable (SECURE)**
```bash
# Windows - Command Prompt (Administrator)
setx SENDGRID_API_KEY "SG.your_actual_api_key_here"
# Restart your IDE/Terminal after setting this

# Windows - PowerShell
$env:SENDGRID_API_KEY = "SG.your_actual_api_key_here"

# Linux/Mac
export SENDGRID_API_KEY="SG.your_actual_api_key_here"
```

**FALLBACK: Use application.properties**
```properties
sendgrid.api.key=SG.your_actual_api_key_here
```

**Generate API Key:**
1. Go to https://app.sendgrid.com/
2. Settings → API Keys → Create API Key
3. Name it: "Spring Boot User Service"
4. Select "Full Access" or at minimum "Mail Send"
5. Copy the key immediately (you won't see it again)

---

#### **3. ✅ Verify Configuration in application.properties**

```properties
# SendGrid API Configuration
sendgrid.api.key=${SENDGRID_API_KEY:}  # Reads from environment variable
sendgrid.from.email=noreply@example.com  # MUST be verified
sendgrid.from.name=Support Team

# Enable DEBUG logging
logging.level.Service.SendGridEmailService=DEBUG
logging.level.Config.SendGridConfig=DEBUG
```

---

### **COMMON HTTP 400 ERRORS & SOLUTIONS**

| Error Message | Cause | Fix |
|---|---|---|
| `Invalid email address` | Sender email not verified OR malformed | Verify sender in SendGrid dashboard |
| `Unauthorized email sender` | Email domain not authenticated | Add CNAME records or verify single sender |
| `Invalid to address` | Recipient email is malformed | Check recipient email format |
| `Bad Request` (generic) | JSON formatting issue OR missing fields | Use our SendGridEmailService (handles this) |
| `Authentication failed` | Invalid API key OR API key has expired | Generate new API key in SendGrid dashboard |
| `Too many requests` | Rate limiting (1000/sec limit) | Implement exponential backoff |

---

### **TESTING WITH POSTMAN**

#### **Test Signup Endpoint**

**Request:**
```http
POST http://localhost:8081/api/auth/signup
Content-Type: application/json

{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "jean.dupont@example.com",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123",
  "address": "123 Rue de Paris",
  "birthDate": "1990-01-15",
  "role": "FREELANCER"
}
```

**Expected Response (200 OK):**
```json
"Compte créé avec succès pour : jean.dupont@example.com"
```

**Check Logs for:**
```
✓ Email de bienvenue envoyé à: jean.dupont@example.com
✓ Email de vérification envoyé à: jean.dupont@example.com
📨 Réponse SendGrid: Status Code = 202
```

---

#### **Test Password Reset Endpoint**

**Request:**
```http
POST http://localhost:8081/api/auth/reset-password/request
Content-Type: application/json

{
  "email": "jean.dupont@example.com"
}
```

**Expected Response (200 OK):**
```json
"Un email de réinitialisation a été envoyé à jean.dupont@example.com"
```

**Check Logs for:**
```
✓ Email de réinitialisation envoyé à: jean.dupont@example.com
📨 Réponse SendGrid: Status Code = 202
```

---

#### **Test Email Verification Resend**

**Request:**
```http
POST http://localhost:8081/api/auth/resend-verification
Content-Type: application/json

{
  "email": "jean.dupont@example.com"
}
```

---

### **DEBUG LOGGING ANALYSIS**

#### **✅ Success Logs:**
```
✓ SendGrid initialized with API key: SG.aaabbb...
✓ Email de bienvenue envoyé à: user@example.com
📨 Réponse SendGrid: Status Code = 202
✓ Email sent successfully to user@example.com
```

#### **❌ Error Logs to Watch For:**
```
✗ SENDGRID API KEY NOT CONFIGURED
✗ ERREUR lors de l'envoi de l'email: [SPECIFIC ERROR MESSAGE]
✗ Erreur SendGrid - Status: 400, Body: {"errors": [{"message": "Invalid email address"}]}
```

---

### **STEP-BY-STEP DEBUGGING PROCESS**

#### **STEP 1: Verify API Key is Set**

Open your Spring Boot console and look for:
```
✓ SendGrid initialized with API key: SG.aaabbb...
```

If NOT present:
```
❌ SENDGRID API KEY NOT CONFIGURED
Set one of these:
  1. Environment variable: SENDGRID_API_KEY
  2. Application property: sendgrid.api.key
```

**FIX**: Set environment variable and restart IDE/Terminal

---

#### **STEP 2: Verify Sender Email Configuration**

Check logs:
```
DEBUG: Préparation de l'email: De=noreply@example.com → À=user@example.com
```

If sender email is wrong:
1. Update `sendgrid.from.email` in application.properties
2. Make sure that email is verified in SendGrid dashboard
3. Restart application

---

#### **STEP 3: Check SendGrid Response Status**

SUCCESS = HTTP 202 (Accepted):
```
📨 Réponse SendGrid: Status Code = 202
✓ Email sent successfully to user@example.com
```

FAILURE = HTTP 400+:
```
✗ Erreur SendGrid - Status: 400
Body: {"errors": [{"message": "Invalid email address"}]}
```

---

#### **STEP 4: Verify Sender Email in SendGrid Dashboard**

1. Go to: https://app.sendgrid.com/
2. Settings → Sender Authentication
3. Look for your sender email (noreply@example.com)
4. If you see 🔴 **Unverified**, click "Verify" and follow steps
5. If you see 🟢 **Verified**, proceed to step 5

---

#### **STEP 5: Test SendGrid API Directly**

If above steps don't work, test SendGrid API directly using curl:

```bash
curl --request POST \
  --url https://api.sendgrid.com/v3/mail/send \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer SG.YOUR_API_KEY' \
  --data '{
    "personalizations": [
      {
        "to": [{"email": "test@example.com"}]
      }
    ],
    "from": {"email": "noreply@example.com"},
    "subject": "Test Email",
    "content": [{"type": "text/plain", "value": "Test"}]
  }'
```

**Expected Response:**
```json
{"errors":[]}  (Empty means success - 202)
```

**Error Response Example:**
```json
{
  "errors": [
    {
      "message": "Unauthorized",
      "field": "from",
      "help": "http://help.sendgrid.com/errors"
    }
  ]
}
```

---

### **PRODUCTION ENVIRONMENT SETUP**

#### **Environment Variables (Linux/Docker)**
```bash
# .env file or deployment config
SENDGRID_API_KEY=SG.your_production_key
```

#### **application-prod.properties**
```properties
# SendGrid
sendgrid.from.email=noreply@yourproductiondomain.com
sendgrid.from.name=Your Company Name

# Logging
logging.level.Service.SendGridEmailService=INFO
logging.level.Config.SendGridConfig=INFO
```

#### **Docker Compose Example**
```yaml
services:
  user-service:
    environment:
      - SENDGRID_API_KEY=${SENDGRID_API_KEY}
      - spring.profiles.active=prod
```

---

### **SECURITY BEST PRACTICES**

✅ **DO:**
- Use environment variables for API keys
- Regenerate API keys regularly
- Limit API key scopes (Mail Send only, no deletions)
- Monitor SendGrid logs for suspicious activity
- Use HTTPS everywhere
- Validate email addresses before sending

❌ **DON'T:**
- Hardcode API keys in application.properties
- Commit API keys to Git
- Share API keys in chat/email
- Use same API key for multiple environments
- Log full API keys (only first 10 chars)
- Send emails to unverified test addresses

---

### **SENDGRID WEBHOOK SETUP (Optional)**

After fixing email sending, consider adding webhooks to track delivery:

1. Go to: https://app.sendgrid.com/
2. Settings → Mail Send Settings → Event Webhook
3. Set webhook URL: `https://your-domain.com/api/webhooks/sendgrid`
4. Enable: Opened, Clicked, Bounce, Dropped, Unsubscribed
5. Implement webhook receiver in your backend

---

### **COMMON MISTAKES TO AVOID**

1. **❌ Forgetting to verify sender email** → 400 error
2. **❌ Using SMTP instead of SDK** → Generic errors, hard to debug
3. **❌ Hardcoding API keys** → Major security risk
4. **❌ Not logging response bodies** → Can't see actual errors
5. **❌ Reusing API keys across environments** → Key compromise spreads
6. **❌ Testing with personal email as sender** → Not verified
7. **❌ Using SimpleMailMessage** → Limited error info
8. **❌ Not handling exceptions** → Silent failures

---

### **SUPPORT RESOURCES**

- **SendGrid Documentation**: https://docs.sendgrid.com/
- **SendGrid Java SDK**: https://github.com/sendgrid/sendgrid-java
- **API Reference**: https://docs.sendgrid.com/api-reference/
- **Security Best Practices**: https://docs.sendgrid.com/ui/account-and-settings/how-to-set-up-domain-authentication
- **Troubleshooting**: https://docs.sendgrid.com/ui/account-and-settings/troubleshooting

