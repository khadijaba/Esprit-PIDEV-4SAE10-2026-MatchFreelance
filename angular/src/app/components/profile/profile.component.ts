import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { User, Role } from '../../models/user.model';
import { environment } from '../../environments/environment';
import { AvatarUploadComponent } from '../shared/avatar-upload/avatar-upload.component';

@Component({
    selector: 'app-profile',
    standalone: true,
    imports: [CommonModule, FormsModule, ReactiveFormsModule, AvatarUploadComponent],
    templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
    user: User | null = null;
    profileForm: FormGroup;
    loading = true;
    saving = false;

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private toastService: ToastService
    ) {
        this.profileForm = this.fb.group({
            firstName: ['', [Validators.required]],
            lastName: ['', [Validators.required]],
            email: [{ value: '', disabled: true }],
            address: ['', [Validators.required]],
            birthDate: ['', [Validators.required]],
            role: [{ value: '', disabled: true }]
        });
    }

    ngOnInit() {
        this.fetchUserProfile();
    }

    fetchUserProfile() {
        this.loading = true;
        this.userService.getCurrentUser().subscribe({
            next: (user) => {
                this.user = user;
                this.profileForm.patchValue({
                    firstName: user.firstName,
                    lastName: user.lastName,
                    email: user.email,
                    address: user.address,
                    birthDate: user.birthDate,
                    role: user.role
                });
                this.loading = false;
            },
            error: (err) => {
                this.toastService.error('Failed to load profile');
                this.loading = false;
            }
        });
    }

    get fullAvatarUrl(): string | null {
        if (!this.user || !this.user.profilePictureUrl) return null;
        const serverBaseUrl = environment.apiUrl.replace(/\/api\/?$/, '');
        return serverBaseUrl + this.user.profilePictureUrl;
    }

    onSubmit() {
        if (this.profileForm.invalid) {
            this.toastService.error('Please fill in all required fields');
            return;
        }

        this.saving = true;
        const updatedData = { ...this.user, ...this.profileForm.getRawValue() };

        this.userService.updateProfile(updatedData).subscribe({
            next: (user) => {
                this.user = user;
                this.saving = false;
                this.toastService.success('Profile updated successfully!');
            },
            error: (err) => {
                this.saving = false;
                this.toastService.error('Failed to update profile');
            }
        });
    }

    onAvatarUpdated(newUrl: string) {
        if (this.user) {
            this.user.profilePictureUrl = newUrl;
        }
    }
}
