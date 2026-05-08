-- V5__add_download_count.sql
-- Dodanie kolumny zliczania pobran talii w Marketplace

ALTER TABLE decks
    ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0;

-- Indeks dla wydajnego sortowania po popularnosci w GET /marketplace
CREATE INDEX idx_decks_download_count ON decks(download_count DESC);