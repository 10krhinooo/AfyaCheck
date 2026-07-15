# ml_service_fixed.py (Zero-Encoding Version for Port 8000)
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime
import logging

from models import PredictionRequest, PredictionResponse, HealthResponse
from predictor import HIVRiskPredictor
from recommendations import get_risk_level, generate_recommendations

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="HIV Risk Prediction API",
    description="ML Service for HIV Risk Prediction with Zero-Encoding",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize predictor - try multiple model formats and paths
predictor = None
model_paths = [
    'xgboost_model.json',
    'xgboost_model.joblib',
    'xgboost_non_imputed.pkl',
    'model.pkl',
    'model.joblib'
]

for model_path in model_paths:
    logger.info(f"Attempting to load model from: {model_path}")
    predictor = HIVRiskPredictor(model_path)
    if predictor.model is not None:
        logger.info(f"Successfully loaded model from: {model_path}")
        break
    else:
        logger.warning(f"Failed to load model from: {model_path}")

if predictor is None or predictor.model is None:
    logger.warning("No model could be loaded. Using rule-based fallback only.")
    predictor = HIVRiskPredictor()
    predictor.model = None


@app.get("/", summary="Root endpoint")
async def root():
    """Root endpoint with API information"""
    model_status = "loaded" if predictor.model is not None else "fallback only"
    return {
        "message": "HIV Risk Prediction API v2.0 (Enhanced Zero-Encoding)",
        "version": "2.0.0",
        "model_status": model_status,
        "enhanced_features": "Zero-encoding for non-sexually active users",
        "documentation": "/docs",
        "health_check": "/health"
    }


@app.get("/health", response_model=HealthResponse, summary="Health check")
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy",
        model_loaded=predictor.model is not None,
        model_name="XGBoost (Enhanced)" if predictor.model is not None else "Rule-Based Fallback",
        features=predictor.feature_columns,
        timestamp=datetime.now().isoformat()
    )


@app.post("/predict", response_model=PredictionResponse, summary="Predict HIV risk")
async def predict(request: PredictionRequest):
    """Predict HIV risk based on user answers with enhanced zero-encoding"""
    try:
        answers = request.answers
        logger.info(f"Received prediction request with answers: {answers}")

        sexual_activity = answers.get('sexual_activity', 'unknown')
        logger.info(f"CRITICAL - User sexual_activity: {sexual_activity}")

        prediction = predictor.predict(answers)

        recommendations = generate_recommendations(prediction['riskScore'], answers)
        risk_level = get_risk_level(prediction['riskScore'])

        response = PredictionResponse(
            success=True,
            hivProbability=prediction['hivProbability'],
            riskScore=prediction['riskScore'],
            riskLevel=risk_level,
            recommendations=recommendations,
            confidence=prediction['confidence'],
            modelUsed=prediction['model_used'],
            featuresUsed=prediction['features_used'],
            featureValues=answers,
            timestamp=datetime.now().isoformat()
        )

        logger.info(f"PREDICTION COMPLETED - Risk Score: {prediction['riskScore']}, Level: {risk_level}, Model Used: {prediction['model_used']}")
        return response

    except Exception as e:
        logger.error(f"Unexpected error in prediction: {e}")
        return PredictionResponse(
            success=False,
            error=f"Internal server error: {str(e)}",
            timestamp=datetime.now().isoformat(),
            hivProbability=0.0,
            riskScore=0,
            riskLevel="Low",
            recommendations=[],
            confidence=0.0,
            modelUsed=False,
            featuresUsed=0,
            featureValues={}
        )


@app.get("/features", summary="Get feature information")
async def get_features():
    """Endpoint to describe required features and their expected values"""
    return {
        "required_features": predictor.feature_columns,
        "value_mappings": {
            "marital_status": predictor.marital_status_mapping,
            "education": predictor.education_mapping,
            "wealth_index": predictor.wealth_index_mapping,
            "hiv_tested": predictor.hiv_tested_mapping,
            "sexual_partners": predictor.sexual_partners_mapping,
            "condom_use": predictor.condom_use_mapping
        },
        "age_range": "15-65 (typical survey range)",
        "zero_encoding": "All sexual risk factors are zeroed when sexual_activity='no'",
        "numeric_partners": "Numeric values for sexual_partners are properly handled (1, 2, 3+ for >=3)",
        "example_request": {
            "answers": {
                "age": "35",
                "marital_status": "married",
                "education": "secondary",
                "wealth_index": "middle",
                "hiv_tested": "no",
                "sexual_partners": "2",
                "condom_use": "sometimes",
                "sexual_activity": "no",
                "recent_partners": "0",
                "high_risk_partner": "no",
                "sti_symptoms": "no",
                "previous_sti": "no",
                "transactional_sex": "no"
            }
        }
    }


@app.get("/model-info", summary="Get model information")
async def get_model_info():
    """Get information about the loaded ML model"""
    model_info = {
        "model_loaded": predictor.model is not None,
        "model_type": str(type(predictor.model).__name__) if predictor.model else None,
        "feature_columns": predictor.feature_columns,
        "feature_count": len(predictor.feature_columns),
        "zero_encoding_enabled": True,
        "max_risk_non_active": "10%",
        "numeric_partners_handling": True,
        "prediction_capability": "probabilities" if hasattr(predictor.model, 'predict_proba') else "binary only" if predictor.model else "none"
    }

    if predictor.model:
        try:
            if hasattr(predictor.model, 'n_estimators'):
                model_info['n_estimators'] = predictor.model.n_estimators
            if hasattr(predictor.model, 'max_depth'):
                model_info['max_depth'] = predictor.model.max_depth
            if hasattr(predictor.model, 'get_params'):
                params = predictor.model.get_params()
                relevant_params = {k: v for k, v in params.items()
                                   if any(keyword in k for keyword in ['n_estimators', 'max_depth', 'learning_rate', 'random_state'])}
                model_info['parameters'] = relevant_params
        except Exception as e:
            logger.warning(f"Could not extract model parameters: {e}")

    return model_info


@app.get("/test-prediction", summary="Test prediction with sample data")
async def test_prediction():
    """Test endpoint with sample data to verify the API is working"""
    sample_answers = {
        "age": "20",
        "marital_status": "single",
        "education": "college/university",
        "wealth_index": "middle income",
        "hiv_tested": "no",
        "sexual_partners": "12",
        "condom_use": "always",
        "sexual_activity": "no",
        "recent_partners": "0",
        "high_risk_partner": "no",
        "sti_symptoms": "no",
        "previous_sti": "no",
        "transactional_sex": "no"
    }

    try:
        prediction = predictor.predict(sample_answers)
        recommendations = generate_recommendations(prediction['riskScore'], sample_answers)
        risk_level = get_risk_level(prediction['riskScore'])

        return {
            "success": True,
            "model_loaded": predictor.model is not None,
            "test_data": sample_answers,
            "prediction": prediction,
            "risk_level": risk_level,
            "recommendations": recommendations,
            "timestamp": datetime.now().isoformat()
        }
    except Exception as e:
        return {
            "success": False,
            "model_loaded": predictor.model is not None,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }


if __name__ == "__main__":
    import uvicorn

    # Development server - runs on port 8000 to match Java configuration
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info",
        reload=True
    )
