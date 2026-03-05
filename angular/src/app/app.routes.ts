import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { ClientLayoutComponent } from './components/client-layout/client-layout.component';
import { authGuard } from './guards/auth.guard';
import { Role } from './models/user.model';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./components/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./components/auth/email-verification/email-verification.component').then(m => m.EmailVerificationComponent),
  },
  {
    path: 'change-password',
    loadComponent: () => import('./components/auth/password-change/password-change.component').then(m => m.PasswordChangeComponent),
  },
  // Frontoffice (public)
  {
    path: '',
    component: FrontLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./components/front-home/front-home.component').then((m) => m.FrontHomeComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./components/front-project-list/front-project-list.component').then((m) => m.FrontProjectListComponent),
      },
      {
        path: 'projects/swipe',
        loadComponent: () =>
          import('./components/project-swipe/project-swipe.component').then((m) => m.ProjectSwipeComponent),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/front-project-detail/front-project-detail.component').then((m) => m.FrontProjectDetailComponent),
      },
      {
        path: 'contracts',
        loadComponent: () =>
          import('./components/freelancer-contract-list/freelancer-contract-list.component').then((m) => m.FreelancerContractListComponent),
      },
      {
        path: 'contracts/:id',
        loadComponent: () =>
          import('./components/freelancer-contract-detail/freelancer-contract-detail.component').then((m) => m.FreelancerContractDetailComponent),
      },
    ],
  },

  // Backoffice (admin)
  {
    path: 'admin',
    component: LayoutComponent,
    canActivate: [authGuard],
    data: { role: Role.ADMIN },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./components/project-list/project-list.component').then((m) => m.ProjectListComponent),
      },
      {
        path: 'projects/new',
        loadComponent: () =>
          import('./components/project-form/project-form.component').then((m) => m.ProjectFormComponent),
      },
      {
        path: 'projects/:id/edit',
        loadComponent: () =>
          import('./components/project-form/project-form.component').then((m) => m.ProjectFormComponent),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/project-details/project-details.component').then((m) => m.ProjectDetailsComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./components/admin-users/admin-users.component').then((m) => m.AdminUsersComponent),
      },
      {
        path: 'user-management',
        loadComponent: () =>
          import('./components/admin/admin-user-management/admin-user-management.component').then((m) => m.AdminUserManagementComponent),
      },
      {
        path: 'candidatures',
        loadComponent: () =>
          import('./components/candidature-list/candidature-list.component').then((m) => m.CandidatureListComponent),
      },
      {
        path: 'contracts',
        loadComponent: () =>
          import('./components/contract-list/contract-list.component').then((m) => m.ContractListComponent),
      },
      {
        path: 'contracts/:id',
        loadComponent: () =>
          import('./components/contract-details/contract-details.component').then((m) => m.ContractDetailsComponent),
      },
    ],
  },

  // Client (project owners review applications)
  {
    path: 'client',
    component: ClientLayoutComponent,
    canActivate: [authGuard],
    data: { role: Role.PROJECT_OWNER },
    children: [
      {
        path: '',
        title: 'Client',
        loadComponent: () =>
          import('./components/client-dashboard/client-dashboard.component').then((m) => m.ClientDashboardComponent),
      },
      {
        path: 'projects/new',
        loadComponent: () =>
          import('./components/client-project-form/client-project-form.component').then((m) => m.ClientProjectFormComponent),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/client-project-detail/client-project-detail.component').then((m) => m.ClientProjectDetailComponent),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
