CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION app_current_tenant() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid
$$;

CREATE OR REPLACE FUNCTION app_current_user() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('app.user_id', true), '')::uuid
$$;

CREATE OR REPLACE FUNCTION app_bypass() RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT current_setting('app.bypass_rls', true) = 'true'
$$;

CREATE OR REPLACE FUNCTION app_mode() RETURNS text
LANGUAGE sql STABLE AS $$
  SELECT COALESCE(current_setting('app.access_mode', true), '')
$$;

CREATE TABLE provinces (
  code varchar(2) PRIMARY KEY,
  name varchar(80) NOT NULL
);

CREATE TABLE municipalities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  province_code varchar(2) NOT NULL REFERENCES provinces(code),
  name varchar(120) NOT NULL,
  UNIQUE (province_code, name)
);

CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(180) NOT NULL,
  password_hash varchar(100) NOT NULL,
  full_name varchar(160) NOT NULL,
  phone varchar(30),
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  platform_admin boolean NOT NULL DEFAULT false,
  last_login_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email)) WHERE deleted_at IS NULL;

CREATE TABLE refresh_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  token_hash varchar(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  replaced_by_id uuid,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE companies (
  id uuid PRIMARY KEY,
  slug varchar(160) NOT NULL UNIQUE,
  commercial_name varchar(160) NOT NULL,
  legal_name varchar(200) NOT NULL,
  rnc varchar(20),
  phone varchar(30),
  email varchar(180),
  address varchar(240),
  province_code varchar(2) REFERENCES provinces(code),
  municipality_id uuid REFERENCES municipalities(id),
  website varchar(200),
  description text,
  logo_object_key varchar(400),
  logo_content_type varchar(80),
  public_phone varchar(30),
  public_email varchar(180),
  public_contact text,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  search_text varchar(400),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT companies_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE memberships (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES users(id),
  company_id uuid NOT NULL REFERENCES companies(id),
  role varchar(32) NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT memberships_role CHECK (role IN ('COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE', 'BUYER')),
  CONSTRAINT memberships_status CHECK (status IN ('ACTIVE', 'DISABLED')),
  UNIQUE (user_id, company_id, role)
);

CREATE INDEX idx_memberships_company ON memberships (company_id);
CREATE INDEX idx_memberships_user ON memberships (user_id);

CREATE TABLE platform_settings (
  setting_key varchar(80) PRIMARY KEY,
  setting_value text NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE notification_templates (
  code varchar(60) PRIMARY KEY,
  description varchar(240) NOT NULL,
  subject varchar(180) NOT NULL,
  body text NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE TABLE projects (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  name varchar(180) NOT NULL,
  slug varchar(180) NOT NULL UNIQUE,
  description text,
  development_type varchar(40) NOT NULL,
  province_code varchar(2) REFERENCES provinces(code),
  municipality_id uuid REFERENCES municipalities(id),
  address varchar(240),
  latitude numeric(9, 6),
  longitude numeric(9, 6),
  estimated_start date,
  estimated_delivery date,
  status varchar(32) NOT NULL,
  publication_status varchar(20) NOT NULL,
  featured boolean NOT NULL DEFAULT false,
  amenities text NOT NULL DEFAULT '[]',
  features text NOT NULL DEFAULT '[]',
  commercial_info text,
  internal_notes text,
  min_price numeric(14, 2),
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  published_at timestamptz,
  search_text varchar(500),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT projects_dev_type CHECK (development_type IN ('APARTMENT_BUILDING', 'HOUSE_COMMUNITY', 'MIXED', 'LOTS', 'COMMERCIAL')),
  CONSTRAINT projects_status CHECK (status IN ('PLANNING', 'IN_CONSTRUCTION', 'READY', 'DELIVERED', 'PAUSED')),
  CONSTRAINT projects_publication CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'HIDDEN')),
  CONSTRAINT projects_currency CHECK (currency ~ '^[A-Z]{3}$'),
  CONSTRAINT projects_price CHECK (min_price IS NULL OR min_price >= 0),
  CONSTRAINT projects_lat CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
  CONSTRAINT projects_lng CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_projects_tenant ON projects (tenant_id);
CREATE INDEX idx_projects_publication ON projects (publication_status, province_code);

CREATE TABLE project_images (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  project_id uuid NOT NULL REFERENCES projects(id),
  object_key varchar(400) NOT NULL,
  content_type varchar(80) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  alt_text varchar(180),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE INDEX idx_project_images_project ON project_images (project_id, sort_order);

CREATE TABLE buildings (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  project_id uuid NOT NULL REFERENCES projects(id),
  name varchar(120) NOT NULL,
  code varchar(40) NOT NULL,
  floors integer,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE (project_id, code)
);

CREATE TABLE units (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  project_id uuid NOT NULL REFERENCES projects(id),
  building_id uuid REFERENCES buildings(id),
  code varchar(40) NOT NULL,
  property_type varchar(32) NOT NULL,
  bedrooms integer,
  bathrooms numeric(4, 1),
  area_m2 numeric(10, 2),
  level_label varchar(40),
  price numeric(14, 2) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  features text NOT NULL DEFAULT '[]',
  availability varchar(20) NOT NULL,
  publicly_visible boolean NOT NULL DEFAULT true,
  row_version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT units_type CHECK (property_type IN ('APARTMENT', 'HOUSE', 'PENTHOUSE', 'LOT', 'COMMERCIAL', 'PARKING')),
  CONSTRAINT units_availability CHECK (availability IN ('AVAILABLE', 'RESERVED', 'SOLD', 'OFF_MARKET')),
  CONSTRAINT units_price CHECK (price >= 0),
  CONSTRAINT units_area CHECK (area_m2 IS NULL OR area_m2 >= 0),
  CONSTRAINT units_bedrooms CHECK (bedrooms IS NULL OR bedrooms >= 0),
  CONSTRAINT units_bathrooms CHECK (bathrooms IS NULL OR bathrooms >= 0),
  CONSTRAINT units_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uq_units_code ON units (project_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_units_tenant_availability ON units (tenant_id, availability);

CREATE TABLE lead_pipeline_stages (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  code varchar(40) NOT NULL,
  name varchar(80) NOT NULL,
  sort_order integer NOT NULL,
  closed boolean NOT NULL DEFAULT false,
  system_default boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE (tenant_id, code)
);

CREATE TABLE leads (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  full_name varchar(160) NOT NULL,
  email varchar(180),
  phone varchar(30),
  project_id uuid REFERENCES projects(id),
  unit_id uuid REFERENCES units(id),
  assigned_user_id uuid REFERENCES users(id),
  stage_code varchar(40) NOT NULL,
  source varchar(32) NOT NULL,
  notes text,
  next_step varchar(240),
  next_step_date date,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT leads_source CHECK (source IN ('MARKETPLACE', 'MANUAL', 'PHONE', 'REFERRAL', 'OTHER')),
  CONSTRAINT leads_stage_fk FOREIGN KEY (tenant_id, stage_code) REFERENCES lead_pipeline_stages (tenant_id, code)
);

CREATE INDEX idx_leads_tenant_stage ON leads (tenant_id, stage_code);

CREATE TABLE lead_activities (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  lead_id uuid NOT NULL REFERENCES leads(id),
  activity_type varchar(32) NOT NULL,
  description text NOT NULL,
  created_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT activity_type CHECK (activity_type IN ('NOTE', 'CALL', 'VISIT', 'EMAIL', 'STATUS_CHANGE', 'WHATSAPP'))
);

CREATE INDEX idx_lead_activities_lead ON lead_activities (lead_id, created_at);

CREATE TABLE customers (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  user_id uuid REFERENCES users(id),
  full_name varchar(160) NOT NULL,
  email varchar(180),
  phone varchar(30),
  national_id varchar(20),
  address varchar(240),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz
);

CREATE UNIQUE INDEX uq_customer_user ON customers (tenant_id, user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_customers_tenant ON customers (tenant_id);

CREATE TABLE reservations (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  unit_id uuid NOT NULL REFERENCES units(id),
  lead_id uuid REFERENCES leads(id),
  customer_id uuid REFERENCES customers(id),
  expires_at timestamptz NOT NULL,
  amount numeric(14, 2),
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  status varchar(20) NOT NULL,
  responsible_user_id uuid REFERENCES users(id),
  notes text,
  expiry_notice_sent_at timestamptz,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT reservations_party CHECK (lead_id IS NOT NULL OR customer_id IS NOT NULL),
  CONSTRAINT reservations_amount CHECK (amount IS NULL OR amount >= 0),
  CONSTRAINT reservations_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'CONVERTED')),
  CONSTRAINT reservations_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uq_unit_active_reservation ON reservations (unit_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_reservations_expiry ON reservations (status, expires_at);

CREATE TABLE contracts (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  project_id uuid NOT NULL REFERENCES projects(id),
  unit_id uuid NOT NULL REFERENCES units(id),
  reservation_id uuid REFERENCES reservations(id),
  agreed_price numeric(14, 2) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  agreement_date date NOT NULL,
  agreed_down_payment numeric(14, 2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL,
  notes text,
  created_by uuid REFERENCES users(id),
  annulled_at timestamptz,
  annulment_reason varchar(400),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT contracts_price CHECK (agreed_price > 0),
  CONSTRAINT contracts_down CHECK (agreed_down_payment >= 0 AND agreed_down_payment <= agreed_price),
  CONSTRAINT contracts_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ANNULLED')),
  CONSTRAINT contracts_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uq_unit_open_contract ON contracts (unit_id) WHERE status IN ('DRAFT', 'ACTIVE');
CREATE INDEX idx_contracts_tenant ON contracts (tenant_id, status);

CREATE TABLE contract_parties (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  contract_id uuid NOT NULL REFERENCES contracts(id),
  customer_id uuid NOT NULL REFERENCES customers(id),
  primary_buyer boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE (contract_id, customer_id)
);

CREATE TABLE payment_plans (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  contract_id uuid NOT NULL UNIQUE REFERENCES contracts(id),
  total_price numeric(14, 2) NOT NULL,
  down_payment numeric(14, 2) NOT NULL DEFAULT 0,
  installment_count integer NOT NULL,
  frequency varchar(20) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  delivery_amount numeric(14, 2),
  conditions text,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT plans_total CHECK (total_price > 0),
  CONSTRAINT plans_down CHECK (down_payment >= 0 AND down_payment <= total_price),
  CONSTRAINT plans_count CHECK (installment_count >= 0 AND installment_count <= 600),
  CONSTRAINT plans_delivery CHECK (delivery_amount IS NULL OR delivery_amount >= 0),
  CONSTRAINT plans_frequency CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY')),
  CONSTRAINT plans_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE installments (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  plan_id uuid NOT NULL REFERENCES payment_plans(id),
  contract_id uuid NOT NULL REFERENCES contracts(id),
  sequence_number integer NOT NULL,
  kind varchar(24) NOT NULL,
  label varchar(120) NOT NULL,
  due_date date NOT NULL,
  amount numeric(14, 2) NOT NULL,
  paid_amount numeric(14, 2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL,
  upcoming_notice_sent_at timestamptz,
  overdue_notice_sent_at timestamptz,
  row_version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT inst_amount CHECK (amount > 0),
  CONSTRAINT inst_paid CHECK (paid_amount >= 0 AND paid_amount <= amount),
  CONSTRAINT inst_kind CHECK (kind IN ('DOWN_PAYMENT', 'REGULAR', 'EXTRAORDINARY', 'DELIVERY')),
  CONSTRAINT inst_status CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED')),
  UNIQUE (plan_id, sequence_number)
);

CREATE INDEX idx_installments_due ON installments (status, due_date);
CREATE INDEX idx_installments_contract ON installments (contract_id, sequence_number);

CREATE TABLE payments (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  contract_id uuid NOT NULL REFERENCES contracts(id),
  amount numeric(14, 2) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'DOP',
  status varchar(20) NOT NULL,
  reported_by uuid REFERENCES users(id),
  validated_by uuid REFERENCES users(id),
  validated_at timestamptz,
  rejection_reason varchar(400),
  reversed_by uuid REFERENCES users(id),
  reversed_at timestamptz,
  reversal_reason varchar(400),
  reference_number varchar(80),
  notes text,
  receipt_document_id uuid,
  row_version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT pay_amount CHECK (amount > 0),
  CONSTRAINT pay_status CHECK (status IN ('REPORTED', 'VALIDATED', 'REJECTED', 'REVERSED')),
  CONSTRAINT pay_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX uq_payment_reference ON payments (tenant_id, reference_number)
  WHERE reference_number IS NOT NULL AND status IN ('REPORTED', 'VALIDATED');
CREATE INDEX idx_payments_tenant_status ON payments (tenant_id, status);

CREATE TABLE payment_allocations (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  payment_id uuid NOT NULL REFERENCES payments(id),
  installment_id uuid NOT NULL REFERENCES installments(id),
  amount numeric(14, 2) NOT NULL,
  voided boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT alloc_amount CHECK (amount > 0),
  UNIQUE (payment_id, installment_id)
);

CREATE TABLE documents (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  owner_type varchar(20) NOT NULL,
  owner_id uuid NOT NULL,
  object_key varchar(400) NOT NULL,
  file_name varchar(240) NOT NULL,
  content_type varchar(120) NOT NULL,
  size_bytes bigint NOT NULL,
  visibility varchar(20) NOT NULL,
  uploaded_by uuid REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  deleted_at timestamptz,
  CONSTRAINT docs_owner CHECK (owner_type IN ('COMPANY', 'PROJECT', 'CONTRACT', 'CUSTOMER', 'PAYMENT')),
  CONSTRAINT docs_visibility CHECK (visibility IN ('PRIVATE', 'BUYER_VISIBLE')),
  CONSTRAINT docs_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_documents_owner ON documents (tenant_id, owner_type, owner_id);

ALTER TABLE payments
  ADD CONSTRAINT payments_receipt_fk FOREIGN KEY (receipt_document_id) REFERENCES documents(id);

CREATE TABLE notifications (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  user_id uuid NOT NULL REFERENCES users(id),
  type varchar(60) NOT NULL,
  title varchar(180) NOT NULL,
  body text NOT NULL,
  read_at timestamptz,
  email_status varchar(20) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT notif_email CHECK (email_status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);

CREATE TABLE company_notification_templates (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL REFERENCES companies(id),
  code varchar(60) NOT NULL,
  subject varchar(180) NOT NULL,
  body text NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  UNIQUE (tenant_id, code)
);

CREATE TABLE audit_events (
  id uuid PRIMARY KEY,
  tenant_id uuid REFERENCES companies(id),
  actor_id uuid REFERENCES users(id),
  action varchar(80) NOT NULL,
  entity_type varchar(60) NOT NULL,
  entity_id uuid,
  summary varchar(400) NOT NULL,
  details text,
  ip_address varchar(64),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE INDEX idx_audit_tenant ON audit_events (tenant_id, created_at DESC);

CREATE OR REPLACE FUNCTION app_apply_tenant_rls(target regclass) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
  EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', target);
  EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', target);
  EXECUTE format('CREATE POLICY tenant_isolation ON %s USING (tenant_id = app_current_tenant()) WITH CHECK (tenant_id = app_current_tenant())', target);
  EXECUTE format('CREATE POLICY tenant_bypass ON %s USING (app_bypass()) WITH CHECK (app_bypass())', target);
END;
$$;

SELECT app_apply_tenant_rls('projects');
SELECT app_apply_tenant_rls('project_images');
SELECT app_apply_tenant_rls('buildings');
SELECT app_apply_tenant_rls('units');
SELECT app_apply_tenant_rls('lead_pipeline_stages');
SELECT app_apply_tenant_rls('leads');
SELECT app_apply_tenant_rls('lead_activities');
SELECT app_apply_tenant_rls('customers');
SELECT app_apply_tenant_rls('reservations');
SELECT app_apply_tenant_rls('contracts');
SELECT app_apply_tenant_rls('contract_parties');
SELECT app_apply_tenant_rls('payment_plans');
SELECT app_apply_tenant_rls('installments');
SELECT app_apply_tenant_rls('payments');
SELECT app_apply_tenant_rls('payment_allocations');
SELECT app_apply_tenant_rls('documents');
SELECT app_apply_tenant_rls('company_notification_templates');

ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;
CREATE POLICY companies_self ON companies
  USING (id = app_current_tenant())
  WITH CHECK (id = app_current_tenant());
CREATE POLICY companies_bypass ON companies
  USING (app_bypass())
  WITH CHECK (app_bypass());
CREATE POLICY companies_marketplace ON companies
  FOR SELECT
  USING (app_mode() = 'marketplace' AND status = 'ACTIVE' AND deleted_at IS NULL);

ALTER TABLE memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE memberships FORCE ROW LEVEL SECURITY;
CREATE POLICY memberships_access ON memberships
  USING (user_id = app_current_user() OR company_id = app_current_tenant())
  WITH CHECK (company_id = app_current_tenant());
CREATE POLICY memberships_bypass ON memberships
  USING (app_bypass())
  WITH CHECK (app_bypass());

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;
CREATE POLICY notifications_own ON notifications
  USING (user_id = app_current_user() OR app_bypass())
  WITH CHECK (tenant_id = app_current_tenant() OR app_bypass());

ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_tenant ON audit_events
  USING ((tenant_id IS NOT NULL AND tenant_id = app_current_tenant()) OR app_bypass())
  WITH CHECK (tenant_id = app_current_tenant() OR app_bypass());

CREATE POLICY projects_marketplace ON projects
  FOR SELECT
  USING (app_mode() = 'marketplace' AND publication_status = 'PUBLISHED' AND deleted_at IS NULL);

CREATE POLICY project_images_marketplace ON project_images
  FOR SELECT
  USING (
    app_mode() = 'marketplace'
    AND EXISTS (
      SELECT 1 FROM projects p
      WHERE p.id = project_images.project_id
        AND p.publication_status = 'PUBLISHED'
        AND p.deleted_at IS NULL
    )
  );

CREATE POLICY buildings_marketplace ON buildings
  FOR SELECT
  USING (
    app_mode() = 'marketplace'
    AND EXISTS (
      SELECT 1 FROM projects p
      WHERE p.id = buildings.project_id
        AND p.publication_status = 'PUBLISHED'
        AND p.deleted_at IS NULL
    )
  );

CREATE POLICY units_marketplace ON units
  FOR SELECT
  USING (
    app_mode() = 'marketplace'
    AND deleted_at IS NULL
    AND publicly_visible = true
    AND availability <> 'OFF_MARKET'
    AND EXISTS (
      SELECT 1 FROM projects p
      WHERE p.id = units.project_id
        AND p.publication_status = 'PUBLISHED'
        AND p.deleted_at IS NULL
    )
  );

DROP FUNCTION app_apply_tenant_rls(regclass);

GRANT USAGE ON SCHEMA public TO inmobiliaria_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO inmobiliaria_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO inmobiliaria_app;
GRANT EXECUTE ON FUNCTION app_current_tenant() TO inmobiliaria_app;
GRANT EXECUTE ON FUNCTION app_current_user() TO inmobiliaria_app;
GRANT EXECUTE ON FUNCTION app_bypass() TO inmobiliaria_app;
CREATE FUNCTION app_user_memberships(p_user uuid)
RETURNS TABLE (
  id uuid,
  company_id uuid,
  role varchar,
  status varchar,
  company_name varchar,
  company_slug varchar,
  company_status varchar,
  has_logo boolean
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT m.id, m.company_id, m.role::varchar, m.status::varchar, c.commercial_name, c.slug, c.status::varchar,
         c.logo_object_key IS NOT NULL
  FROM memberships m
  JOIN companies c ON c.id = m.company_id
  WHERE m.user_id = p_user AND m.status = 'ACTIVE' AND c.deleted_at IS NULL
$$;

REVOKE ALL ON FUNCTION app_user_memberships(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app_user_memberships(uuid) TO inmobiliaria_app;
GRANT EXECUTE ON FUNCTION app_mode() TO inmobiliaria_app;
