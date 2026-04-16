import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { Role } from '../../../models/user.model';
import { FaceRecognitionService } from '../../../services/face-recognition.service';
import { ViewChild, ElementRef } from '@angular/core';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
    @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
    loginForm!: FormGroup;
    isLoading = false;
    errorMsg = '';

    // Face Recognition
    isModelsLoaded = false;
    isScanningFace = false;
    faceScanStatus = '';

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private toastService: ToastService,
        private faceRecService: FaceRecognitionService
    ) { }

    ngOnInit() {
        this.faceRecService.loadModels()
            .then(() => this.isModelsLoaded = true)
            .catch(err => {
                console.error("Model loading error ->", err);
                this.toastService.error('Failed to load Face ID AI models. Please check your browser console.');
            });

        this.loginForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', []], // Removing required so we can login with face
        });
    }

    async startFaceScan() {
        if (!this.loginForm.get('email')?.valid) {
            this.toastService.error('Please enter a valid email address first.');
            this.loginForm.get('email')?.markAsTouched();
            return;
        }

        if (!this.isModelsLoaded) {
            this.toastService.error('Face recognition models are still loading. Please wait.');
            return;
        }

        this.isScanningFace = true;
        this.faceScanStatus = 'Starting camera...';

        setTimeout(async () => {
            if (this.videoElement && this.videoElement.nativeElement) {
                await this.faceRecService.startCamera(this.videoElement.nativeElement);
                this.faceScanStatus = 'Please face the camera and click Verify Face.';
            }
        }, 100);
    }

    async verifyFace() {
        if (!this.videoElement || !this.videoElement.nativeElement) return;
        this.faceScanStatus = 'Verifying face...';

        try {
            const desc = await this.faceRecService.detectFaceDescriptor(this.videoElement.nativeElement);
            if (desc) {
                this.faceScanStatus = 'Face captured. Logging in...';
                this.stopFaceScan();
                this.loginWithFaceDescriptor(desc);
            } else {
                this.faceScanStatus = 'No face detected. Try again.';
                this.toastService.error('Could not detect a face. Please ensure good lighting and face the camera directly.');
            }
        } catch (error) {
            console.error('Verify completely failed:', error);
            this.faceScanStatus = 'Scan error. Check console.';
            this.toastService.error('Face scanning crashed. Please check browser console.');
        }
    }

    stopFaceScan() {
        if (this.videoElement && this.videoElement.nativeElement) {
            this.faceRecService.stopCamera(this.videoElement.nativeElement);
        }
        this.isScanningFace = false;
    }

    loginWithFaceDescriptor(descriptor: string) {
        this.isLoading = true;
        const request = {
            email: this.loginForm.get('email')?.value,
            faceDescriptor: descriptor
        };

        this.authService.loginWithFace(request).subscribe({
            next: (res) => this.handleSuccessfulLogin(),
            error: (err) => {
                this.isLoading = false;
                this.errorMsg = err.error || 'Face verification failed.';
                this.toastService.error('Login failed');
            }
        });
    }

    handleSuccessfulLogin() {
        this.isLoading = false;
        this.toastService.success('Login successful');

        const role = this.authService.getUserRole();
        if (role === Role.ADMIN) {
            this.router.navigate(['/admin/dashboard']);
        } else if (role === Role.PROJECT_OWNER) {
            this.router.navigate(['/client']);
        } else {
            this.router.navigate(['/projects']);
        }
    }

    onSubmit() {
        if (this.loginForm.get('password')?.invalid && !this.isScanningFace) {
            this.toastService.error('Password is required for normal sign in.');
            return;
        }

        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;
        this.errorMsg = '';

        this.authService.login(this.loginForm.value).subscribe({
            next: (res) => this.handleSuccessfulLogin(),
            error: (err) => {
                this.isLoading = false;
                this.errorMsg = err.error || 'Invalid credentials. Please try again.';
                this.toastService.error('Login failed');
            }
        });
    }
}
