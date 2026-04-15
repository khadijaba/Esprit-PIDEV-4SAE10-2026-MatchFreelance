import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { adminGuard, authGuard } from './guards/auth.guard';

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
        path: 'formations/:formationId/examen/:examenId',
        loadComponent: () =>
          import('./components/passer-examen/passer-examen.component').then((m) => m.PasserExamenComponent),
      },
      {
        path: 'certificat/:id',
        loadComponent: () =>
          import('./components/certificat-view/certificat-view.component').then((m) => m.CertificatViewComponent),
      },
      {
        path: 'verify-certificat/:numero',
        loadComponent: () =>
          import('./components/verify-certificat/verify-certificat.component').then((m) => m.VerifyCertificatComponent),
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./components/login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./components/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./components/reset-password/reset-password.component').then((m) => m.ResetPasswordComponent),
      },
      {
        path: 'verify-email',
        loadComponent: () =>
          import('./components/verify-email/verify-email.component').then((m) => m.VerifyEmailComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./components/register/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./components/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'parcours-intelligent',
        loadComponent: () =>
          import('./components/parcours-intelligent/parcours-intelligent.component').then((m) => m.ParcoursIntelligentComponent),
      },
      {
        path: 'mes-competences',
        loadComponent: () =>
          import('./components/mes-competences/mes-competences.component').then((m) => m.MesCompetencesComponent),
      },
      {
        path: 'dashboard-freelancer',
        loadComponent: () =>
          import('./components/dashboard-freelancer/dashboard-freelancer.component').then((m) => m.DashboardFreelancerComponent),
      },
      {
        path: 'dashboard-client',
        loadComponent: () =>
          import('./components/dashboard-client/dashboard-client.component').then((m) => m.DashboardClientComponent),
      },
      {
        path: 'mon-activite',
        loadComponent: () =>
          import('./components/mon-activite/mon-activite.component').then((m) => m.MonActiviteComponent),
      },
      {
        path: 'classement',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/ranking-recommendation/ranking-recommendation.component').then(
            (m) => m.RankingRecommendationComponent
          ),
      },
      {
        path: 'microservice-user',
        loadComponent: () =>
          import('./components/pidev-user-public/pidev-user-public.component').then((m) => m.PidevUserPublicComponent),
      },
    ],
  },

  // Backoffice (admin) — réservé aux utilisateurs connectés avec rôle ADMIN
  {
    path: 'admin',
    component: LayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'profile',
        loadComponent: () =>
          import('./components/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'pidev-user',
        loadComponent: () =>
          import('./components/admin-pidev-user/admin-pidev-user.component').then((m) => m.AdminPidevUserComponent),
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
        path: 'formations/:id/edit',
        loadComponent: () =>
          import('./components/formation-form/formation-form.component').then((m) => m.FormationFormComponent),
      },
      {
        path: 'formations/:id',
        loadComponent: () =>
          import('./components/formation-details/formation-details.component').then((m) => m.FormationDetailsComponent),
      },
      {
        path: 'examens',
        loadComponent: () =>
          import('./components/examen-list/examen-list.component').then((m) => m.ExamenListComponent),
      },
      {
        path: 'examens/new',
        loadComponent: () =>
          import('./components/examen-form/examen-form.component').then((m) => m.ExamenFormComponent),
      },
      {
        path: 'examens/:id',
        loadComponent: () =>
          import('./components/examen-details/examen-details.component').then((m) => m.ExamenDetailsComponent),
      },
      {
        path: 'skills',
        loadComponent: () =>
          import('./components/skill-list/skill-list.component').then((m) => m.SkillListComponent),
      },
      {
        path: 'skills/new',
        loadComponent: () =>
          import('./components/skill-form/skill-form.component').then((m) => m.SkillFormComponent),
      },
      {
        path: 'skills/:id/edit',
        loadComponent: () =>
          import('./components/skill-form/skill-form.component').then((m) => m.SkillFormComponent),
      },
      {
        path: 'rapports',
        loadComponent: () =>
          import('./components/rapports/rapports.component').then((m) => m.RapportsComponent),
      },
      {
        path: 'recommandations',
        loadComponent: () =>
          import('./components/recommandations/recommandations.component').then((m) => m.RecommandationsComponent),
      },
      {
        path: 'rappels',
        loadComponent: () =>
          import('./components/rappels/rappels.component').then((m) => m.RappelsComponent),
      },
      {
        path: 'previsionnel',
        loadComponent: () =>
          import('./components/previsionnel/previsionnel.component').then((m) => m.PrevisionnelComponent),
      },
      {
        path: 'ranking',
        loadComponent: () =>
          import('./components/ranking-recommendation/ranking-recommendation.component').then(
            (m) => m.RankingRecommendationComponent
          ),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
