-- Identity moves to Keycloak (Phase 7 of the React migration). Existing "users" rows stay
-- (referenced by questionnaire/password_reset_tokens/verification_token FKs), but going
-- forward a row is looked up/created by Keycloak's JWT "sub" claim rather than by password
-- credentials. keycloak_id is nullable so pre-migration rows (created before the Keycloak
-- cutover, or via the still-present UserService.register() path) remain valid.
ALTER TABLE users ADD COLUMN keycloak_id character varying(255);
CREATE UNIQUE INDEX users_keycloak_id_key ON users (keycloak_id) WHERE keycloak_id IS NOT NULL;

ALTER TABLE users DROP CONSTRAINT users_provider_check;
ALTER TABLE users ADD CONSTRAINT users_provider_check
    CHECK (provider IN ('LOCAL', 'GOOGLE', 'GITHUB', 'APPLE', 'KEYCLOAK'));
