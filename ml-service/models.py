# Pydantic request/response schemas for the HIV risk prediction API.
from pydantic import BaseModel
from typing import Dict, List, Optional


class PredictionRequest(BaseModel):
    answers: Dict[str, str]


class PredictionResponse(BaseModel):
    success: bool
    hivProbability: float
    riskScore: int
    riskLevel: str
    recommendations: List[str]
    confidence: float
    modelUsed: bool
    featuresUsed: int
    featureValues: Dict[str, str]
    timestamp: str
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: str
    features: List[str]
    timestamp: str
