import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { ToastService } from '../../../services/toast.service';
import { User } from '../../../models/user.model';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-user-management.component.html',
})
export class AdminUserManagementComponent implements OnInit {
  users: (User & { enabled?: boolean })[] = [];
  loading = true;
  updatingUserId: number | null = null;

  constructor(
    private userService: UserService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        // Filter out admin users and ensure enabled property is set
        this.users = users
          .filter(user => user.role !== 'ADMIN')
          .map(user => ({
            ...user,
            enabled: user.enabled ?? true // Default to true if undefined
          }));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load users');
      }
    });
  }

  toggleUserStatus(userId: number, enabled: boolean) {
    this.updatingUserId = userId;
    
    this.userService.updateUserStatus(userId, enabled).subscribe({
      next: () => {
        // Update the user in the local array
        const userIndex = this.users.findIndex(u => u.id === userId);
        if (userIndex !== -1) {
          this.users[userIndex].enabled = enabled;
        }
        
        this.updatingUserId = null;
        this.toastService.success(`User ${enabled ? 'enabled' : 'disabled'} successfully`);
      },
      error: () => {
        this.updatingUserId = null;
        this.toastService.error('Failed to update user status');
      }
    });
  }

  getStatusClass(enabled?: boolean): string {
    return enabled ? 'text-green-600 bg-green-50' : 'text-red-600 bg-red-50';
  }

  getEnabledUsersCount(): number {
    return this.users.filter(u => u.enabled === true).length;
  }

  getDisabledUsersCount(): number {
    return this.users.filter(u => u.enabled === false).length;
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'PROJECT_OWNER':
        return 'bg-teal-100 text-teal-800';
      case 'FREELANCER':
        return 'bg-orange-100 text-orange-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatRole(role: string): string {
    return role.replace('_', ' ');
  }
}
