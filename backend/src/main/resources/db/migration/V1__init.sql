CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE geo_area (
  code        varchar(20) PRIMARY KEY,
  parent_code varchar(20) REFERENCES geo_area(code),
  name        varchar(120) NOT NULL,
  type        varchar(20) NOT NULL CHECK (type IN ('province','district')),
  active      boolean NOT NULL DEFAULT true
);

CREATE TABLE service_category (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name        varchar(120) NOT NULL,
  slug        varchar(120) NOT NULL UNIQUE,
  description text,
  active      boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE service (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id uuid NOT NULL REFERENCES service_category(id),
  name        varchar(120) NOT NULL,
  slug        varchar(120) NOT NULL UNIQUE,
  description text,
  active      boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_account (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email         varchar(255) NOT NULL,
  email_norm    varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  role          varchar(20) NOT NULL CHECK (role IN ('CUSTOMER','PROVIDER','ADMIN')),
  status        varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
  version       bigint NOT NULL DEFAULT 0,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE refresh_token (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_account_id uuid NOT NULL REFERENCES user_account(id),
  token_hash      varchar(255) NOT NULL UNIQUE,
  expires_at      timestamptz NOT NULL,
  revoked_at      timestamptz,
  replaced_by     uuid,
  created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE customer_profile (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_account_id uuid NOT NULL UNIQUE REFERENCES user_account(id),
  full_name       varchar(120) NOT NULL,
  phone           varchar(40),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE customer_address (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id uuid NOT NULL REFERENCES customer_profile(id),
  label       varchar(80) NOT NULL,
  address_line text NOT NULL,
  area_code   varchar(20) NOT NULL REFERENCES geo_area(code),
  is_default  boolean NOT NULL DEFAULT false,
  deleted_at  timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE provider_profile (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_account_id     uuid NOT NULL UNIQUE REFERENCES user_account(id),
  business_name       varchar(160) NOT NULL,
  description         text,
  experience_summary  text,
  contact_phone       varchar(40),
  verification_status varchar(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (verification_status IN ('DRAFT','PENDING_VERIFICATION','VERIFIED','REJECTED','SUSPENDED')),
  version             bigint NOT NULL DEFAULT 0,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE provider_service (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id uuid NOT NULL REFERENCES provider_profile(id),
  service_id  uuid NOT NULL REFERENCES service(id),
  description text,
  active      boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (provider_id, service_id)
);

CREATE TABLE provider_area (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id uuid NOT NULL REFERENCES provider_profile(id),
  area_code   varchar(20) NOT NULL REFERENCES geo_area(code),
  active      boolean NOT NULL DEFAULT true,
  UNIQUE (provider_id, area_code)
);

CREATE TABLE provider_availability (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id uuid NOT NULL REFERENCES provider_profile(id),
  day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
  start_time  time NOT NULL,
  end_time    time NOT NULL,
  CHECK (start_time < end_time)
);

CREATE TABLE service_request (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id     uuid NOT NULL REFERENCES customer_profile(id),
  status          varchar(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','CONFIRMED','PROVIDERS_FOUND','PROVIDER_SELECTED','CANCELLED')),
  current_version int NOT NULL DEFAULT 0,
  version         bigint NOT NULL DEFAULT 0,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE request_version (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id       uuid NOT NULL REFERENCES service_request(id),
  version          int NOT NULL,
  category_id      uuid NOT NULL REFERENCES service_category(id),
  service_id       uuid NOT NULL REFERENCES service(id),
  description      text NOT NULL,
  address_snapshot text NOT NULL,
  area_code        varchar(20) NOT NULL REFERENCES geo_area(code),
  preferred_start  timestamptz NOT NULL,
  preferred_end    timestamptz NOT NULL,
  budget_amount    numeric(14,0),
  budget_currency  char(3) NOT NULL DEFAULT 'VND',
  created_at       timestamptz NOT NULL DEFAULT now(),
  UNIQUE (request_id, version),
  CHECK (preferred_start < preferred_end)
);

CREATE TABLE discovery_run (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id      uuid NOT NULL REFERENCES service_request(id),
  request_version int NOT NULL,
  executed_at     timestamptz NOT NULL DEFAULT now(),
  no_result_reason varchar(40)
);

CREATE TABLE discovery_candidate (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  discovery_run_id uuid NOT NULL REFERENCES discovery_run(id),
  provider_id      uuid NOT NULL REFERENCES provider_profile(id),
  reason_codes     jsonb,
  UNIQUE (discovery_run_id, provider_id)
);

CREATE TABLE provider_selection (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id      uuid NOT NULL REFERENCES service_request(id),
  request_version int NOT NULL,
  provider_id     uuid NOT NULL REFERENCES provider_profile(id),
  selected_by     uuid NOT NULL REFERENCES user_account(id),
  selected_at     timestamptz NOT NULL DEFAULT now(),
  status          varchar(20) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE','CONVERTED','RELEASED')),
  UNIQUE (request_id, request_version)
);

CREATE TABLE quotation (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  selection_id  uuid NOT NULL REFERENCES provider_selection(id),
  status        varchar(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','SUBMITTED','ACCEPTED','REJECTED','WITHDRAWN','EXPIRED')),
  total         numeric(14,0) NOT NULL CHECK (total >= 0),
  currency      char(3) NOT NULL DEFAULT 'VND',
  service_start timestamptz NOT NULL,
  service_end   timestamptz NOT NULL,
  valid_until   timestamptz,
  version       bigint NOT NULL DEFAULT 0,
  created_at    timestamptz NOT NULL DEFAULT now(),
  CHECK (service_start < service_end)
);
CREATE UNIQUE INDEX uq_quote_active_selection ON quotation(selection_id)
  WHERE status='SUBMITTED';

CREATE TABLE quotation_item (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_id uuid NOT NULL REFERENCES quotation(id),
  description  text NOT NULL,
  amount       numeric(14,0) NOT NULL CHECK (amount >= 0),
  currency     char(3) NOT NULL DEFAULT 'VND'
);

CREATE TABLE booking (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_id uuid NOT NULL UNIQUE REFERENCES quotation(id),
  customer_id  uuid NOT NULL REFERENCES customer_profile(id),
  provider_id  uuid NOT NULL REFERENCES provider_profile(id),
  status       varchar(20) NOT NULL DEFAULT 'CONFIRMED'
    CHECK (status IN ('CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')),
  start_at     timestamptz NOT NULL,
  end_at       timestamptz NOT NULL,
  version      bigint NOT NULL DEFAULT 0,
  created_at   timestamptz NOT NULL DEFAULT now(),
  CHECK (start_at < end_at)
);

CREATE TABLE provider_slot_reservation (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id uuid NOT NULL REFERENCES provider_profile(id),
  booking_id  uuid NOT NULL UNIQUE REFERENCES booking(id),
  start_at    timestamptz NOT NULL,
  end_at      timestamptz NOT NULL,
  status      varchar(20) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE','RELEASED')),
  CHECK (start_at < end_at)
);
ALTER TABLE provider_slot_reservation ADD CONSTRAINT ex_provider_active_slot
  EXCLUDE USING gist (provider_id WITH =, tstzrange(start_at,end_at,'[)') WITH &&)
  WHERE (status='ACTIVE');

CREATE TABLE payment_record (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id   uuid NOT NULL REFERENCES booking(id),
  adapter      varchar(50) NOT NULL,
  external_ref varchar(120),
  status       varchar(20) NOT NULL CHECK (status IN ('PENDING','RECORDED','FAILED')),
  amount       numeric(14,0) NOT NULL,
  currency     char(3) NOT NULL DEFAULT 'VND',
  created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_external ON payment_record(adapter,external_ref)
  WHERE external_ref IS NOT NULL;
CREATE UNIQUE INDEX uq_booking_recorded_payment ON payment_record(booking_id)
  WHERE status='RECORDED';

CREATE TABLE review (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id  uuid NOT NULL UNIQUE REFERENCES booking(id),
  customer_id uuid NOT NULL REFERENCES customer_profile(id),
  provider_id uuid NOT NULL REFERENCES provider_profile(id),
  rating      smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     varchar(1000),
  status      varchar(20) NOT NULL DEFAULT 'PUBLISHED'
    CHECK (status IN ('PUBLISHED','HIDDEN')),
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_event (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id      uuid REFERENCES user_account(id),
  actor_role    varchar(20) NOT NULL,
  action        varchar(80) NOT NULL,
  entity_type   varchar(40) NOT NULL,
  entity_id     uuid NOT NULL,
  before_summary jsonb,
  after_summary  jsonb,
  reason        varchar(255),
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_record (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id      uuid NOT NULL REFERENCES user_account(id),
  operation     varchar(80) NOT NULL,
  idem_key      varchar(120) NOT NULL,
  request_hash  varchar(64) NOT NULL,
  status        varchar(20) NOT NULL CHECK (status IN ('NEW','IN_PROGRESS','COMPLETED','CONFLICT')),
  response_code int,
  response_body jsonb,
  created_at    timestamptz NOT NULL DEFAULT now(),
  expires_at    timestamptz NOT NULL,
  UNIQUE (actor_id, operation, idem_key)
);
