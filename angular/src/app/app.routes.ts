import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { ClientLayoutComponent } from './components/client-layout/client-layout.component';
import { FreelancerLayoutComponent } from './components/freelancer-layout/freelancer-layout.component';
import { adminGuard, clientGuard, freelancerGuard } from './guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login.component').then((m) => m.LoginComponent),
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
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/front-project-detail/front-project-detail.component').then((m) => m.FrontProjectDetailComponent),
      },
    ],
  },

  // Backoffice (admin)
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
      {
        path: 'interviews',
        loadComponent: () =>
          import('./components/interview-list/interview-list.component').then((m) => m.InterviewListComponent),
        data: { role: 'admin' },
      },
    ],
  },

  // Client dashboard (project owners review applications)
  {
    path: 'client',
    component: ClientLayoutComponent,
    canActivate: [clientGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
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
      {
        path: 'interviews',
        loadComponent: () =>
          import('./components/interview-list/interview-list.component').then((m) => m.InterviewListComponent),
        data: { role: 'client' },
      },
      {
        path: 'interviews/:id',
        loadComponent: () =>
          import('./components/interview-detail/interview-detail.component').then((m) => m.InterviewDetailComponent),
        data: { role: 'client' },
      },
      {
        path: 'interviews/:id/visio',
        loadComponent: () =>
          import('./components/visio-call/visio-call.component').then((m) => m.VisioCallComponent),
      },
    ],
  },

  // Freelancer (availability for interviews)
  {
    path: 'freelancer',
    component: FreelancerLayoutComponent,
    canActivate: [freelancerGuard],
    children: [
      { path: '', redirectTo: 'projects', pathMatch: 'full' },
      {
        path: 'projects',
        loadComponent: () =>
          import('./components/front-project-list/front-project-list.component').then(
            (m) => m.FrontProjectListComponent,
          ),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./components/front-project-detail/front-project-detail.component').then(
            (m) => m.FrontProjectDetailComponent,
          ),
      },
      {
        path: 'availability',
        loadComponent: () =>
          import('./components/freelancer-availability/freelancer-availability.component').then((m) => m.FreelancerAvailabilityComponent),
      },
      {
        path: 'interviews',
        loadComponent: () =>
          import('./components/interview-list/interview-list.component').then((m) => m.InterviewListComponent),
        data: { role: 'freelancer' },
      },
      {
        path: 'interviews/:id',
        loadComponent: () =>
          import('./components/interview-detail/interview-detail.component').then((m) => m.InterviewDetailComponent),
        data: { role: 'freelancer' },
      },
      {
        path: 'interviews/:id/visio',
        loadComponent: () =>
          import('./components/visio-call/visio-call.component').then((m) => m.VisioCallComponent),
      },
      {
        path: 'productivity',
        loadComponent: () =>
          import('./components/productivity-board/productivity-board.component').then((m) => m.ProductivityBoardComponent),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
