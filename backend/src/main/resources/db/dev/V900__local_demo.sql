-- Datos de demostración solo para el perfil local. Contraseña de todas las cuentas: AlzadaLocal1!

INSERT INTO users (id, email, password_hash, full_name, phone, status, platform_admin, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000001', 'admin@alzada.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Administración Alzada', '809-555-0100', 'ACTIVE', true, now(), now()),
  ('00000000-0000-4000-8000-000000000011', 'maria.gomez@altamar.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'María Gómez', '809-555-1101', 'ACTIVE', false, now(), now()),
  ('00000000-0000-4000-8000-000000000012', 'pedro.santos@altamar.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Pedro Santos', '809-555-1102', 'ACTIVE', false, now(), now()),
  ('00000000-0000-4000-8000-000000000013', 'laura.diaz@altamar.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Laura Díaz', '809-555-1103', 'ACTIVE', false, now(), now()),
  ('00000000-0000-4000-8000-000000000014', 'juan.perez@correo.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Juan Pérez', '809-555-1104', 'ACTIVE', false, now(), now()),
  ('00000000-0000-4000-8000-000000000021', 'carlos.mejia@horizonte.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Carlos Mejía', '809-555-2101', 'ACTIVE', false, now(), now()),
  ('00000000-0000-4000-8000-000000000031', 'ana.rios@correo.do', '$2y$12$6Sz5pIMM6RSuQK2SKaqeMu9vM4NvO4xifuj7h4te2NrlojFqBCk42', 'Ana Ríos', '809-555-3101', 'ACTIVE', false, now(), now());

INSERT INTO companies (
  id, slug, commercial_name, legal_name, rnc, phone, email, address, province_code, municipality_id,
  website, description, public_phone, public_email, public_contact, status, search_text, created_at, updated_at
) VALUES
  (
    '00000000-0000-4000-8000-000000000101', 'altamar', 'Altamar Desarrollos', 'Altamar Desarrollos, S.R.L.', '131000001',
    '809-555-1100', 'contacto@altamar.do', 'Avenida Juan Dolio, Bávaro', 'AL',
    (SELECT id FROM municipalities WHERE province_code = 'AL' AND name = 'Higüey'),
    'https://altamar.do', 'Constructora enfocada en vivienda turística y residencial en el este del país.',
    '809-555-1100', 'ventas@altamar.do', 'Sala de ventas en Bávaro, de lunes a sábado de 9:00 a 18:00.',
    'ACTIVE', 'altamar desarrollos higuey la altagracia', now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000102', 'grupo-horizonte', 'Grupo Horizonte', 'Grupo Horizonte, S.A.', '131000002',
    '809-555-2100', 'contacto@horizonte.do', 'Avenida Estrella Sadhalá, Santiago', 'ST',
    (SELECT id FROM municipalities WHERE province_code = 'ST' AND name = 'Santiago de los Caballeros'),
    'https://horizonte.do', 'Desarrollos de casas en Santiago y la región del Cibao.',
    '809-555-2100', 'ventas@horizonte.do', 'Oficina comercial en Santiago.',
    'ACTIVE', 'grupo horizonte santiago', now(), now()
  );

INSERT INTO memberships (id, user_id, company_id, role, status, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000111', '00000000-0000-4000-8000-000000000011', '00000000-0000-4000-8000-000000000101', 'COMPANY_ADMIN', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000112', '00000000-0000-4000-8000-000000000012', '00000000-0000-4000-8000-000000000101', 'SALES_ADVISOR', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000113', '00000000-0000-4000-8000-000000000013', '00000000-0000-4000-8000-000000000101', 'FINANCE', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000114', '00000000-0000-4000-8000-000000000014', '00000000-0000-4000-8000-000000000101', 'BUYER', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000115', '00000000-0000-4000-8000-000000000031', '00000000-0000-4000-8000-000000000101', 'SALES_ADVISOR', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000121', '00000000-0000-4000-8000-000000000021', '00000000-0000-4000-8000-000000000102', 'COMPANY_ADMIN', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000122', '00000000-0000-4000-8000-000000000031', '00000000-0000-4000-8000-000000000102', 'SALES_ADVISOR', 'ACTIVE', now(), now());

INSERT INTO lead_pipeline_stages (id, tenant_id, code, name, sort_order, closed, system_default, created_at, updated_at)
SELECT gen_random_uuid(), company_id, code, name, sort_order, closed, true, now(), now()
FROM (VALUES
  ('00000000-0000-4000-8000-000000000101'::uuid),
  ('00000000-0000-4000-8000-000000000102'::uuid)
) AS companies(company_id)
CROSS JOIN (VALUES
  ('NEW', 'Nuevo', 1, false),
  ('CONTACTED', 'Contactado', 2, false),
  ('VISIT_SCHEDULED', 'Visita coordinada', 3, false),
  ('NEGOTIATION', 'Negociación', 4, false),
  ('RESERVED', 'Reservado', 5, false),
  ('WON', 'Ganado', 6, true),
  ('LOST', 'Perdido', 7, true)
) AS stages(code, name, sort_order, closed);

INSERT INTO projects (
  id, tenant_id, name, slug, description, development_type, province_code, municipality_id, address,
  latitude, longitude, estimated_start, estimated_delivery, status, publication_status, featured,
  amenities, features, commercial_info, internal_notes, min_price, currency, published_at, search_text, created_at, updated_at
) VALUES
  (
    '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000101', 'Torre Brisa', 'torre-brisa',
    'Torre residencial de apartamentos frente al mar en Bávaro, con áreas sociales y estacionamiento.',
    'APARTMENT_BUILDING', 'AL', (SELECT id FROM municipalities WHERE province_code = 'AL' AND name = 'Higüey'),
    'Boulevard Turístico del Este, Bávaro', 18.678000, -68.432000, CURRENT_DATE - 200, CURRENT_DATE + 240,
    'IN_CONSTRUCTION', 'PUBLISHED', true,
    '["Piscina","Gimnasio","Seguridad 24 horas","Área infantil"]',
    '["Planta eléctrica","Gas común","Lobby climatizado"]',
    'Apartamentos desde 2 habitaciones. Inicial y cuotas a convenir en sala de ventas.',
    'Notas internas de Altamar. No se publican.',
    6500000.00, 'DOP', now(), 'torre brisa higuey la altagracia', now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000202', '00000000-0000-4000-8000-000000000101', 'Residencial Palma', 'residencial-palma',
    'Comunidad de casas en planificación. Aún no está publicada en el marketplace.',
    'HOUSE_COMMUNITY', 'AL', (SELECT id FROM municipalities WHERE province_code = 'AL' AND name = 'Higüey'),
    'Verón, Higüey', 18.610000, -68.430000, CURRENT_DATE + 60, CURRENT_DATE + 500,
    'PLANNING', 'DRAFT', false,
    '["Parque lineal","Garita"]', '["Calles de concreto"]',
    'Información comercial pendiente.', 'Borrador interno.',
    8200000.00, 'DOP', NULL, 'residencial palma higuey la altagracia', now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000203', '00000000-0000-4000-8000-000000000102', 'Villas del Norte', 'villas-del-norte',
    'Casas unifamiliares en Santiago, con patio y tres habitaciones.',
    'HOUSE_COMMUNITY', 'ST', (SELECT id FROM municipalities WHERE province_code = 'ST' AND name = 'Santiago de los Caballeros'),
    'Carretera Luperón, Santiago', 19.480000, -70.720000, CURRENT_DATE - 90, CURRENT_DATE + 180,
    'IN_CONSTRUCTION', 'PUBLISHED', true,
    '["Áreas verdes","Cancha","Seguridad"]', '["Tinaco","Cocina modular"]',
    'Casas disponibles para reserva con cita previa.', NULL,
    9800000.00, 'DOP', now(), 'villas del norte santiago', now(), now()
  );

INSERT INTO buildings (id, tenant_id, project_id, name, code, floors, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201', 'Torre A', 'A', 12, now(), now()),
  ('00000000-0000-4000-8000-000000000302', '00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000203', 'Manzana 1', 'M1', 2, now(), now());

INSERT INTO units (
  id, tenant_id, project_id, building_id, code, property_type, bedrooms, bathrooms, area_m2, level_label,
  price, currency, features, availability, publicly_visible, row_version, created_at, updated_at
) VALUES
  ('00000000-0000-4000-8000-000000000401', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000301', 'A-201', 'APARTMENT', 3, 2.0, 128.50, '2', 8500000.00, 'DOP', '["Balcón","Cocina abierta"]', 'SOLD', false, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000402', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000301', 'A-102', 'APARTMENT', 2, 2.0, 96.00, '1', 6900000.00, 'DOP', '["Vista al jardín"]', 'RESERVED', true, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000403', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000301', 'A-101', 'APARTMENT', 2, 2.0, 94.00, '1', 6500000.00, 'DOP', '["Cocina abierta"]', 'AVAILABLE', true, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000404', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000301', 'A-301', 'PENTHOUSE', 3, 3.0, 168.00, '3', 12500000.00, 'DOP', '["Terraza","Family room"]', 'AVAILABLE', true, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000405', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000202', NULL, 'P-01', 'HOUSE', 3, 2.5, 180.00, '1', 8200000.00, 'DOP', '["Patio"]', 'AVAILABLE', false, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000406', '00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000203', '00000000-0000-4000-8000-000000000302', 'H-01', 'HOUSE', 3, 2.5, 160.00, '1', 9800000.00, 'DOP', '["Patio","Marquesina"]', 'AVAILABLE', true, 0, now(), now()),
  ('00000000-0000-4000-8000-000000000407', '00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000203', '00000000-0000-4000-8000-000000000302', 'H-02', 'HOUSE', 3, 2.0, 145.00, '1', 9100000.00, 'DOP', '["Patio"]', 'AVAILABLE', true, 0, now(), now());

INSERT INTO customers (id, tenant_id, user_id, full_name, email, phone, national_id, address, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000501', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000014', 'Juan Pérez', 'juan.perez@correo.do', '809-555-1104', '001-0000000-1', 'Bávaro', now(), now());

INSERT INTO leads (
  id, tenant_id, full_name, email, phone, project_id, unit_id, assigned_user_id, stage_code, source, notes, next_step, next_step_date, created_at, updated_at
) VALUES
  (
    '00000000-0000-4000-8000-000000000601', '00000000-0000-4000-8000-000000000101', 'Carmen Ortiz', 'carmen.ortiz@correo.do', '809-555-2201',
    '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000402', '00000000-0000-4000-8000-000000000012',
    'RESERVED', 'MANUAL', 'Interesada en el apartamento A-102.', 'Confirmar separación', CURRENT_DATE + 1, now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000602', '00000000-0000-4000-8000-000000000101', 'Luis Herrera', 'luis.herrera@correo.do', '809-555-2202',
    '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000403', '00000000-0000-4000-8000-000000000012',
    'CONTACTED', 'MARKETPLACE', 'Solicitó información desde el marketplace.', 'Coordinar visita', CURRENT_DATE + 2, now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000603', '00000000-0000-4000-8000-000000000102', 'Rosa Jiménez', 'rosa.jimenez@correo.do', '809-555-2203',
    '00000000-0000-4000-8000-000000000203', NULL, NULL,
    'NEW', 'MARKETPLACE', 'Consultó Villas del Norte.', 'Llamar', CURRENT_DATE + 1, now(), now()
  );

INSERT INTO lead_activities (id, tenant_id, lead_id, activity_type, description, created_by, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000611', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000602', 'CALL', 'Primera llamada. Pidió ver el apartamento el fin de semana.', '00000000-0000-4000-8000-000000000012', now(), now());

INSERT INTO reservations (
  id, tenant_id, unit_id, lead_id, customer_id, expires_at, amount, currency, status, responsible_user_id, notes, created_at, updated_at
) VALUES (
  '00000000-0000-4000-8000-000000000701', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000402',
  '00000000-0000-4000-8000-000000000601', NULL, now() + interval '20 hours', 150000.00, 'DOP', 'ACTIVE',
  '00000000-0000-4000-8000-000000000012', 'Separación temporal del apartamento A-102.', now(), now()
);

INSERT INTO contracts (
  id, tenant_id, project_id, unit_id, agreed_price, currency, agreement_date, agreed_down_payment, status, notes, created_by, created_at, updated_at
) VALUES (
  '00000000-0000-4000-8000-000000000801', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000201',
  '00000000-0000-4000-8000-000000000401', 8500000.00, 'DOP', CURRENT_DATE - 40, 1700000.00, 'ACTIVE',
  'Precio pactado independiente del precio publicado.', '00000000-0000-4000-8000-000000000013', now(), now()
);

INSERT INTO contract_parties (id, tenant_id, contract_id, customer_id, primary_buyer, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000811', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000801', '00000000-0000-4000-8000-000000000501', true, now(), now());

INSERT INTO payment_plans (
  id, tenant_id, contract_id, total_price, down_payment, installment_count, frequency, currency, delivery_amount, conditions, created_at, updated_at
) VALUES (
  '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000801',
  8500000.00, 1700000.00, 6, 'MONTHLY', 'DOP', 2000000.00,
  'Inicial, seis cuotas mensuales y un pago contra entrega. El calendario es propio de esta operación.', now(), now()
);

INSERT INTO installments (
  id, tenant_id, plan_id, contract_id, sequence_number, kind, label, due_date, amount, paid_amount, status, row_version, created_at, updated_at
) VALUES
  ('00000000-0000-4000-8000-000000000911', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 1, 'DOWN_PAYMENT', 'Inicial', CURRENT_DATE - 40, 1700000.00, 1700000.00, 'PAID', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000912', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 2, 'REGULAR', 'Cuota 1', CURRENT_DATE - 10, 800000.00, 0.00, 'OVERDUE', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000913', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 3, 'REGULAR', 'Cuota 2', CURRENT_DATE + 3, 800000.00, 0.00, 'PENDING', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000914', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 4, 'REGULAR', 'Cuota 3', CURRENT_DATE + 33, 800000.00, 0.00, 'PENDING', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000915', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 5, 'REGULAR', 'Cuota 4', CURRENT_DATE + 63, 800000.00, 0.00, 'PENDING', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000916', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 6, 'REGULAR', 'Cuota 5', CURRENT_DATE + 93, 800000.00, 0.00, 'PENDING', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000917', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 7, 'REGULAR', 'Cuota 6', CURRENT_DATE + 123, 800000.00, 0.00, 'PENDING', 0, now(), now()),
  ('00000000-0000-4000-8000-000000000918', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000901', '00000000-0000-4000-8000-000000000801', 8, 'DELIVERY', 'Contra entrega', CURRENT_DATE + 240, 2000000.00, 0.00, 'PENDING', 0, now(), now());

INSERT INTO payments (
  id, tenant_id, contract_id, amount, currency, status, reported_by, validated_by, validated_at, reference_number, notes, row_version, created_at, updated_at
) VALUES
  (
    '00000000-0000-4000-8000-000000000921', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000801',
    1700000.00, 'DOP', 'VALIDATED', '00000000-0000-4000-8000-000000000013', '00000000-0000-4000-8000-000000000013', now(),
    'TRX-INICIAL-201', 'Inicial recibida en sala de ventas.', 0, now(), now()
  ),
  (
    '00000000-0000-4000-8000-000000000922', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000801',
    200000.00, 'DOP', 'REPORTED', '00000000-0000-4000-8000-000000000014', NULL, NULL,
    'TRX-ABONO-201', 'Abono reportado por el comprador. No aplica a la cuota hasta que finanzas lo confirme.', 0, now(), now()
  );

INSERT INTO payment_allocations (id, tenant_id, payment_id, installment_id, amount, voided, created_at, updated_at) VALUES
  ('00000000-0000-4000-8000-000000000931', '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000921', '00000000-0000-4000-8000-000000000911', 1700000.00, false, now(), now());
