import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-admin-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-users-list.component.html',
})
export class AdminUsersListComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  loading = true;
  roleFilter: string | '' = '';
  searchTerm = '';

  constructor(
    private userService: UserService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data ?? [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger les profils utilisateurs');
      },
    });
  }

  applyFilters() {
    let result = this.users;
    if (this.roleFilter) {
      result = result.filter((u) => (u.role ?? '').toUpperCase() === this.roleFilter.toUpperCase());
    }
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(
        (u) =>
          (u.email ?? '').toLowerCase().includes(q) ||
          (u.fullName ?? u.username ?? '').toLowerCase().includes(q)
      );
    }
    this.filteredUsers = result;
  }

  onFilterChange() {
    this.applyFilters();
  }

  roleLabel(role: string | undefined): string {
    if (!role) return '—';
    const map: Record<string, string> = {
      FREELANCER: 'Freelancer',
      CLIENT: 'Project Owner',
      PROJECT_OWNER: 'Project Owner',
      ADMIN: 'Admin',
    };
    return map[role.toUpperCase()] ?? role;
  }

  roleBadgeClass(role: string | undefined): string {
    if (!role) return 'bg-gray-100 text-gray-700';
    const r = role.toUpperCase();
    if (r === 'FREELANCER') return 'bg-blue-100 text-blue-700';
    if (r === 'CLIENT' || r === 'PROJECT_OWNER') return 'bg-emerald-100 text-emerald-700';
    if (r === 'ADMIN') return 'bg-amber-100 text-amber-700';
    return 'bg-gray-100 text-gray-700';
  }

  displayName(u: User): string {
    return (u.fullName ?? u.username ?? u.email ?? `#${u.id}`).toString().trim() || `Profil #${u.id}`;
  }
}
