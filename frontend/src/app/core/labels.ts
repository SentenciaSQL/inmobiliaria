export const ROLE_LABELS: Record<string, string> = {
  COMPANY_ADMIN: 'Administrador',
  SALES_ADVISOR: 'Asesor comercial',
  FINANCE: 'Finanzas',
  BUYER: 'Comprador',
  PLATFORM_ADMIN: 'Administrador de la plataforma',
};

export const PROJECT_TYPES: Record<string, string> = {
  APARTMENT_BUILDING: 'Torre de apartamentos',
  HOUSE_COMMUNITY: 'Comunidad de casas',
  MIXED: 'Desarrollo mixto',
  LOTS: 'Solares',
  COMMERCIAL: 'Comercial',
};

export const PROJECT_STATUS: Record<string, string> = {
  PLANNING: 'En planificación',
  IN_CONSTRUCTION: 'En construcción',
  READY: 'Listo',
  DELIVERED: 'Entregado',
  PAUSED: 'Pausado',
};

export const PUBLICATION: Record<string, string> = {
  DRAFT: 'Borrador',
  PUBLISHED: 'Publicado',
  HIDDEN: 'Oculto',
};

export const PROPERTY_TYPES: Record<string, string> = {
  APARTMENT: 'Apartamento',
  HOUSE: 'Casa',
  PENTHOUSE: 'Penthouse',
  LOT: 'Solar',
  COMMERCIAL: 'Local comercial',
  PARKING: 'Parqueo',
};

export const AVAILABILITY: Record<string, string> = {
  AVAILABLE: 'Disponible',
  RESERVED: 'Reservado',
  SOLD: 'Vendido',
  OFF_MARKET: 'Fuera de mercado',
};

export const CONTRACT_STATUS: Record<string, string> = {
  DRAFT: 'Borrador',
  ACTIVE: 'Activo',
  COMPLETED: 'Completado',
  ANNULLED: 'Anulado',
};

export const PAYMENT_STATUS: Record<string, string> = {
  REPORTED: 'Reportado',
  VALIDATED: 'Confirmado',
  REJECTED: 'Rechazado',
  REVERSED: 'Reversado',
};

export const INSTALLMENT_STATUS: Record<string, string> = {
  PENDING: 'Pendiente',
  PARTIAL: 'Abono parcial',
  PAID: 'Pagada',
  OVERDUE: 'Vencida',
  CANCELLED: 'Cancelada',
};

export const RESERVATION_STATUS: Record<string, string> = {
  ACTIVE: 'Activa',
  EXPIRED: 'Expirada',
  CANCELLED: 'Cancelada',
  CONVERTED: 'Convertida',
};

export function label(map: Record<string, string>, value: string | null | undefined): string {
  if (!value) return '—';
  return map[value] ?? value;
}
