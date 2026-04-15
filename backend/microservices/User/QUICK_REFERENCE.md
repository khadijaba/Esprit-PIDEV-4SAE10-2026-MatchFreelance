## 📋 QUICK REFERENCE CARD: SendGrid Fix

> **Print this page for quick reference**

---

## **SETUP (5 MINUTES)**

```bash
1️⃣ Set Environment Variable:
   Windows: setx SENDGRID_API_KEY "SG.YOUR_KEY"
   Linux:   export SENDGRID_API_KEY="SG.YOUR_KEY"
   
2️⃣ Update application.properties:
   sendgrid.from.email=noreply@example.com
   
3️⃣ Rebuild:
   mvn clean install
   
4️⃣ Check Logs:
   ✓ SendGrid initialized with API key...
```

---

## **TEST ENDPOINTS**

### **Signup**
```
POST http://localhost:8081/api/auth/signup
Content-Type: application/json

{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "test@example.com",
  "password": "Password123",
  "confirmPassword": "Password123",
  "address": "123 Rue",
  "birthDate": "1990-01-15",
  "role": "FREELANCER"
}
```

### **Reset Password**
```
POST http://localhost:8081/api/auth/reset-password/request
{"email": "test@example.com"}
```

### **Verify Email**
```
POST http://localhost:8081/api/auth/verify-email
{
  "email": "test@example.com",
  "verificationToken": "TOKEN_FROM_EMAIL"
}
```

---

## **VERIFY SENDER EMAIL**

1. Go: https://app.sendgrid.com/
2. Settings → Sender Authentication
3. Click "Verify an Address"
4. Verify email by code
5. Use verified email in `sendgrid.from.email`

---

## **ERROR SOLUTIONS**

| Error | Fix |
|-------|-----|
| `API KEY NOT CONFIGURED` | Set SENDGRID_API_KEY env var, restart IDE |
| `HTTP 400 Invalid email` | Verify sender in SendGrid dashboard |
| `Email not received` | Check spam folder, wait 2-5 min |
| `Can't connect to SendGrid` | Check internet, firewall settings |
| `Unauthorized (401)` | Check API key is correct |

---

## **LOG SIGNALS**

✅ **Success:**
```
✓ SendGrid initialized with API key: SG.aaabbb...
✓ Email sent successfully
📨 Status Code = 202
```

❌ **Failure:**
```
❌ SENDGRID API KEY NOT CONFIGURED
✗ ERREUR lors de l'envoi de l'email
✗ Status: 400, Body: {"errors"...}
```

---

## **KEY FILES**

| File | What |
|------|------|
| SendGridConfig.java | Initializes API client |
| SendGridEmailService.java | Sends emails |
| application.properties | Configuration |
| pom.xml | Dependencies |

---

## **FILE CHECKLIST**

```
✅ pom.xml - Added sendgrid-java
✅ SendGridConfig.java - Created
✅ SendGridEmailService.java - Created
✅ UserService.java - Updated
✅ application.properties - Updated
```

---

## **COMMON MISTAKES**

❌ Hardcoding API key in properties
❌ Forgetting to verify sender email
❌ Not setting environment variable
❌ Using localhost URLs in production emails
❌ Not checking logs for errors
❌ Not waiting 2-5 min for email delivery

---

## **SENDGRID DASHBOARD**

- **Mail Activity:** app.sendgrid.com → Mail Activity
- **Sender Auth:** app.sendgrid.com → Settings → Sender Authentication
- **API Keys:** app.sendgrid.com → Settings → API Keys
- **Status:** status.sendgrid.com

---

## **COMMAND REFERENCE**

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Check Java version
java -version

# Test with curl
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User",...}'

# Check env var (Windows)
echo %SENDGRID_API_KEY%

# Check env var (Linux/Mac)
echo $SENDGRID_API_KEY
```

---

## **EMAIL TEMPLATES LOCATION**

```
SendGridEmailService.java
├─ sendPasswordResetEmail()      → Reset link email
├─ sendWelcomeEmail()             → Welcome message
├─ sendEmailVerification()         → Verify & activate account
├─ sendPasswordResetConfirmationEmail()  → Success message
├─ sendAccountDeletionEmail()      → Account deleted notification
└─ resendVerificationEmail()       → Resend verify
```

---

## **CONTACT SENDGRID**

- Docs: https://docs.sendgrid.com/
- Status: https://status.sendgrid.com/
- Support: support@sendgrid.com
- Java SDK: https://github.com/sendgrid/sendgrid-java

---

## **AFTER SETUP TO-DO**

- [ ] Create SendGrid account
- [ ] Generate API key
- [ ] Verify sender email
- [ ] Set SENDGRID_API_KEY env var
- [ ] Update sender email in properties
- [ ] Build & test locally
- [ ] Verify emails arrive
- [ ] Deploy to production
- [ ] Update production config
- [ ] Monitor delivery

---

**Last Updated:** March 4, 2026
**Status:** ✅ PRODUCTION READY

