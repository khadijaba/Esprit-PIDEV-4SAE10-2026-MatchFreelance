import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { ClientLayoutComponent } from './components/client-layout/client-layout.component';

export const routes: Routes = [
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
      {
        path: 'events',
        loadComponent: () =>
          import('./components/front-event-list/front-event-list.component').then((m) => m.FrontEventListComponent),
      },
      {
        path: 'events/calendar',
        loadComponent: () =>
          import('./components/event-calendar/event-calendar.component').then((m) => m.EventCalendarComponent),
      },
      {
        path: 'events/:id',
        loadComponent: () =>
          import('./components/front-event-detail/front-event-detail.component').then((m) => m.FrontEventDetailComponent),
      },
    ],
  },

  // Backoffice (admin)
  {
    path: 'admin',
    component: LayoutComponent,
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
        path: 'events',
        loadComponent: () =>
          import('./components/event-list/event-list.component').then((m) => m.EventListComponent),
      },
      {
        path: 'events/new',
        loadComponent: () =>
          import('./components/event-form/event-form.component').then((m) => m.EventFormComponent),
      },
      {
        path: 'events/:id/edit',
        loadComponent: () =>
          import('./components/event-form/event-form.component').then((m) => m.EventFormComponent),
      },
      {
        path: 'events/:id',
        loadComponent: () =>
          import('./components/event-detail/event-detail.component').then((m) => m.EventDetailComponent),
      },
      {
        path: 'events/:id/participants',
        loadComponent: () =>
          import('./components/participation-list/participation-list.component').then((m) => m.ParticipationListComponent),
      },
      {
        path: 'events-calendar',
        loadComponent: () =>
          import('./components/event-calendar/event-calendar.component').then((m) => m.EventCalendarComponent),
      },
      {
        path: 'workspaces',
        loadComponent: () =>
          import('./components/workspace-list/workspace-list.component').then((m) => m.WorkspaceListComponent),
      },
      {
        path: 'workspaces/new',
        loadComponent: () =>
          import('./components/workspace-form/workspace-form.component').then((m) => m.WorkspaceFormComponent),
      },
      {
        path: 'workspaces/:id/edit',
        loadComponent: () =>
          import('./components/workspace-form/workspace-form.component').then((m) => m.WorkspaceFormComponent),
      },
      {
        path: 'workspaces/:id',
        loadComponent: () =>
          import('./components/workspace-detail/workspace-detail.component').then((m) => m.WorkspaceDetailComponent),
      },
      {
        path: 'sponsors',
        loadComponent: () =>
          import('./components/sponsor-list/sponsor-list.component').then((m) => m.SponsorListComponent),
      },
      {
        path: 'sponsors/new',
        loadComponent: () =>
          import('./components/sponsor-form/sponsor-form.component').then((m) => m.SponsorFormComponent),
      },
      {
        path: 'sponsors/:id/edit',
        loadComponent: () =>
          import('./components/sponsor-form/sponsor-form.component').then((m) => m.SponsorFormComponent),
      },
      {
        path: 'sponsors/:id',
        loadComponent: () =>
          import('./components/sponsor-detail/sponsor-detail.component').then((m) => m.SponsorDetailComponent),
      },
      {
        path: 'feedbacks',
        loadComponent: () =>
          import('./components/feedback-list/feedback-list.component').then((m) => m.FeedbackListComponent),
      },
      {
        path: 'events/:eventId/feedbacks',
        loadComponent: () =>
          import('./components/feedback-event/feedback-event.component').then((m) => m.FeedbackEventComponent),
      },
    ],
  },

  // Client dashboard (project owners review applications)
  {
    path: 'client',
    component: ClientLayoutComponent,
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
    ],
  },

  { path: '**', redirectTo: '' },
];
