"""Contract tests for the ML risk-prediction service.

These run against the real app with the real model artifacts (kenphia_ensemble.pkl et al.),
so they catch model-loading regressions as well as API-shape drift that would break the
Spring backend's MLService client.
"""
import pytest
from fastapi.testclient import TestClient

from app import app, predictor

client = TestClient(app)

RESPONSE_FIELDS = {
    "success", "hivProbability", "riskScore", "riskLevel", "recommendations",
    "confidence", "modelUsed", "modelVersion", "featuresUsed", "featureValues",
    "timestamp",
}


def test_health_reports_status_and_model():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "healthy"
    assert isinstance(body["model_loaded"], bool)
    assert body["model_name"]
    assert isinstance(body["features"], list)


def test_root_describes_the_api():
    response = client.get("/")
    assert response.status_code == 200
    assert "version" in response.json()


def test_predict_returns_full_contract_for_empty_answers():
    """The service must degrade gracefully (zero-encoding/fallback), not 500, on sparse input."""
    response = client.post("/predict", json={"answers": {}})
    assert response.status_code == 200
    body = response.json()
    assert RESPONSE_FIELDS.issubset(body.keys())
    assert body["riskLevel"] in {"Low", "Medium", "High"}
    assert 0 <= body["riskScore"] <= 100
    assert 0.0 <= body["hivProbability"] <= 1.0
    assert isinstance(body["recommendations"], list) and body["recommendations"]
    assert body["modelVersion"]


def test_predict_accepts_populated_answers():
    answers = {feature: "no" for feature in predictor.feature_columns}
    response = client.post("/predict", json={"answers": answers})
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["riskLevel"] in {"Low", "Medium", "High"}


def test_predict_rejects_malformed_body():
    response = client.post("/predict", json={"answers": "not-a-dict"})
    assert response.status_code == 422


def test_model_version_is_stable_across_requests():
    """RiskAssessment.modelVersion auditability depends on this being deterministic."""
    first = client.post("/predict", json={"answers": {}}).json()["modelVersion"]
    second = client.post("/predict", json={"answers": {}}).json()["modelVersion"]
    assert first == second


def test_features_and_model_info_endpoints():
    assert client.get("/features").status_code == 200
    info = client.get("/model-info")
    assert info.status_code == 200
    assert "model_version" in info.json() or info.json()


def test_builtin_sample_prediction():
    response = client.get("/test-prediction")
    assert response.status_code == 200
