-- Store weekly availability as minutes from midnight (integer) to avoid
-- java.sql.Time timezone shifting on JPA binding.
ALTER TABLE provider_availability
  ALTER COLUMN start_time TYPE integer
    USING (EXTRACT(HOUR FROM start_time) * 60 + EXTRACT(MINUTE FROM start_time))::integer,
  ALTER COLUMN end_time TYPE integer
    USING (EXTRACT(HOUR FROM end_time) * 60 + EXTRACT(MINUTE FROM end_time))::integer;
