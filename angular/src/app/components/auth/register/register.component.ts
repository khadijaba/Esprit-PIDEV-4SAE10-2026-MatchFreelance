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
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './register.component.html',
})
export class RegisterComponent implements OnInit {
    @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
    registerForm!: FormGroup;
    isLoading = false;
    errorMsg = '';

    // Face Recognition
    isModelsLoaded = false;
    isScanningFace = false;
    faceDescriptor: string | null = null;
    faceScanStatus = 'No face scanned';

    // Avatar
    avatarFile: File | null = null;
    avatarPreview: string | null = null;

    passwordStrengthScore = 0;
    passwordStrengthLabel: 'Too weak' | 'Weak' | 'Okay' | 'Strong' = 'Too weak';

    // Available roles for registration
    roles = [
        { value: Role.FREELANCER, label: 'Freelancer' },
        { value: Role.PROJECT_OWNER, label: 'Project Owner' },
        { value: Role.ADMIN, label: 'Administrator' }
    ];

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

        this.registerForm = this.fb.group({
            firstName: ['', [Validators.required, Validators.minLength(2)]],
            lastName: ['', [Validators.required, Validators.minLength(2)]],
            address: ['', [Validators.required]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            confirmPassword: ['', [Validators.required]],
            birthDate: ['', [Validators.required]],
            role: [Role.FREELANCER, [Validators.required]],
            faceDescriptor: ['']
        });

        this.updatePasswordStrength(this.registerForm.get('password')?.value ?? '');
        this.registerForm.get('password')?.valueChanges.subscribe((value) => {
            this.updatePasswordStrength(value ?? '');
        });
    }

    private updatePasswordStrength(password: string) {
        const score = this.calculatePasswordStrengthScore(password);
        this.passwordStrengthScore = score;
        this.passwordStrengthLabel = this.getPasswordStrengthLabel(score);
    }

    private calculatePasswordStrengthScore(password: string): number {
        if (!password) return 0;

        const lengthScore = password.length >= 12 ? 2 : password.length >= 8 ? 1 : 0;
        const hasLower = /[a-z]/.test(password);
        const hasUpper = /[A-Z]/.test(password);
        const hasNumber = /\d/.test(password);
        const hasSymbol = /[^A-Za-z\d]/.test(password);

        const variety = [hasLower, hasUpper, hasNumber, hasSymbol].filter(Boolean).length;
        const varietyScore = variety >= 3 ? 2 : variety >= 2 ? 1 : 0;

        const raw = lengthScore + varietyScore;
        return Math.max(0, Math.min(4, raw));
    }

    private getPasswordStrengthLabel(score: number): 'Too weak' | 'Weak' | 'Okay' | 'Strong' {
        if (score <= 1) return 'Too weak';
        if (score === 2) return 'Weak';
        if (score === 3) return 'Okay';
        return 'Strong';
    }

    get passwordStrengthBars(): number[] {
        return [1, 2, 3, 4];
    }

    get passwordStrengthBarClass(): string {
        if (this.passwordStrengthScore <= 1) return 'bg-red-500';
        if (this.passwordStrengthScore === 2) return 'bg-amber-500';
        if (this.passwordStrengthScore === 3) return 'bg-yellow-500';
        return 'bg-emerald-500';
    }

    async startFaceScan() {
        if (!this.isModelsLoaded) {
            this.toastService.error('Face recognition models are still loading. Please wait.');
            return;
        }
        this.isScanningFace = true;
        this.faceScanStatus = 'Starting camera...';

        // Wait for view generation
        setTimeout(async () => {
            if (this.videoElement && this.videoElement.nativeElement) {
                await this.faceRecService.startCamera(this.videoElement.nativeElement);
                this.faceScanStatus = 'Please face the camera and click Capture Face.';
            }
        }, 100);
    }

    async captureFace() {
        if (!this.videoElement || !this.videoElement.nativeElement) return;
        this.faceScanStatus = 'Scanning face...';

        try {
            const desc = await this.faceRecService.detectFaceDescriptor(this.videoElement.nativeElement);
            if (desc) {
                this.faceDescriptor = desc;
                this.registerForm.get('faceDescriptor')?.setValue(desc);
                this.faceScanStatus = 'Face captured successfully!';
                this.toastService.success('Face scanned and saved.');
                this.stopFaceScan();
            } else {
                this.faceScanStatus = 'No face detected. Try again.';
                this.toastService.error('Could not detect a face. Please ensure good lighting and face the camera directly.');
            }
        } catch (error) {
            console.error('Capture completely failed:', error);
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

    onAvatarSelected(event: Event) {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (file) {
            this.avatarFile = file;
            const reader = new FileReader();
            reader.onload = () => {
                this.avatarPreview = reader.result as string;
            };
            reader.readAsDataURL(file);
        }
    }

    removeAvatar() {
        this.avatarFile = null;
        this.avatarPreview = null;
    }

    onSubmit() {
        if (this.registerForm.invalid) {
            this.registerForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;
        this.errorMsg = '';

        const formValue = this.registerForm.value;

        // Ensure birthDate format is YYYY-MM-DD
        const date = new Date(formValue.birthDate);
        const formattedDate = date.toISOString().split('T')[0];

        const requestPayload = {
            ...formValue,
            birthDate: formattedDate
        };

        const formData = new FormData();
        // Append individual fields as standard form data for maximum backend compatibility
        formData.append('firstName', requestPayload.firstName);
        formData.append('lastName', requestPayload.lastName);
        formData.append('email', requestPayload.email);
        formData.append('password', requestPayload.password);
        formData.append('address', requestPayload.address);
        formData.append('birthDate', requestPayload.birthDate);
        formData.append('role', requestPayload.role);

        if (requestPayload.faceDescriptor) {
            formData.append('faceDescriptor', requestPayload.faceDescriptor);
        }

        if (this.avatarFile) {
            formData.append('file', this.avatarFile);
        }

        this.authService.register(formData).subscribe({
            next: (res) => {
                this.isLoading = false;
                this.toastService.success('Registration successful! Please check your email for verification.');
                this.router.navigate(['/verify-email']);
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMsg = err.error || 'Registration failed. Please try again.';
                this.toastService.error('Registration failed');
            }
        });
    }
}
