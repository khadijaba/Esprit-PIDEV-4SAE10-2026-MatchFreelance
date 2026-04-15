import { Routes } from '@angular/router';
import { LayoutComponent } from './components/backend/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { ClientLayoutComponent } from './components/client-layout/client-layout.component';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./components/login/login.component').then((m) => m.LoginComponent) },

  // Frontoffice (public + freelancer)
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
        path: 'pitch-analyzer',
        loadComponent: () =>
          import('./components/pitch-analyzer/pitch-analyzer.component').then((m) => m.PitchAnalyzerComponent),
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
        canActivate: [roleGuard(['FREELANCER'])],
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
        canActivate: [roleGuard(['FREELANCER'])],
      },
      {
        path: 'contracts/:id',
        loadComponent: () =>
          import('./components/freelancer-contract-detail/freelancer-contract-detail.component').then((m) => m.FreelancerContractDetailComponent),
        canActivate: [roleGuard(['FREELANCER'])],
      },
    ],
  },

  // Backoffice (admin)
  {
    path: 'admin',
    component: LayoutComponent,
    canActivate: [roleGuard(['ADMIN'])],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/backend/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./components/backend/project-list/project-list.component').then((m) => m.ProjectListComponent),
      },
      {
        path: 'projects/new',
        loadComponent: () =>
          import('./components/backend/project-form/project-form.component').then((m) => m.ProjectFormComponent),
      },
      {
        path: 'projects/:id/edit',
        loadComponent: () =>
          import('./components/backend/project-form/project-form.component').then((m) => m.ProjectFormComponent),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/backend/project-details/project-details.component').then((m) => m.ProjectDetailsComponent),
      },
      {
        path: 'candidatures',
        loadComponent: () =>
          import('./components/backend/candidature-list/candidature-list.component').then((m) => m.CandidatureListComponent),
      },
      {
        path: 'contracts',
        loadComponent: () =>
          import('./components/backend/contract-list/contract-list.component').then((m) => m.ContractListComponent),
      },
      {
        path: 'contracts/:id',
        loadComponent: () =>
          import('./components/backend/contract-details/contract-details.component').then((m) => m.ContractDetailsComponent),
      },
    ],
  },

  // Client (project owners review applications)
  {
    path: 'client',
    component: ClientLayoutComponent,
    canActivate: [roleGuard(['CLIENT'])],
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
