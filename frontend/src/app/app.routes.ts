import { Routes } from '@angular/router';
import { CompanySwitchPage, LoginPage } from './auth/access.pages';
import { BuyerHomePage, BuyerOperationPage, BuyerShell } from './buyer/buyer.pages';
import {
  BuildingsPage, CompanyProfilePage, DashboardPage, LeadDetailPage, LeadsPage, ProjectEditorPage, ProjectsPage, ReservationsPage, UnitsPage,
} from './company/company.pages';
import {
  AuditPage, ContractDetailPage, ContractsPage, CustomersPage, NoticesPage, PaymentsPage, TemplatesPage, TeamPage,
} from './company/finance.pages';
import { authGuard, companyGuard, platformGuard, roleGuard } from './core/guards';
import { PlatformShell, PrivateShell } from './layout/private-shell';
import { PlatformCompaniesPage, PlatformHomePage, PlatformSettingsPage } from './platform/platform.pages';
import { CompanyPublicPage, HomePage, ProjectDetailPage } from './public/marketplace.pages';

const staff = roleGuard(['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE']);
const sales = roleGuard(['COMPANY_ADMIN', 'SALES_ADVISOR']);
const finance = roleGuard(['COMPANY_ADMIN', 'FINANCE']);
const admin = roleGuard(['COMPANY_ADMIN']);
const buyer = roleGuard(['BUYER']);

export const routes: Routes = [
  { path: '', component: HomePage },
  { path: 'proyectos/:slug', component: ProjectDetailPage },
  { path: 'constructoras/:slug', component: CompanyPublicPage },
  { path: 'ingresar', component: LoginPage },
  { path: 'seleccionar-empresa', component: CompanySwitchPage, canActivate: [authGuard] },
  {
    path: 'plataforma',
    component: PlatformShell,
    canActivate: [authGuard, platformGuard],
    children: [
      { path: '', component: PlatformHomePage },
      { path: 'constructoras', component: PlatformCompaniesPage },
      { path: 'configuracion', component: PlatformSettingsPage },
    ],
  },
  {
    path: 'app',
    component: PrivateShell,
    canActivate: [companyGuard, staff],
    children: [
      { path: '', component: DashboardPage },
      { path: 'empresa', component: CompanyProfilePage, canActivate: [admin] },
      { path: 'proyectos', component: ProjectsPage },
      { path: 'proyectos/:id', component: ProjectEditorPage },
      { path: 'edificios', component: BuildingsPage },
      { path: 'unidades', component: UnitsPage },
      { path: 'prospectos', component: LeadsPage, canActivate: [sales] },
      { path: 'prospectos/:id', component: LeadDetailPage, canActivate: [sales] },
      { path: 'reservas', component: ReservationsPage, canActivate: [sales] },
      { path: 'compradores', component: CustomersPage },
      { path: 'contratos', component: ContractsPage },
      { path: 'contratos/:id', component: ContractDetailPage },
      { path: 'pagos', component: PaymentsPage, canActivate: [finance] },
      { path: 'equipo', component: TeamPage, canActivate: [admin] },
      { path: 'plantillas', component: TemplatesPage, canActivate: [admin] },
      { path: 'auditoria', component: AuditPage, canActivate: [admin] },
      { path: 'notificaciones', component: NoticesPage },
    ],
  },
  {
    path: 'portal',
    component: BuyerShell,
    canActivate: [companyGuard, buyer],
    children: [
      { path: '', component: BuyerHomePage },
      { path: 'operaciones/:id', component: BuyerOperationPage },
    ],
  },
  { path: '**', redirectTo: '' },
];
