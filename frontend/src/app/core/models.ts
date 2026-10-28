export interface Problem {
  status?: number;
  detail?: string;
  code?: string;
  fields?: Record<string, string>;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Membership {
  id: string;
  companyId: string;
  companyName: string;
  companySlug: string;
  companyStatus: string;
  logoUrl: string | null;
  role: string;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  platformAdmin: boolean;
  memberships: Membership[];
}

export interface Session {
  accessToken: string;
  expiresIn: number;
  user: User;
}

export interface Province {
  code: string;
  name: string;
  municipalities: { id: string; name: string }[];
}

export interface ProjectCard {
  id: string;
  slug: string;
  name: string;
  developmentType: string;
  status: string;
  provinceName: string;
  municipalityName: string;
  minPrice: number | null;
  currency: string;
  estimatedDelivery: string | null;
  featured: boolean;
  companyName: string;
  companySlug: string;
  companyLogoUrl: string | null;
  coverUrl: string | null;
}

export interface PublicUnit {
  id: string;
  code: string;
  propertyType: string;
  bedrooms: number | null;
  bathrooms: number | null;
  areaM2: number | null;
  levelLabel: string | null;
  price: number;
  currency: string;
  availability: string;
  features: string[];
  buildingName: string | null;
}

export interface PublicBuilding {
  id: string;
  name: string;
  code: string;
  floors: number | null;
}

export interface ProjectPage {
  summary: ProjectCard;
  description: string | null;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  estimatedStart: string | null;
  estimatedDelivery: string | null;
  amenities: string[];
  features: string[];
  commercialInfo: string | null;
  images: { id: string; altText: string | null; url: string }[];
  buildings: PublicBuilding[];
  units: PublicUnit[];
  company: {
    id: string;
    slug: string;
    commercialName: string;
    description: string | null;
    logoUrl: string | null;
    publicPhone: string | null;
    publicEmail: string | null;
    website: string | null;
    publicContact: string | null;
  };
}

export interface CompanyPage {
  id: string;
  slug: string;
  commercialName: string;
  description: string | null;
  logoUrl: string | null;
  publicPhone: string | null;
  publicEmail: string | null;
  website: string | null;
  publicContact: string | null;
  provinceName: string | null;
  municipalityName: string | null;
  address: string | null;
  projects: ProjectCard[];
}

export interface CompanyProfile {
  id: string;
  slug: string;
  commercialName: string;
  legalName: string;
  rnc: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  provinceCode: string | null;
  provinceName: string | null;
  municipalityId: string | null;
  municipalityName: string | null;
  website: string | null;
  description: string | null;
  logoUrl: string | null;
  publicPhone: string | null;
  publicEmail: string | null;
  publicContact: string | null;
  status: string;
}

export interface Brand {
  id: string;
  commercialName: string;
  slug: string;
  logoUrl: string | null;
  publicPhone: string | null;
  publicEmail: string | null;
  publicContact: string | null;
}

export interface Member {
  id: string;
  userId: string;
  fullName: string;
  email: string;
  phone: string | null;
  role: string;
  status: string;
}

export interface Dashboard {
  projects: number;
  availableUnits: number;
  reservedUnits: number;
  soldUnits: number;
  openLeads: number;
  reservationsExpiringSoon: number;
  overdueInstallments: number;
  collections: number;
  currency: string;
  from: string;
  to: string;
}

export interface PlatformMetrics {
  activeCompanies: number;
  suspendedCompanies: number;
  publishedProjects: number;
  users: number;
}

export interface ProjectImage {
  id: string;
  altText: string | null;
  sortOrder: number;
  url: string;
}

export interface ProjectDetail {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  developmentType: string;
  provinceCode: string;
  provinceName: string;
  municipalityId: string;
  municipalityName: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  estimatedStart: string | null;
  estimatedDelivery: string | null;
  status: string;
  publicationStatus: string;
  featured: boolean;
  amenities: string[];
  features: string[];
  commercialInfo: string | null;
  internalNotes: string | null;
  minPrice: number | null;
  currency: string;
  images: ProjectImage[];
}

export interface Building {
  id: string;
  projectId: string;
  name: string;
  code: string;
  floors: number | null;
}

export interface Unit {
  id: string;
  projectId: string;
  buildingId: string | null;
  code: string;
  propertyType: string;
  bedrooms: number | null;
  bathrooms: number | null;
  areaM2: number | null;
  levelLabel: string | null;
  price: number;
  currency: string;
  features: string[];
  availability: string;
  publiclyVisible: boolean;
}

export interface Stage {
  id: string;
  code: string;
  name: string;
  sortOrder: number;
  closed: boolean;
  systemDefault: boolean;
}

export interface Lead {
  id: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  projectId: string | null;
  unitId: string | null;
  assignedUserId: string | null;
  stageCode: string;
  source: string;
  notes: string | null;
  nextStep: string | null;
  nextStepDate: string | null;
  createdAt: string;
}

export interface Activity {
  id: string;
  activityType: string;
  description: string;
  createdAt: string;
}

export interface Reservation {
  id: string;
  unitId: string;
  leadId: string | null;
  customerId: string | null;
  expiresAt: string;
  amount: number | null;
  currency: string;
  status: string;
  notes: string | null;
  createdAt: string;
}

export interface Customer {
  id: string;
  userId: string | null;
  fullName: string;
  email: string | null;
  phone: string | null;
  nationalId: string | null;
  address: string | null;
}

export interface Contract {
  id: string;
  projectId: string;
  unitId: string;
  reservationId: string | null;
  agreedPrice: number;
  currency: string;
  agreementDate: string;
  agreedDownPayment: number;
  status: string;
  notes: string | null;
  annulmentReason: string | null;
}

export interface ContractParty {
  id: string;
  customerId: string;
  primaryBuyer: boolean;
}

export interface Installment {
  id: string;
  sequenceNumber: number;
  kind: string;
  label: string;
  dueDate: string;
  amount: number;
  paidAmount: number;
  status: string;
}

export interface Payment {
  id: string;
  contractId: string;
  amount: number;
  currency: string;
  status: string;
  referenceNumber: string | null;
  notes: string | null;
  rejectionReason: string | null;
  reversalReason: string | null;
  receiptDocumentId: string | null;
  validatedAt: string | null;
  createdAt: string;
}

export interface Allocation {
  id: string;
  paymentId: string;
  installmentId: string;
  amount: number;
  voided: boolean;
}

export interface PlanView {
  plan: {
    id: string;
    totalPrice: number;
    downPayment: number;
    installmentCount: number;
    frequency: string;
    currency: string;
    deliveryAmount: number | null;
    conditions: string | null;
  } | null;
  installments: Installment[];
  payments: Payment[];
  allocations: Allocation[];
  paid: number;
  balance: number;
}

export interface DocumentItem {
  id: string;
  ownerType: string;
  ownerId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  visibility: string;
  createdAt: string;
}

export interface Notice {
  id: string;
  type: string;
  title: string;
  body: string;
  readAt: string | null;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  action: string;
  entityType: string;
  summary: string;
  createdAt: string;
}

export interface TemplateView {
  code: string;
  description: string;
  subject: string;
  body: string;
  enabled?: boolean;
  customized?: boolean;
}

export interface BuyerHome {
  customerName: string | null;
  logoUrl: string | null;
  companyName: string;
  publicPhone: string | null;
  publicEmail: string | null;
  operations: BuyerOperation[];
}

export interface BuyerOperation {
  contractId: string;
  status: string;
  agreedPrice: number;
  agreedDownPayment: number;
  currency: string;
  agreementDate: string;
  projectName: string | null;
  projectSlug: string | null;
  unitCode: string | null;
  propertyType: string | null;
  plan: PlanView | null;
}
