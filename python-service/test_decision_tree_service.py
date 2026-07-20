"""Contract tests for the decision-tree question-sequencing service.

These run against the real app with the real model artifact
(kenphia_grounded_question_tree_model.joblib), so they catch model-loading regressions as
well as API-shape drift that would break the Spring backend's DecisionTreeClient.
"""
from fastapi.testclient import TestClient

from decision_tree_service import app, questionnaire

client = TestClient(app)


def test_health_reports_model_loaded():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "healthy"
    assert body["model_loaded"] is True


def test_all_questions_listed():
    response = client.get("/questions/all")
    assert response.status_code == 200


def test_next_question_from_empty_answers():
    available = list(questionnaire.all_questions)
    response = client.post(
        "/question/next",
        json={"current_answers": {}, "available_questions": available},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["next_question"] in available
    assert body["progress"]["answered"] == 0
    assert isinstance(body["enough_info"], bool)
    assert body["model_version"].startswith("decision-tree")


def test_next_question_excludes_already_answered():
    available = list(questionnaire.all_questions)
    first = client.post(
        "/question/next",
        json={"current_answers": {}, "available_questions": available},
    ).json()["next_question"]

    followup = client.post(
        "/question/next",
        json={"current_answers": {first: "yes"}, "available_questions": available},
    )
    assert followup.status_code == 200
    body = followup.json()
    assert body["success"] is True
    assert body["next_question"] != first
    assert first not in body["remaining_questions"]


def test_next_question_rejects_malformed_body():
    response = client.post("/question/next", json={"current_answers": {}})
    assert response.status_code == 422


def test_model_info_endpoint():
    assert client.get("/model/info").status_code == 200
