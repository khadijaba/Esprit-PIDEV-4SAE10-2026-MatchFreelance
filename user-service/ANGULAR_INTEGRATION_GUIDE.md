## 🔗 ANGULAR FRONTEND INTEGRATION GUIDE

> **How your Angular app should interact with the new SendGrid email service**

---

## **EMAIL FLOW DIAGRAM**

```
┌──────────────────────────────────────────────────────────┐
│  Angular Frontend (User Browser)                         │
└──────────────────────────────────────────────────────────┘
           │                      ▲
           │ 1. POST /signup      │ 6. Success response
           │ (form data)          │
           │                      │
           ▼                      └──────────────────┐
┌──────────────────────────────────────────────────────────┐
│  Spring Boot Backend                                     │
│                                                          │
│  AuthController                                         │
│  ├─ POST /api/auth/signup → UserService.register()     │
│  │                      ↓                               │
│  └─ SendGridEmailService.sendWelcomeEmail()            │
│  └─ SendGridEmailService.sendEmailVerification()       │
│                      ↓                                  │
│  🚀 Email sent via SendGrid API 🚀                     │
└──────────────────────────────────────────────────────────┘
           
           ↓ (async, 2-5 min delay)
           
┌──────────────────────────────────────────────────────────┐
│  User's Email Inbox                                      │
│                                                          │
│  📧 Welcome Email: "Bienvenue sur notre plateforme!"  │
│  📧 Verify Email: "Veuillez vérifier votre email"    │
│     └─ Link: http://localhost:3000/verify-email      │
│        ?token=ABC123DEF456                             │
│        &email=user@example.com                         │
└──────────────────────────────────────────────────────────┘
           
           │ User clicks "Verify" link
           ▼
┌──────────────────────────────────────────────────────────┐
│  Angular Frontend - Email Verification Page             │
│                                                          │
│  // Extract from URL:                                  │
│  // ?token=ABC123DEF456&email=user@example.com        │
│                                                          │
│  // Display: "Verifying your email..."                │
│  // POST /api/auth/verify-email                       │
│  // { token: "ABC123DEF456", email: "..." }           │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│  Backend: UserService.verifyEmail()                      │
│  ├─ Validate token                                       │
│  ├─ Enable account                                       │
│  └─ Return success response                              │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│  Angular Frontend: Account Activated!                    │
│  └─ Redirect to login page                              │
│     Show: "Email verified. You can now login."         │
└──────────────────────────────────────────────────────────┘
```

---

## **API ENDPOINTS FOR ANGULAR**

### **1. SIGNUP ENDPOINT**
```
POST /api/auth/signup
Content-Type: application/json

REQUEST:
{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "jean@example.com",
  "password": "SecurePass123",
  "confirmPassword": "SecurePass123",
  "address": "123 Rue de Paris",
  "birthDate": "1990-01-15",
  "role": "FREELANCER"  // or "PROJECT_OWNER"
}

RESPONSE (200 OK):
"Compte créé avec succès pour : jean@example.com"

RESPONSE (400 ERROR):
"Cet email est déjà utilisé"
OR
"Les mots de passe ne correspondent pas"
OR
"Impossible de créer un compte admin via l'inscription"
```

---

### **2. VERIFY EMAIL URL PATTERN**
```
After signup, user receives email with link:
http://localhost:3000/verify-email?token=ABC123DEF456&email=jean@example.com

Your Angular app should:
1. Extract token and email from URL params
2. Display "Verifying your email..." message
3. Call POST /api/auth/verify-email with token & email
4. On success: "Email verified. Account activated!"
5. On error: Show error message
```

---

### **3. VERIFY EMAIL ENDPOINT**
```
POST /api/auth/verify-email
Content-Type: application/json

REQUEST:
{
  "email": "jean@example.com",
  "verificationToken": "ABC123DEF456"
}

RESPONSE (200 OK):
"Email vérifié avec succès. Votre compte est maintenant activé."

RESPONSE (400 ERROR):
"Token de vérification invalide"
OR
"Token ne correspond pas à cet email"
OR
"Token de vérification expiré"
```

---

### **4. SIGNIN ENDPOINT**
```
POST /api/auth/signin
Content-Type: application/json

REQUEST:
{
  "email": "jean@example.com",
  "password": "SecurePass123"
}

RESPONSE (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "jean@example.com",
  "role": "FREELANCER",
  "message": "Redirection vers le Dashboard Freelancer"
}

RESPONSE (401 ERROR):
"Email ou mot de passe incorrect"
```

---

### **5. PASSWORD RESET - REQUEST**
```
POST /api/auth/reset-password/request
Content-Type: application/json

REQUEST:
{
  "email": "jean@example.com"
}

RESPONSE (200 OK):
"Un email de réinitialisation a été envoyé à jean@example.com"

User receives email with reset link:
http://localhost:3000/reset-password?token=XYZ789ABC456
```

---

### **6. PASSWORD RESET - CONFIRM**
```
POST /api/auth/reset-password/confirm
Content-Type: application/json

REQUEST:
{
  "token": "XYZ789ABC456",
  "newPassword": "NewPassword456",
  "confirmPassword": "NewPassword456"
}

RESPONSE (200 OK):
"Mot de passe réinitialisé avec succès"

RESPONSE (400 ERROR):
"Token de réinitialisation invalide"
OR
"Token de réinitialisation expiré"
OR
"Les mots de passe ne correspondent pas"
```

---

## **ANGULAR SERVICE EXAMPLE**

Create `auth.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';
  private currentUserSubject: BehaviorSubject<any>;
  public currentUser: Observable<any>;

  constructor(private http: HttpClient) {
    this.currentUserSubject = new BehaviorSubject<any>(
      localStorage.getItem('currentUser')
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  // Signup
  signup(firstName: string, lastName: string, email: string, 
         password: string, confirmPassword: string, address: string, 
         birthDate: string, role: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/signup`, {
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
      address,
      birthDate,
      role
    });
  }

  // Verify Email
  verifyEmail(email: string, verificationToken: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-email`, {
      email,
      verificationToken
    });
  }

  // Resend Verification
  resendVerification(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/resend-verification`, { email });
  }

  // Sign In
  signin(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/signin`, { email, password })
      .pipe(
        map(response => {
          if (response && response.token) {
            localStorage.setItem('currentUser', JSON.stringify(response));
            this.currentUserSubject.next(response);
          }
          return response;
        })
      );
  }

  // Password Reset - Request
  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password/request`, { email });
  }

  // Password Reset - Confirm
  confirmPasswordReset(token: string, newPassword: string, 
                      confirmPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password/confirm`, {
      token,
      newPassword,
      confirmPassword
    });
  }

  // Logout
  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}
```

---

## **ANGULAR COMPONENT EXAMPLES**

### **Signup Component**

```typescript
@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnInit {
  signupForm: FormGroup;
  loading = false;
  submitted = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.signupForm = this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
      address: ['', Validators.required],
      birthDate: ['', Validators.required],
      role: ['FREELANCER', Validators.required]
    });
  }

  get f() { return this.signupForm.controls; }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.signupForm.invalid) {
      return;
    }

    this.loading = true;
    this.authService.signup(
      this.f.firstName.value,
      this.f.lastName.value,
      this.f.email.value,
      this.f.password.value,
      this.f.confirmPassword.value,
      this.f.address.value,
      this.f.birthDate.value,
      this.f.role.value
    ).subscribe(
      response => {
        this.loading = false;
        this.successMessage = response;
        
        // Show message and redirect to login
        setTimeout(() => {
          this.router.navigate(['/verify-email']);
        }, 3000);
      },
      error => {
        this.loading = false;
        this.errorMessage = error.error || 'Signup failed';
      }
    );
  }
}
```

---

### **Email Verification Component**

```typescript
@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.css']
})
export class VerifyEmailComponent implements OnInit {
  verificationForm: FormGroup;
  loading = false;
  verifying = false;
  successMessage = '';
  errorMessage = '';
  token: string = '';
  email: string = '';

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // Get token and email from URL params
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      this.email = params['email'] || '';
      
      if (this.token && this.email) {
        // Auto-verify if params present
        this.autoVerifyEmail();
      }
    });

    this.verificationForm = FormGroup({
      email: ['', [Validators.required, Validators.email]],
      token: ['', Validators.required]
    });
  }

  autoVerifyEmail() {
    this.verifying = true;
    this.authService.verifyEmail(this.email, this.token)
      .subscribe(
        response => {
          this.verifying = false;
          this.successMessage = response;
          
          // Redirect to login after verification
          setTimeout(() => {
            this.router.navigate(['/signin']);
          }, 2000);
        },
        error => {
          this.verifying = false;
          this.errorMessage = error.error || 'Verification failed';
        }
      );
  }

  onSubmit() {
    if (this.verificationForm.invalid) {
      return;
    }

    this.autoVerifyEmail();
  }

  // For resending verification email
  resendVerification() {
    this.loading = true;
    this.authService.resendVerification(this.email)
      .subscribe(
        response => {
          this.loading = false;
          this.successMessage = response;
        },
        error => {
          this.loading = false;
          this.errorMessage = error.error;
        }
      );
  }
}
```

---

### **Signin Component**

```typescript
@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html',
  styleUrls: ['./signin.component.css']
})
export class SigninComponent implements OnInit {
  signinForm: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.signinForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  get f() { return this.signinForm.controls; }

  onSubmit() {
    this.submitted = true;
    this.errorMessage = '';

    if (this.signinForm.invalid) {
      return;
    }

    this.loading = true;
    this.authService.signin(this.f.email.value, this.f.password.value)
      .subscribe(
        response => {
          this.loading = false;
          
          // Redirect based on role
          const role = response.role;
          if (role === 'ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else if (role === 'PROJECT_OWNER') {
            this.router.navigate(['/owner/dashboard']);
          } else {
            this.router.navigate(['/freelancer/dashboard']);
          }
        },
        error => {
          this.loading = false;
          this.errorMessage = error.error || 'Signin failed';
        }
      );
  }
}
```

---

### **Password Reset Component**

```typescript
@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  resetForm: FormGroup;
  loading = false;
  step = 1; // 1: Request, 2: Confirm
  successMessage = '';
  errorMessage = '';
  token: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    // Check if token in URL (step 2)
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      if (this.token) {
        this.step = 2;
      }
    });

    // Step 1: Request reset
    this.resetForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  // Step 1: Request password reset
  requestReset() {
    if (!this.resetForm.valid) return;

    this.loading = true;
    this.authService.requestPasswordReset(this.resetForm.value.email)
      .subscribe(
        response => {
          this.loading = false;
          this.successMessage = response;
          
          // After 3 seconds redirect to signin
          setTimeout(() => {
            this.router.navigate(['/signin']);
          }, 3000);
        },
        error => {
          this.loading = false;
          this.errorMessage = error.error;
        }
      );
  }

  // Step 2: Confirm reset with new password
  confirmReset() {
    this.resetForm = this.formBuilder.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    if (!this.resetForm.valid) return;

    this.loading = true;
    this.authService.confirmPasswordReset(
      this.token,
      this.resetForm.value.newPassword,
      this.resetForm.value.confirmPassword
    ).subscribe(
      response => {
        this.loading = false;
        this.successMessage = response;
        
        setTimeout(() => {
          this.router.navigate(['/signin']);
        }, 2000);
      },
      error => {
        this.loading = false;
        this.errorMessage = error.error;
      }
    );
  }
}
```

---

## **HTTP INTERCEPTOR FOR JWT TOKEN**

```typescript
import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor() { }

  intercept(request: HttpRequest<any>, next: HttpHandler): 
      Observable<HttpEvent<any>> {
    
    // Add JWT token to headers if available
    const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
    
    if (currentUser && currentUser.token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${currentUser.token}`
        }
      });
    }

    return next.handle(request);
  }
}

// Add to app.module.ts:
// providers: [
//   { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
// ]
```

---

## **USER FLOW SCENARIOS**

### **Scenario 1: New User Registration**
```
1. User fills signup form
2. Submits to POST /api/auth/signup
3. Backend creates user (enabled=false)
4. Backend sends 2 emails:
   a) Welcome email
   b) Verification email with link + token
5. Frontend shows: "Welcome! Check your email"
6. User clicks link in email
7. Link redirects to: /verify-email?token=XXX&email=user@example.com
8. Angular extracts params and auto-verifies
9. Backend enables account
10. Frontend redirects to signin page
```

### **Scenario 2: Forgot Password**
```
1. User clicks "Forgot Password" link
2. User enters email
3. Frontend: POST /api/auth/reset-password/request
4. Backend sends reset email with link + token
5. User clicks link in email
6. Link: /reset-password?token=XXX
7. Angular shows password reset form
8. User enters new password
9. Frontend: POST /api/auth/reset-password/confirm
10. Backend updates password
11. Frontend redirects to signin with success message
```

### **Scenario 3: Email Verification Resend**
```
1. User lost verification email
2. User enters email on verification page
3. Clicks "Resend Verification Email"
4. Frontend: POST /api/auth/resend-verification
5. Backend sends new verification email
6. User checks email again
7. Clicks new verification link
8. Account activated
```

---

## **ERROR HANDLING BEST PRACTICES**

```typescript
// Handle common errors
const handleAuthError = (error: any): string => {
  if (!error || !error.error) {
    return 'Une erreur est survenue. Vérifiez votre connexion.';
  }

  const errorMsg = error.error;

  // Signup errors
  if (errorMsg.includes('déjà utilisé')) {
    return 'Cet email existe déjà. Veuillez vous connecter.';
  }
  if (errorMsg.includes('ne correspondent pas')) {
    return 'Les mots de passe ne correspondent pas.';
  }
  
  // SignIn errors
  if (errorMsg.includes('Email ou mot de passe')) {
    return 'Email ou mot de passe incorrect.';
  }
  
  // Verification errors
  if (errorMsg.includes('Token')) {
    return 'Le lien a expiré. Demander un nouvel email.';
  }

  return errorMsg;
};
```

---

## **TESTING WITH ANGULAR CLI**

```bash
# Create new component
ng generate component components/signup
ng generate component components/verify-email
ng generate component components/signin
ng generate component components/reset-password

# Create service
ng generate service services/auth

# Run application
ng serve

# Navigate to
http://localhost:4200/signup
```

---

## **IMPORTANT NOTES**

1. **Email Links:** Update localhost URLs to production in SendGridEmailService for production
2. **Token Handling:** Tokens expire (15 min for reset, 24h for verify)
3. **CORS:** Backend must allow Angular frontend origin
4. **Storage:** Don't store sensitive data in localStorage (use secure HttpOnly cookies ideally)
5. **HTTPS:** Use HTTPS in production
6. **Testing:** Wait 2-5 minutes for emails when testing

---

**Status:** ✅ Ready for integration with Angular frontend

