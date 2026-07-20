-- Google Places IDs an admin has hidden from the health-centers page's live search
-- supplement (see HealthCenterService/HealthCenterController). Curated (admin-entered)
-- centers are already soft-deletable via health_center.is_active; this table exists
-- because live Places results have no equivalent local row to flag.
CREATE TABLE health_center_blacklist (
    id BIGSERIAL PRIMARY KEY,
    place_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
