-- Traces which model produced each risk assessment (ml-service's HIVRiskPredictor.model_version,
-- or a rule-based-fallback marker), important for auditability on a health-risk product.
-- Nullable: assessments recorded before this column existed have no captured version.
ALTER TABLE risk_assessment ADD COLUMN model_version text;
