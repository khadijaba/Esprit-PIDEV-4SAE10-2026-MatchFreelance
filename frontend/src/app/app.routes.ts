import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { FrontLayoutComponent } from './components/front-layout/front-layout.component';
import { adminGuard, authGuard, clientGuard } from './guards/auth.guard';

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
        path: 'freelancer/productivity',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/productivity-board/productivity-board.component').then((m) => m.ProductivityBoardComponent),
      },
      {
        path: 'dashboard-client',
        canActivate: [clientGuard],
        loadComponent: () =>
          import('./components/dashboard-client/dashboard-client.component').then((m) => m.DashboardClientComponent),
      },
      {
        path: 'client',
        canActivate: [clientGuard],
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
        path: 'communaute',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/blog-forum-embed/blog-forum-embed.component').then(
            (m) => m.BlogForumEmbedComponent
          ),
      },
      {
        path: 'microservice-user',
        loadComponent: () =>
          import('./components/pidev-user-public/pidev-user-public.component').then((m) => m.PidevUserPublicComponent),
      },
      {
        path: 'projets',
        loadComponent: () =>
          import('./components/matchfreelance-projects/matchfreelance-projects.component').then(
            (m) => m.MatchfreelanceProjectsComponent
          ),
      },
      {
        path: 'projets/:id/postuler',
        loadComponent: () =>
          import('./components/project-apply-detail/project-apply-detail.component').then(
            (m) => m.ProjectApplyDetailComponent
          ),
      },
      {
        path: 'projets/nouveau',
        canActivate: [clientGuard],
        loadComponent: () =>
          import('./components/project-create-step1/project-create-step1.component').then((m) => m.ProjectCreateStep1Component),
      },
      {
        path: 'projets/nouveau/verification/:id',
        canActivate: [clientGuard],
        loadComponent: () =>
          import('./components/project-verify-publish/project-verify-publish.component').then(
            (m) => m.ProjectVerifyPublishComponent
          ),
      },
      {
        path: 'projets/:id/recrutement',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/project-recrutement/project-recrutement.component').then(
            (m) => m.ProjectRecrutementComponent
          ),
      },
      {
        path: 'projets/:id/supervision',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/project-supervision/project-supervision.component').then(
            (m) => m.ProjectSupervisionComponent
          ),
      },
      {
        path: 'projets/:id',
        loadComponent: () =>
          import('./components/project-owner-detail/project-owner-detail.component').then(
            (m) => m.ProjectOwnerDetailComponent
          ),
      },
      {
        path: 'mes-contrats/:contractId',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/freelancer-contract-detail/freelancer-contract-detail.component').then(
            (m) => m.FreelancerContractDetailComponent
          ),
      },
      {
        path: 'mes-contrats',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/freelancer-contracts/freelancer-contracts.component').then(
            (m) => m.FreelancerContractsComponent
          ),
      },
      {
        path: 'mes-candidatures',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./components/freelancer-applications/freelancer-applications.component').then(
            (m) => m.FreelancerApplicationsComponent
          ),
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
