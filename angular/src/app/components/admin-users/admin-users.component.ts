import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { User, UserFilterRequest, PageResponse, Role } from '../../models/user.model';

@Component({
    selector: 'app-admin-users',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './admin-users.component.html'
})
export class AdminUsersComponent implements OnInit {
    users: User[] = [];
    loading = true;
    deletingId: number | null = null;
    Math = Math;

    // Pagination and Filtering
    totalElements = 0;
    pageSize = 10;
    currentPage = 0;
    totalPages = 0;
    searchTerm = '';
    roleFilter: Role | '' = '';

    roles = [
        { value: Role.FREELANCER, label: 'Freelancer' },
        { value: Role.PROJECT_OWNER, label: 'Project Owner' },
        { value: Role.ADMIN, label: 'Administrator' },
        { value: Role.USER, label: 'Standard User' }
    ];

    constructor(
        private userService: UserService,
        private toastService: ToastService
    ) { }

    ngOnInit() {
        this.loadUsers();
    }

    loadUsers() {
        this.loading = true;

        const filter: UserFilterRequest = {
            page: this.currentPage,
            size: this.pageSize,
            sortBy: 'id',
            sortDir: 'desc'
        };

        if (this.searchTerm) {
            filter.searchTerm = this.searchTerm;
        }

        if (this.roleFilter) {
            filter.role = this.roleFilter as Role;
        }

        this.userService.searchUsers(filter).subscribe({
            next: (res: PageResponse<User>) => {
                this.users = res.content;
                this.totalElements = res.totalElements;
                this.totalPages = Math.ceil(this.totalElements / this.pageSize);
                this.loading = false;
            },
            error: (err: any) => {
                console.error('Failed to load users', err);
                this.toastService.error('Failed to load users');
                this.loading = false;
            }
        });
    }

    onSearch() {
        this.currentPage = 0;
        this.loadUsers();
    }

    onClearFilter() {
        this.searchTerm = '';
        this.roleFilter = '';
        this.currentPage = 0;
        this.loadUsers();
    }

    deleteUser(user: User) {
        if (confirm(`Are you sure you want to delete ${user.firstName} ${user.lastName}?`)) {
            this.deletingId = user.id;

            this.userService.deleteUser(user.id).subscribe({
                next: () => {
                    this.toastService.success('User deleted successfully');
                    this.deletingId = null;

                    // If deleted last item on page, go back one page
                    if (this.users.length === 1 && this.currentPage > 0) {
                        this.currentPage--;
                    }
                    this.loadUsers();
                },
                error: (err: any) => {
                    console.error('Failed to delete user', err);
                    this.toastService.error(err.error || 'Failed to delete user');
                    this.deletingId = null;
                }
            });
        }
    }

    // Pagination navigation
    prevPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.loadUsers();
        }
    }

    nextPage() {
        if (this.currentPage < this.totalPages - 1) {
            this.currentPage++;
            this.loadUsers();
        }
    }

    goToPage(page: number) {
        this.currentPage = page;
        this.loadUsers();
    }

    // Helper for pagination UI
    getPageNumbers(): number[] {
        const pages: number[] = [];
        const maxPagesToShow = 5;

        let startPage = Math.max(0, this.currentPage - Math.floor(maxPagesToShow / 2));
        let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);

        if (endPage - startPage + 1 < maxPagesToShow) {
            startPage = Math.max(0, endPage - maxPagesToShow + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }

        return pages;
    }
}
