# Email Verification Testing Guide (Code-Based System)

## Overview
This guide helps you test the email verification functionality when you're offline and cannot receive actual emails. The system now uses 6-digit verification codes instead of clickable links.

## How It Works
When a user registers, the system:
1. Creates a user account with `enabled = false`
2. Generates a 6-digit verification code
3. Logs the code in the console for offline testing
4. Sends the verification code via email (which will fail offline, but that's expected)

## Testing Steps

### 1. Start Your Backend Server
```bash
cd User
mvn spring-boot:run
```

### 2. Start Your Angular Frontend
```bash
cd angular
ng serve
```

### 3. Register a New User
1. Go to `http://localhost:4200/register`
2. Fill out the registration form
3. Submit the form

### 4. Get the Verification Code
Check your backend console logs. You'll see messages like:
```
📧 ENVOI D'EMAILS DÉSACTIVÉ POUR TEST - Code généré pour: user@example.com
🔑 DEBUG CODE (offline testing): 123456 for email: user@example.com
```

Copy the 6-digit code from the `🔑 DEBUG CODE` line.

### 5. Verify the Email
#### Option A: Direct URL with Email Pre-filled
1. Go to: `http://localhost:4200/verify-email?email=user@example.com`
2. Replace `user@example.com` with your registered email
3. Enter the 6-digit verification code from the logs
4. Click "Verify Email"

#### Option B: Manual Form
1. Go to `http://localhost:4200/verify-email`
2. Enter your email address
3. Enter the 6-digit verification code from the logs
4. Click "Verify Email"

### 6. Verification Success
- If successful, you'll see a success message
- The user's `enabled` field in the database will be set to `true`
- You'll be redirected to the login page after 2 seconds

### 7. Test Login
1. Go to `http://localhost:4200/login`
2. Login with the verified credentials
3. You should now be able to access the system

## Resend Verification Code (If Needed)
If you need a new code:
1. Go to `http://localhost:4200/verify-email`
2. Enter your email
3. Click "Resend verification code"
4. Check the backend logs for the new code:
```
🔑 DEBUG RESEND CODE (offline testing): 654321 for email: user@example.com
```

## Database Verification
To verify the `enabled` field was updated:
1. Check your database (H2 console, MySQL, etc.)
2. Look at the `users` table
3. The `enabled` column should be `true` for verified users

## Common Issues

### Code Not Found
- Make sure you're using the exact 6-digit code from the logs
- Codes expire after 24 hours
- Each new registration or resend generates a new code

### Invalid Code Format
- Verification codes must be exactly 6 digits
- The frontend will validate this automatically
- Example: `123456` (valid), `abc123` (invalid)

### Email Already Verified
- Once `enabled` is set to `true`, you cannot verify the same email again
- You'll get an error: "Ce compte est déjà vérifié"

### Backend Not Running
- Make sure your Spring Boot application is running
- Check the console for any errors

## Code Format and Validation
- **Format**: 6 digits (e.g., "123456")
- **Storage**: Codes are stored in memory mapped by email
- **Expiration**: 24 hours from generation
- **Validation**: Frontend validates format, backend validates against stored code

## Security Notes
- The debug code logging is only for development/testing
- In production, remove or comment out the `🔑 DEBUG CODE` log lines
- Verification codes are stored in memory and will be lost on server restart
- Each email can only have one active code at a time

## Production Deployment
Before deploying to production:
1. Remove the debug code logging lines from `UserService.java`
2. Configure your email service (SendGrid) properly
3. Test with actual email delivery
4. Consider implementing rate limiting for code requests

## Email Template Preview
The email template now shows:
- A prominently displayed 6-digit code
- Step-by-step instructions
- Clear expiration information
- Professional styling with the code highlighted

## Differences from Token System
- **Before**: Long UUID tokens in clickable links
- **Now**: 6-digit codes entered manually
- **Benefit**: More secure, easier for users to type, works better on mobile
- **User Experience**: Similar to two-factor authentication systems
