import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  AuditEvent,
  Brand,
  BuyerHome,
  BuyerOperation,
  CompanyPage,
  CompanyProfile,
  Contract,
  ContractParty,
  Customer,
  Dashboard,
  DocumentItem,
  Lead,
  Member,
  Notice,
  Page,
  Payment,
  PlanView,
  PlatformMetrics,
  ProjectCard,
  ProjectDetail,
  ProjectPage,
  Province,
  Reservation,
  Session,
  Stage,
  TemplateView,
  Unit,
  User,
  Activity,
  Building,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  login(email: string, password: string): Observable<Session> {
    return this.http.post<Session>('/api/v1/auth/login', { email, password });
  }

  refresh(): Observable<Session> {
    return this.http.post<Session>('/api/v1/auth/refresh', {});
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', {});
  }

  me(): Observable<User> {
    return this.http.get<User>('/api/v1/auth/me');
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put('/api/v1/auth/password', { currentPassword, newPassword }, { responseType: 'text' }).pipe(map(() => undefined));
  }

  locations(): Observable<Province[]> {
    return this.http.get<Province[]>('/api/v1/public/locations');
  }

  publicProjects(filters: Record<string, string | number | boolean | null | undefined>): Observable<Page<ProjectCard>> {
    return this.http.get<Page<ProjectCard>>('/api/v1/public/projects', { params: this.params(filters) });
  }

  featured(): Observable<ProjectCard[]> {
    return this.http.get<ProjectCard[]>('/api/v1/public/projects/featured');
  }

  publicProject(slug: string): Observable<ProjectPage> {
    return this.http.get<ProjectPage>(`/api/v1/public/projects/${slug}`);
  }

  publicCompany(slug: string): Observable<CompanyPage> {
    return this.http.get<CompanyPage>(`/api/v1/public/companies/${slug}`);
  }

  inquire(slug: string, body: { name: string; email: string; phone: string; unitId: string | null; message: string }): Observable<void> {
    return this.http.post<void>(`/api/v1/public/projects/${slug}/inquiries`, body);
  }

  dashboard(from?: string, to?: string): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/v1/dashboard', { params: this.params({ from, to }) });
  }

  brand(): Observable<Brand> {
    return this.http.get<Brand>('/api/v1/company/brand');
  }

  company(): Observable<CompanyProfile> {
    return this.http.get<CompanyProfile>('/api/v1/company');
  }

  updateCompany(body: Partial<CompanyProfile>): Observable<CompanyProfile> {
    return this.http.put<CompanyProfile>('/api/v1/company', body);
  }

  uploadLogo(file: File): Observable<CompanyProfile> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<CompanyProfile>('/api/v1/company/logo', data);
  }

  deleteLogo(): Observable<CompanyProfile> {
    return this.http.delete<CompanyProfile>('/api/v1/company/logo');
  }

  members(): Observable<Member[]> {
    return this.http.get<Member[]>('/api/v1/company/members');
  }

  addMember(body: { fullName: string; email: string; phone: string; role: string; password: string }): Observable<Member> {
    return this.http.post<Member>('/api/v1/company/members', body);
  }

  updateMember(id: string, body: { role: string; status: string }): Observable<Member> {
    return this.http.patch<Member>(`/api/v1/company/members/${id}`, body);
  }

  projects(filters: Record<string, string | number | null | undefined>): Observable<Page<ProjectDetail>> {
    return this.http.get<Page<ProjectDetail>>('/api/v1/projects', { params: this.params(filters) });
  }

  project(id: string): Observable<ProjectDetail> {
    return this.http.get<ProjectDetail>(`/api/v1/projects/${id}`);
  }

  saveProject(id: string | null, body: unknown): Observable<ProjectDetail> {
    return id ? this.http.put<ProjectDetail>(`/api/v1/projects/${id}`, body) : this.http.post<ProjectDetail>('/api/v1/projects', body);
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/projects/${id}`);
  }

  publish(id: string, publication: string, featured: boolean): Observable<ProjectDetail> {
    return this.http.patch<ProjectDetail>(`/api/v1/projects/${id}/publication`, { publication, featured });
  }

  notifyBuyers(id: string, message: string): Observable<void> {
    return this.http.post<void>(`/api/v1/projects/${id}/notify`, { message });
  }

  uploadProjectImage(id: string, file: File, alt: string): Observable<unknown> {
    const data = new FormData();
    data.append('file', file);
    data.append('alt', alt);
    return this.http.post(`/api/v1/projects/${id}/images`, data);
  }

  deleteProjectImage(projectId: string, imageId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/projects/${projectId}/images/${imageId}`);
  }

  buildings(projectId: string): Observable<Building[]> {
    return this.http.get<Building[]>(`/api/v1/projects/${projectId}/buildings`);
  }

  saveBuilding(projectId: string, id: string | null, body: { name: string; code: string; floors: number | null }): Observable<Building> {
    return id
      ? this.http.put<Building>(`/api/v1/buildings/${id}`, body)
      : this.http.post<Building>(`/api/v1/projects/${projectId}/buildings`, body);
  }

  deleteBuilding(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/buildings/${id}`);
  }

  units(filters: Record<string, string | number | null | undefined>): Observable<Page<Unit>> {
    return this.http.get<Page<Unit>>('/api/v1/units', { params: this.params(filters) });
  }

  unitsOf(projectId: string): Observable<Unit[]> {
    return this.http.get<Unit[]>(`/api/v1/projects/${projectId}/units`);
  }

  saveUnit(projectId: string, id: string | null, body: unknown): Observable<Unit> {
    return id ? this.http.put<Unit>(`/api/v1/units/${id}`, body) : this.http.post<Unit>(`/api/v1/projects/${projectId}/units`, body);
  }

  changeAvailability(id: string, availability: string): Observable<Unit> {
    return this.http.patch<Unit>(`/api/v1/units/${id}/availability`, { availability });
  }

  deleteUnit(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/units/${id}`);
  }

  pipeline(): Observable<Stage[]> {
    return this.http.get<Stage[]>('/api/v1/pipeline');
  }

  addStage(name: string): Observable<Stage> {
    return this.http.post<Stage>('/api/v1/pipeline', { name });
  }

  leads(filters: Record<string, string | number | boolean | null | undefined>): Observable<Page<Lead>> {
    return this.http.get<Page<Lead>>('/api/v1/leads', { params: this.params(filters) });
  }

  lead(id: string): Observable<{ lead: Lead; activities: Activity[] }> {
    return this.http.get<{ lead: Lead; activities: Activity[] }>(`/api/v1/leads/${id}`);
  }

  saveLead(id: string | null, body: unknown): Observable<Lead> {
    return id ? this.http.put<Lead>(`/api/v1/leads/${id}`, body) : this.http.post<Lead>('/api/v1/leads', body);
  }

  addActivity(id: string, type: string, description: string): Observable<Activity> {
    return this.http.post<Activity>(`/api/v1/leads/${id}/activities`, { type, description });
  }

  reservations(filters: Record<string, string | number | null | undefined>): Observable<Page<Reservation>> {
    return this.http.get<Page<Reservation>>('/api/v1/reservations', { params: this.params(filters) });
  }

  reserve(body: unknown): Observable<Reservation> {
    return this.http.post<Reservation>('/api/v1/reservations', body);
  }

  cancelReservation(id: string, notes: string): Observable<Reservation> {
    return this.http.post<Reservation>(`/api/v1/reservations/${id}/cancel`, { notes });
  }

  customers(filters: Record<string, string | number | null | undefined>): Observable<Page<Customer>> {
    return this.http.get<Page<Customer>>('/api/v1/customers', { params: this.params(filters) });
  }

  saveCustomer(id: string | null, body: unknown): Observable<Customer> {
    return id ? this.http.put<Customer>(`/api/v1/customers/${id}`, body) : this.http.post<Customer>('/api/v1/customers', body);
  }

  grantPortal(id: string, password: string): Observable<Customer> {
    return this.http.post<Customer>(`/api/v1/customers/${id}/portal`, { password });
  }

  contracts(filters: Record<string, string | number | null | undefined>): Observable<Page<Contract>> {
    return this.http.get<Page<Contract>>('/api/v1/contracts', { params: this.params(filters) });
  }

  contract(id: string): Observable<{ contract: Contract; parties: ContractParty[] }> {
    return this.http.get<{ contract: Contract; parties: ContractParty[] }>(`/api/v1/contracts/${id}`);
  }

  saveContract(id: string | null, body: unknown): Observable<Contract> {
    return id ? this.http.put<Contract>(`/api/v1/contracts/${id}`, body) : this.http.post<Contract>('/api/v1/contracts', body);
  }

  addParty(id: string, customerId: string, primary: boolean): Observable<ContractParty> {
    return this.http.post<ContractParty>(`/api/v1/contracts/${id}/parties`, { customerId, primary });
  }

  removeParty(id: string, customerId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/contracts/${id}/parties/${customerId}`);
  }

  activateContract(id: string): Observable<Contract> {
    return this.http.post<Contract>(`/api/v1/contracts/${id}/activate`, {});
  }

  completeContract(id: string): Observable<Contract> {
    return this.http.post<Contract>(`/api/v1/contracts/${id}/complete`, {});
  }

  annulContract(id: string, reason: string): Observable<Contract> {
    return this.http.post<Contract>(`/api/v1/contracts/${id}/annul`, { reason });
  }

  plan(contractId: string): Observable<PlanView> {
    return this.http.get<PlanView>(`/api/v1/contracts/${contractId}/payment-plan`);
  }

  savePlan(contractId: string, body: unknown): Observable<PlanView> {
    return this.http.put<PlanView>(`/api/v1/contracts/${contractId}/payment-plan`, body);
  }

  payments(filters: Record<string, string | number | null | undefined>): Observable<Page<Payment>> {
    return this.http.get<Page<Payment>>('/api/v1/payments', { params: this.params(filters) });
  }

  registerPayment(contractId: string, amount: number, reference: string, notes: string, receipt: File | null): Observable<Payment> {
    const data = new FormData();
    data.append('amount', String(amount));
    if (reference) data.append('reference', reference);
    if (notes) data.append('notes', notes);
    if (receipt) data.append('receipt', receipt);
    return this.http.post<Payment>(`/api/v1/contracts/${contractId}/payments`, data);
  }

  validatePayment(id: string): Observable<Payment> {
    return this.http.post<Payment>(`/api/v1/payments/${id}/validate`, {});
  }

  rejectPayment(id: string, reason: string): Observable<Payment> {
    return this.http.post<Payment>(`/api/v1/payments/${id}/reject`, { reason });
  }

  reversePayment(id: string, reason: string): Observable<Payment> {
    return this.http.post<Payment>(`/api/v1/payments/${id}/reverse`, { reason });
  }

  documents(ownerType: string, ownerId: string): Observable<DocumentItem[]> {
    return this.http.get<DocumentItem[]>('/api/v1/documents', { params: { ownerType, ownerId } });
  }

  uploadDocument(ownerType: string, ownerId: string, visibility: string, file: File): Observable<DocumentItem> {
    const data = new FormData();
    data.append('ownerType', ownerType);
    data.append('ownerId', ownerId);
    data.append('visibility', visibility);
    data.append('file', file);
    return this.http.post<DocumentItem>('/api/v1/documents', data);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/documents/${id}`);
  }

  downloadDocument(id: string, fileName: string): Observable<Blob> {
    return this.http.get(`/api/v1/documents/${id}/content`, { responseType: 'blob' }).pipe(
      tap((blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName || 'documento';
        anchor.rel = 'noopener';
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(url);
      }),
    );
  }

  notifications(page = 0): Observable<Page<Notice>> {
    return this.http.get<Page<Notice>>('/api/v1/notifications', { params: { page } });
  }

  unread(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>('/api/v1/notifications/unread');
  }

  markRead(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>('/api/v1/notifications/read-all', {});
  }

  companyTemplates(): Observable<TemplateView[]> {
    return this.http.get<TemplateView[]>('/api/v1/company/notification-templates');
  }

  saveCompanyTemplate(code: string, subject: string, body: string, enabled: boolean): Observable<TemplateView> {
    return this.http.put<TemplateView>(`/api/v1/company/notification-templates/${code}`, { subject, body, enabled });
  }

  audit(page = 0): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>('/api/v1/audit', { params: { page } });
  }

  platformMetrics(): Observable<PlatformMetrics> {
    return this.http.get<PlatformMetrics>('/api/v1/platform/metrics');
  }

  platformCompanies(filters: Record<string, string | number | null | undefined>): Observable<Page<CompanyProfile>> {
    return this.http.get<Page<CompanyProfile>>('/api/v1/platform/companies', { params: this.params(filters) });
  }

  createCompany(body: unknown): Observable<CompanyProfile> {
    return this.http.post<CompanyProfile>('/api/v1/platform/companies', body);
  }

  companyStatus(id: string, status: string): Observable<CompanyProfile> {
    return this.http.patch<CompanyProfile>(`/api/v1/platform/companies/${id}/status`, { status });
  }

  platformSettings(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>('/api/v1/platform/settings');
  }

  updatePlatformSettings(body: unknown): Observable<Record<string, string>> {
    return this.http.put<Record<string, string>>('/api/v1/platform/settings', body);
  }

  platformTemplates(): Observable<TemplateView[]> {
    return this.http.get<TemplateView[]>('/api/v1/platform/templates');
  }

  updatePlatformTemplate(code: string, subject: string, body: string): Observable<TemplateView> {
    return this.http.put<TemplateView>(`/api/v1/platform/templates/${code}`, { subject, body });
  }

  platformAudit(page = 0): Observable<Page<AuditEvent>> {
    return this.http.get<Page<AuditEvent>>('/api/v1/platform/audit', { params: { page } });
  }

  buyerSummary(): Observable<BuyerHome> {
    return this.http.get<BuyerHome>('/api/v1/buyer/summary');
  }

  buyerContract(id: string): Observable<BuyerOperation> {
    return this.http.get<BuyerOperation>(`/api/v1/buyer/contracts/${id}`);
  }

  reportPayment(contractId: string, amount: number, reference: string, notes: string, file: File): Observable<Payment> {
    const data = new FormData();
    data.append('amount', String(amount));
    if (reference) data.append('reference', reference);
    if (notes) data.append('notes', notes);
    data.append('file', file);
    return this.http.post<Payment>(`/api/v1/buyer/contracts/${contractId}/payment-reports`, data);
  }

  buyerDocuments(): Observable<DocumentItem[]> {
    return this.http.get<DocumentItem[]>('/api/v1/buyer/documents');
  }

  private params(filters: Record<string, string | number | boolean | null | undefined>): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
