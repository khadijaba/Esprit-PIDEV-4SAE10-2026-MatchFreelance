import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { adminGuard, freelancerGuard, projectOwnerGuard } from './guards/role.guard';

export const routes: Routes = [
  // Frontoffice (public) – login/register inside layout for header+footer with logo
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
        path: 'login',
        loadComponent: () =>
          import('./components/auth/login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./components/auth/register/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./components/front-project-list/front-project-list.component').then((m) => m.FrontProjectListComponent),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/front-project-detail/front-project-detail.component').then((m) => m.FrontProjectDetailComponent),
      },
      {
        path: 'formations',
        loadComponent: () =>
          import('./components/front-formation-list/front-formation-list.component').then((m) => m.FrontFormationListComponent),
      },
      {
        path: 'formations/:id',
        loadComponent: () =>
          import('./components/front-formation-detail/front-formation-detail.component').then((m) => m.FrontFormationDetailComponent),
      },
      {
        path: 'discussion/:userId',
        loadComponent: () =>
          import('./components/discussion/discussion.component').then((m) => m.DiscussionComponent),
      },
      {
        path: 'discussions',
        loadComponent: () =>
          import('./components/discussions-list/discussions-list.component').then((m) => m.DiscussionsListComponent),
      },
      {
        path: 'project-owner/projects',
        loadComponent: () =>
          import('./components/project-list/project-list.component').then((m) => m.ProjectListComponent),
        canActivate: [projectOwnerGuard],
      },
      {
        path: 'project-owner/projects/new',
        loadComponent: () =>
          import('./components/project-form/project-form.component').then((m) => m.ProjectFormComponent),
        canActivate: [projectOwnerGuard],
      },
      {
        path: 'project-owner/projects/:id/edit',
        loadComponent: () =>
          import('./components/project-form/project-form.component').then((m) => m.ProjectFormComponent),
        canActivate: [projectOwnerGuard],
      },
    ],
  },

  // Backoffice (admin) – accès réservé au rôle ADMIN uniquement
  {
    path: 'admin',
    component: LayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'intelligence',
        loadComponent: () =>
          import('./components/intelligence-dashboard/intelligence-dashboard.component').then((m) => m.IntelligenceDashboardComponent),
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
        path: 'formations',
        loadComponent: () =>
          import('./components/formation-list/formation-list.component').then((m) => m.FormationListComponent),
      },
      {
        path: 'formations/new',
        loadComponent: () =>
          import('./components/formation-form/formation-form.component').then((m) => m.FormationFormComponent),
      },
      {
        path: 'formations/:id',
        loadComponent: () =>
          import('./components/formation-details/formation-details.component').then((m) => m.FormationDetailsComponent),
      },
      {
        path: 'formations/:id/edit',
        loadComponent: () =>
          import('./components/formation-form/formation-form.component').then((m) => m.FormationFormComponent),
      },
      {
        path: 'skills',
        loadComponent: () =>
          import('./components/skills-list/skills-list.component').then((m) => m.SkillsListComponent),
      },
      {
        path: 'cvs',
        loadComponent: () =>
          import('./components/cv-supervision/cv-supervision.component').then((m) => m.CvSupervisionComponent),
      },
      {
        path: 'portfolios',
        loadComponent: () =>
          import('./components/portfolio-supervision/portfolio-supervision.component').then((m) => m.PortfolioSupervisionComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./components/admin-users-list/admin-users-list.component').then((m) => m.AdminUsersListComponent),
      },
      {
        path: 'users/:id',
        loadComponent: () =>
          import('./components/admin-user-profile/admin-user-profile.component').then((m) => m.AdminUserProfileComponent),
      },
    ],
  },

  // Freelancer area (protected, role FREELANCER)
  {
    path: 'freelancer',
    component: FrontLayoutComponent,
    canActivate: [freelancerGuard],
    children: [
      {
        path: 'skills',
        loadComponent: () =>
          import('./components/freelancer-skills/freelancer-skills.component').then((m) => m.FreelancerSkillsComponent),
      },
      {
        path: 'career-path',
        loadComponent: () =>
          import('./components/career-path/career-path.component').then((m) => m.CareerPathComponent),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./components/freelancer-notifications/freelancer-notifications.component').then((m) => m.FreelancerNotificationsComponent),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
