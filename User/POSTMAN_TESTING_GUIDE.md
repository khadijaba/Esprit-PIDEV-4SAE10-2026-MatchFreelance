## 📮 SendGrid Email Integration - Postman Testing Guide

> **Download Postman:** https://www.postman.com/downloads/

---

## **POSTMAN ENVIRONMENT SETUP**

### **Create New Environment:**

1. Open Postman
2. Click: **Environments** (left sidebar)
3. Click: **Create New**
4. Name: `SendGrid Integration - Dev`
5. Add variables:

```
Variable Name          | Initial Value
———————————————————————|————————————————————————
base_url              | http://localhost:8081
user_email            | test-user@example.com
user_firstname        | Jean
user_lastname         | Dupont
user_password         | SecurePassword123
user_address          | 123 Rue de Paris
user_birthdate        | 1990-01-15
user_role             | FREELANCER
```

6. Save the environment
7. Select it in top-right dropdown before testing

---

## **TEST CASE 1: USER SIGNUP (With Email Verification)**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/signup`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "firstName": "{{user_firstname}}",
  "lastName": "{{user_lastname}}",
  "email": "{{user_email}}",
  "password": "{{user_password}}",
  "confirmPassword": "{{user_password}}",
  "address": "{{user_address}}",
  "birthDate": "{{user_birthdate}}",
  "role": "{{user_role}}"
}
```

### **Expected Response (200 OK):**
```json
"Compte créé avec succès pour : test-user@example.com"
```

### **Expected Logs (Spring Boot Console):**
```
✓ SendGrid initialized with API key: SG.aaabbb...
📧 Envoi des emails de vérification pour: test-user@example.com
✓ Email de bienvenue envoyé à: test-user@example.com
✓ Email de vérification envoyé à: test-user@example.com
📨 Réponse SendGrid: Status Code = 202
✓ Email sent successfully to test-user@example.com
```

### **What Happens Behind Scenes:**
1. ✅ User account created in database
2. ✅ Account set as `enabled = false` (pending verification)
3. ✅ Welcome email sent
4. ✅ Verification token generated
5. ✅ Verification email sent with token

### **If Emails Don't Arrive:**
1. Check spam folder
2. Look at Spring Boot logs for errors
3. See SENDGRID_DEBUG_GUIDE.md → Troubleshooting

---

## **TEST CASE 2: REQUEST PASSWORD RESET**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/reset-password/request`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "email": "{{user_email}}"
}
```

### **Expected Response (200 OK):**
```json
"Un email de réinitialisation a été envoyé à test-user@example.com"
```

### **Expected Logs:**
```
✓ Email de réinitialisation envoyé à: test-user@example.com
📨 Réponse SendGrid: Status Code = 202
```

### **What Email Should Contain:**
- Subject: "Réinitialisation de votre mot de passe"
- Button: "Réinitialiser le mot de passe"
- Expiration: 15 minutes
- Link format: `http://localhost:3000/reset-password?token=UUID`

---

## **TEST CASE 3: CONFIRM PASSWORD RESET**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/reset-password/confirm`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "token": "UUID-FROM-EMAIL-LINK",
  "newPassword": "NewPassword456",
  "confirmPassword": "NewPassword456"
}
```

**Note:** Replace `UUID-FROM-EMAIL-LINK` with actual token from email

### **Expected Response (200 OK):**
```json
"Mot de passe réinitialisé avec succès"
```

### **Expected Logs:**
```
✓ Email de confirmation de réinitialisation envoyé à: test-user@example.com
📨 Réponse SendGrid: Status Code = 202
```

---

## **TEST CASE 4: VERIFY EMAIL**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/verify-email`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "email": "{{user_email}}",
  "verificationToken": "UUID-FROM-EMAIL-LINK"
}
```

**Note:** Replace `UUID-FROM-EMAIL-LINK` with actual token from verification email

### **Expected Response (200 OK):**
```json
"Email vérifié avec succès. Votre compte est maintenant activé."
```

### **What Happens:**
1. Verification token validated
2. User account enabled (`enabled = true`)
3. Account is now active

---

## **TEST CASE 5: RESEND VERIFICATION EMAIL**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/resend-verification`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "email": "{{user_email}}"
}
```

### **Expected Response (200 OK):**
```json
"Un nouvel email de vérification a été envoyé à test-user@example.com"
```

### **Expected Logs:**
```
✓ Email de renvoi de vérification envoyé à: test-user@example.com
📨 Réponse SendGrid: Status Code = 202
```

---

## **TEST CASE 6: USER SIGN IN**

### **Request Details:**
- **Method:** POST
- **URL:** `{{base_url}}/api/auth/signin`
- **Headers:**
  ```
  Content-Type: application/json
  ```

### **Request Body:**
```json
{
  "email": "{{user_email}}",
  "password": "{{user_password}}"
}
```

### **Expected Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "test-user@example.com",
  "role": "FREELANCER",
  "message": "Redirection vers le Dashboard Freelancer"
}
```

**Note:** User must be verified first (email verification completed)

---

## **TEST CASE 7: GET ALL USERS (Admin Only)**

### **Request Details:**
- **Method:** GET
- **URL:** `{{base_url}}/api/admin/users`
- **Headers:**
  ```
  Authorization: Bearer YOUR_JWT_TOKEN
  ```

### **Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "firstName": "Jean",
    "lastName": "Dupont",
    "email": "test-user@example.com",
    "role": "FREELANCER",
    "enabled": true,
    "birthDate": "1990-01-15",
    "address": "123 Rue de Paris"
  }
]
```

---

## **ERROR SCENARIOS TO TEST**

### **Test 1: Duplicate Email Registration**
```json
{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "already-exists@example.com",  // Already in DB
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123",
  "address": "123 Rue de Paris",
  "birthDate": "1990-01-15",
  "role": "FREELANCER"
}
```
**Expected:** 400 - "Cet email est déjà utilisé"

---

### **Test 2: Password Mismatch**
```json
{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "new-user@example.com",
  "password": "SecurePassword123",
  "confirmPassword": "DifferentPassword456",  // ❌ Doesn't match
  "address": "123 Rue de Paris",
  "birthDate": "1990-01-15",
  "role": "FREELANCER"
}
```
**Expected:** 400 - "Les mots de passe ne correspondent pas"

---

### **Test 3: Admin Creation via Signup (Not Allowed)**
```json
{
  "firstName": "John",
  "lastName": "Admin",
  "email": "fake-admin@example.com",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123",
  "address": "456 Admin Ave",
  "birthDate": "1985-05-20",
  "role": "ADMIN"  // ❌ Can't create ADMIN via signup
}
```
**Expected:** 400 - "Impossible de créer un compte admin via l'inscription"

---

### **Test 4: Invalid Email Format in SignIn**
```json
{
  "email": "not-an-email",  // ❌ No @ symbol
  "password": "SecurePassword123"
}
```
**Expected:** 400 - Validation error

---

### **Test 5: Wrong Password SignIn**
```json
{
  "email": "test-user@example.com",
  "password": "WrongPassword"  // ❌
}
```
**Expected:** 401 - "Email ou mot de passe incorrect"

---

## **DEBUGGING: MONITORING EMAIL DELIVERY**

### **Check SendGrid Delivery Status:**
1. Open: https://app.sendgrid.com/
2. Go: Mail Activity
3. Search by recipient email
4. View detailed status

---

### **Check Application Logs:**
```bash
# Terminal where Spring Boot runs
# Look for these patterns:

# ✅ SUCCESS
✓ SendGrid initialized
✓ Email sent successfully
📨 Status Code = 202

# ❌ FAILURE
✗ SENDGRID API KEY NOT CONFIGURED
✗ ERREUR lors de l'envoi
✗ Erreur SendGrid - Status: 400
```

---

## **POSTMAN COLLECTION JSON (Import This)**

```json
{
  "info": {
    "name": "SendGrid Email Integration Tests",
    "description": "Test cases for user registration, password reset, and email verification"
  },
  "item": [
    {
      "name": "1. Signup (with email verification)",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/auth/signup",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"firstName\":\"{{user_firstname}}\",\"lastName\":\"{{user_lastname}}\",\"email\":\"{{user_email}}\",\"password\":\"{{user_password}}\",\"confirmPassword\":\"{{user_password}}\",\"address\":\"{{user_address}}\",\"birthDate\":\"{{user_birthdate}}\",\"role\":\"{{user_role}}\"}"
        }
      }
    },
    {
      "name": "2. Request Password Reset",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/auth/reset-password/request",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\":\"{{user_email}}\"}"
        }
      }
    },
    {
      "name": "3. Resend Verification Email",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/auth/resend-verification",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\":\"{{user_email}}\"}"
        }
      }
    }
  ]
}
```

---

## **QUICK TEST CHECKLIST**

- [ ] Have you set `SENDGRID_API_KEY` environment variable?
- [ ] Have you verified sender email in SendGrid dashboard?
- [ ] Is Spring Boot running on port 8081?
- [ ] Are you using the correct Postman environment?
- [ ] Can you see logs with "✓ SendGrid initialized"?
- [ ] Are emails arriving in inbox/spam folder?
- [ ] Are response status codes 200/202?

If all above are YES, integration is working! 🎉

