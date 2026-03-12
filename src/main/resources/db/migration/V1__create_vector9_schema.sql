CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE subcategories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_category_id BIGINT,
    parent_category_name VARCHAR(255),
    enabled BOOLEAN DEFAULT true,
    include_cars_features BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subcategories_enabled ON subcategories(enabled);
CREATE INDEX idx_subcategories_parent ON subcategories(parent_category_id);

CREATE TABLE owners (
    id UUID PRIMARY KEY,
    login VARCHAR(100) UNIQUE NOT NULL,
    avatar VARCHAR(255),
    created_date VARCHAR(50),
    business_plan VARCHAR(50),
    business_id VARCHAR(50),
    is_verified BOOLEAN DEFAULT false,
    verification_date VARCHAR(50),
    first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_owners_login ON owners(login);
CREATE INDEX idx_owners_verified ON owners(is_verified);

CREATE TABLE ads (
    id BIGINT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    subcategory_id BIGINT NOT NULL REFERENCES subcategories(id),
    price_value DECIMAL(12,2),
    price_unit VARCHAR(10),
    price_measurement VARCHAR(50),
    price_mode VARCHAR(50),
    price_per_meter DECIMAL(12,2),
    old_price_value DECIMAL(12,2),
    body_ro TEXT,
    body_ru TEXT,
    ad_state VARCHAR(50),
    offer_type_id INTEGER,
    offer_type_value INTEGER,
    offer_type_text VARCHAR(100),
    owner_id UUID REFERENCES owners(id),
    transport_year INTEGER,
    real_estate_type VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    last_seen_at TIMESTAMP,
    last_updated_at TIMESTAMP,
    first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details_enriched BOOLEAN DEFAULT false,
    enrichment_status VARCHAR(50) DEFAULT 'pending',
    enrichment_attempts INTEGER DEFAULT 0,
    enrichment_last_attempt_at TIMESTAMP,
    details_last_enriched_at TIMESTAMP,
    views_today INTEGER,
    views_total INTEGER,
    views_since_republish INTEGER,
    views_last_fetched_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ads_subcategory ON ads(subcategory_id);
CREATE INDEX idx_ads_status ON ads(status);
CREATE INDEX idx_ads_last_updated ON ads(last_updated_at);
CREATE INDEX idx_ads_owner ON ads(owner_id);
CREATE INDEX idx_ads_details_enriched ON ads(details_enriched);
CREATE INDEX idx_ads_enrichment_status ON ads(enrichment_status);
CREATE INDEX idx_ads_ad_state ON ads(ad_state);

CREATE TABLE ad_images (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    image_filename VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ad_id, position)
);

CREATE INDEX idx_ad_images_ad_id ON ad_images(ad_id);
CREATE UNIQUE INDEX idx_ad_images_ad_id_filename ON ad_images(ad_id, image_filename);

CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    old_price DECIMAL(12,2),
    new_price DECIMAL(12,2),
    price_unit VARCHAR(10),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_history_ad ON price_history(ad_id);
CREATE INDEX idx_price_history_date ON price_history(changed_at);

CREATE TABLE crawl_checkpoints (
    subcategory_id BIGINT PRIMARY KEY REFERENCES subcategories(id),
    current_skip INTEGER DEFAULT 0,
    last_checkpoint_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_ads_count INTEGER,
    ads_processed INTEGER DEFAULT 0
);

CREATE TABLE car_features (
    ad_id BIGINT PRIMARY KEY REFERENCES ads(id) ON DELETE CASCADE,
    fuel_type VARCHAR(50),
    drive_type VARCHAR(50),
    transmission VARCHAR(50),
    mileage INTEGER,
    engine_volume DECIMAL(4,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ad_attributes (
    ad_id BIGINT PRIMARY KEY REFERENCES ads(id) ON DELETE CASCADE,
    characteristics JSONB NOT NULL,
    location JSONB,
    contact_info JSONB,
    enriched_at TIMESTAMP NOT NULL,
    scrape_duration_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE view_history (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    views_total INTEGER NOT NULL,
    views_since_republish INTEGER NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_view_history_ad ON view_history(ad_id);
CREATE INDEX idx_view_history_recorded_at ON view_history(recorded_at);