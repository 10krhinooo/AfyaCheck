from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Optional, Any
import pandas as pd
import numpy as np
from datetime import datetime
import os
import logging
import json

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="HIV Risk Prediction API",
    description="ML Service for HIV Risk Prediction",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Adjust in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

class HIVRiskPredictor:
    def __init__(self, model_path: str = 'xgboost_model.json'):
        self.model = self.load_model(model_path)
        self.feature_columns = [
            'age', 'marital_status', 'education', 'wealth_index',
            'hiv_tested', 'sexual_partners', 'condom_use'
        ]

        # Try to load feature columns from model info
        try:
            with open('model_info.json', 'r') as f:
                model_info = json.load(f)
                self.feature_columns = model_info.get('feature_columns', self.feature_columns)
                logger.info(f"Loaded feature columns from model_info.json: {self.feature_columns}")
        except Exception as e:
            logger.warning(f"Could not load model_info.json: {e}. Using default feature columns.")

        # Define categorical value mappings
        self.marital_status_mapping = {
            'never_married': 0, 'married': 1, 'divorced': 2, 'widowed': 3, 'separated': 4
        }
        self.education_mapping = {
            'no_education': 0, 'primary': 1, 'secondary': 2, 'higher': 3
        }
        self.wealth_index_mapping = {
            'poorest': 0, 'poorer': 1, 'middle': 2, 'richer': 3, 'richest': 4
        }
        self.hiv_tested_mapping = {'no': 0, 'yes': 1}
        self.sexual_partners_mapping = {'0': 0, '1': 1, '2': 2, '3+': 3}
        self.condom_use_mapping = {'never': 0, 'sometimes': 1, 'always': 2}

        logger.info(f"HIVRiskPredictor initialized with model path: {model_path}")
        logger.info(f"Model loaded: {self.model is not None}")
        logger.info(f"Feature columns: {self.feature_columns}")

    def load_model(self, model_path: str):
        """Load the trained ML model using multiple formats"""
        try:
            # Try XGBoost native format first
            if model_path.endswith('.json') and os.path.exists(model_path):
                try:
                    import xgboost as xgb
                    model = xgb.XGBClassifier()
                    model.load_model(model_path)
                    logger.info(f"Model loaded successfully from {model_path} using XGBoost native format")
                    return model
                except Exception as e:
                    logger.warning(f"Failed to load as XGBoost native format: {e}")

            # Try joblib format
            joblib_path = model_path.replace('.pkl', '.joblib').replace('.json', '.joblib')
            if os.path.exists(joblib_path):
                try:
                    import joblib
                    model = joblib.load(joblib_path)
                    logger.info(f"Model loaded successfully from {joblib_path} using joblib")
                    return model
                except Exception as e:
                    logger.warning(f"Failed to load as joblib: {e}")

            # Try legacy pickle format
            if os.path.exists(model_path):
                try:
                    import pickle
                    with open(model_path, 'rb') as f:
                        model_data = pickle.load(f)

                    # Handle different pickle formats
                    if isinstance(model_data, dict):
                        if 'model' in model_data:
                            model = model_data['model']
                        elif 'classifier' in model_data:
                            model = model_data['classifier']
                        else:
                            # Take the first value that looks like a model
                            for key, value in model_data.items():
                                if hasattr(value, 'predict') or hasattr(value, 'predict_proba'):
                                    model = value
                                    break
                            else:
                                model = list(model_data.values())[0] if model_data else None
                    else:
                        model = model_data

                    if model is not None:
                        logger.info(f"Legacy model loaded from {model_path}")
                        return model
                    else:
                        logger.error(f"No model found in pickle file: {model_path}")

                except Exception as e:
                    logger.error(f"Error loading legacy model from {model_path}: {e}")

            # Try alternative paths
            alternative_paths = [
                'xgboost_model.json',
                'xgboost_model.joblib',
                'model.joblib',
                'xgboost_non_imputed.pkl',
                'model.pkl'
            ]

            for alt_path in alternative_paths:
                if alt_path != model_path and os.path.exists(alt_path):
                    logger.info(f"Trying alternative path: {alt_path}")
                    model = self.load_model(alt_path)
                    if model is not None:
                        return model

            logger.error("No model file could be loaded from any format")
            return None

        except Exception as e:
            logger.error(f"Error in load_model: {e}")
            return None

    def preprocess_features(self, answers: Dict[str, str]) -> List[int]:
        """Convert form answers to model features"""
        features = {}

        # Age (numeric)
        age = answers.get('age', '30')
        try:
            features['age'] = int(age) if age.isdigit() else 30
        except ValueError:
            features['age'] = 30
            logger.warning(f"Invalid age value: {age}, using default: 30")

        # Marital Status (categorical)
        marital_status = answers.get('marital_status', 'never_married').lower()
        features['marital_status'] = self.marital_status_mapping.get(marital_status, 0)

        # Education Level (categorical)
        education = answers.get('education', 'secondary').lower()
        features['education'] = self.education_mapping.get(education, 2)

        # Wealth Index (categorical)
        wealth_index = answers.get('wealth_index', 'middle').lower()
        features['wealth_index'] = self.wealth_index_mapping.get(wealth_index, 2)

        # HIV Testing History (categorical)
        hiv_tested = answers.get('hiv_tested', 'no').lower()
        features['hiv_tested'] = self.hiv_tested_mapping.get(hiv_tested, 0)

        # Number of Sexual Partners (categorical)
        sexual_partners = answers.get('sexual_partners', '1').lower()
        features['sexual_partners'] = self.sexual_partners_mapping.get(sexual_partners, 1)

        # Condom Use Frequency (categorical)
        condom_use = answers.get('condom_use', 'sometimes').lower()
        features['condom_use'] = self.condom_use_mapping.get(condom_use, 1)

        # Ensure all feature columns are present in correct order
        feature_vector = [features.get(col, 0) for col in self.feature_columns]

        logger.debug(f"Preprocessed features: {feature_vector}")
        return feature_vector

    def predict(self, answers: Dict[str, str]) -> Dict[str, Any]:
        """Make prediction using the ML model"""
        if self.model is None:
            logger.warning("ML model not available, using fallback prediction")
            return self.fallback_prediction(answers)

        try:
            features = self.preprocess_features(answers)
            features_array = [features]  # Convert to 2D array

            # Check if model has predict_proba method
            if hasattr(self.model, 'predict_proba'):
                prediction = self.model.predict_proba(features_array)[0]
                hiv_probability = float(prediction[1])  # Probability of HIV positive
                confidence = float(np.max(prediction))
                logger.debug(f"Using predict_proba - probabilities: {prediction}")
            else:
                # Fallback for models without predict_proba
                prediction = self.model.predict(features_array)[0]
                hiv_probability = float(prediction)
                confidence = 0.85
                logger.debug(f"Using predict - raw prediction: {prediction}")

            risk_score = int(hiv_probability * 100)

            logger.info(f"ML prediction - Probability: {hiv_probability:.3f}, Risk Score: {risk_score}, Confidence: {confidence:.3f}")

            return {
                'hivProbability': hiv_probability,
                'riskScore': risk_score,
                'confidence': confidence,
                'features_used': len(features),
                'model_used': True
            }
        except Exception as e:
            logger.error(f"Prediction error: {e}")
            return self.fallback_prediction(answers)

    def fallback_prediction(self, answers: Dict[str, str]) -> Dict[str, Any]:
        """Fallback rule-based prediction"""
        logger.info("Using fallback rule-based prediction")
        risk_score = 0

        # Simple rule-based scoring
        age = answers.get('age', '30')
        try:
            age_int = int(age)
            if age_int >= 35:
                risk_score += 20
        except ValueError:
            pass

        sexual_partners = answers.get('sexual_partners', '1')
        if sexual_partners == '3+':
            risk_score += 30
        elif sexual_partners == '2':
            risk_score += 15

        condom_use = answers.get('condom_use', 'sometimes')
        if condom_use == 'never':
            risk_score += 25

        hiv_tested = answers.get('hiv_tested', 'no')
        if hiv_tested == 'no':
            risk_score += 10

        risk_score = min(risk_score, 100)

        logger.info(f"Fallback prediction - Risk Score: {risk_score}")

        return {
            'hivProbability': risk_score / 100.0,
            'riskScore': risk_score,
            'confidence': 0.75,
            'features_used': 0,
            'model_used': False
        }

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
        logger.info(f"✅ Successfully loaded model from: {model_path}")
        break
    else:
        logger.warning(f"❌ Failed to load model from: {model_path}")

if predictor is None or predictor.model is None:
    logger.warning("❌ No model could be loaded. Using rule-based fallback only.")
    # Create predictor with no model for fallback only
    predictor = HIVRiskPredictor()
    predictor.model = None

def get_risk_level(score: int) -> str:
    """Determine risk level based on score"""
    if score >= 15:
        return "High"
    elif score >= 8:
        return "Medium"
    else:
        return "Low"

def generate_recommendations(risk_score: int, answers: Dict[str, str]) -> List[str]:
    """Generate recommendations based on risk score and answers"""
    recommendations = []

    # Base recommendations based on risk score
    if risk_score >=50:
        recommendations.extend([
            "High risk detected: Consider immediate HIV testing and counseling",
            "Consult healthcare provider for comprehensive STI screening",
            "Discuss PrEP (Pre-Exposure Prophylaxis) options with your doctor",
            "Regular testing every 3-6 months strongly recommended"
        ])
    elif risk_score >=200:
        recommendations.extend([
            "Moderate risk: Schedule HIV testing at your earliest convenience",
            "Consider routine STI screening during next healthcare visit",
            "Practice consistent condom use to reduce transmission risk",
            "Annual HIV testing recommended while sexually active"
        ])
    else:
        recommendations.extend([
            "Low risk: Maintain current protective behaviors",
            "Consider baseline HIV testing for peace of mind",
            "Regular health check-ups support overall wellness",
            "Open communication with partners about sexual health"
        ])

    # Context-specific recommendations based on individual factors
    condom_use = answers.get('condom_use', '').lower()
    if condom_use == 'never':
        recommendations.append("Consistent condom use can significantly reduce HIV transmission risk")

    sexual_partners = answers.get('sexual_partners', '').lower()
    if sexual_partners == '3+':
        recommendations.append("Multiple partners increase risk; consider more frequent testing")

    hiv_tested = answers.get('hiv_tested', '').lower()
    if hiv_tested == 'no':
        recommendations.append("Getting tested provides important health information and peace of mind")

    age = answers.get('age', '30')
    try:
        age_int = int(age)
        if age_int >= 35:
            recommendations.append("Older age groups may benefit from regular screening as part of routine healthcare")
    except ValueError:
        pass

    return recommendations

@app.get("/", summary="Root endpoint")
async def root():
    """Root endpoint with API information"""
    model_status = "loaded" if predictor.model is not None else "fallback only"
    return {
        "message": "HIV Risk Prediction API",
        "version": "1.0.0",
        "model_status": model_status,
        "documentation": "/docs",
        "health_check": "/health"
    }

@app.get("/health", response_model=HealthResponse, summary="Health check")
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy",
        model_loaded=predictor.model is not None,
        model_name="XGBoost (Non-Imputed)" if predictor.model is not None else "Rule-Based Fallback",
        features=predictor.feature_columns,
        timestamp=datetime.now().isoformat()
    )

@app.post("/predict", response_model=PredictionResponse, summary="Predict HIV risk")
async def predict(request: PredictionRequest):
    """Predict HIV risk based on user answers"""
    try:
        answers = request.answers
        logger.info(f"Received prediction request with answers: {answers}")

        # Validate required fields
        required_fields = [
            'age', 'marital_status', 'education', 'wealth_index',
            'hiv_tested', 'sexual_partners', 'condom_use'
        ]

        missing_fields = [field for field in required_fields if field not in answers]
        if missing_fields:
            logger.warning(f"Missing required fields: {missing_fields}")
            return PredictionResponse(
                success=False,
                error=f'Missing required fields: {missing_fields}',
                timestamp=datetime.now().isoformat(),
                # Provide default values for required fields
                hivProbability=0.0,
                riskScore=0,
                riskLevel="Low",
                recommendations=[],
                confidence=0.0,
                modelUsed=False,
                featuresUsed=0,
                featureValues={}
            )

        # Make prediction
        prediction = predictor.predict(answers)

        # Generate recommendations
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

        logger.info(f"Prediction completed - Risk Score: {prediction['riskScore']}, Level: {risk_level}, Model Used: {prediction['model_used']}")
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
        "example_request": {
            "answers": {
                "age": "35",
                "marital_status": "married",
                "education": "secondary",
                "wealth_index": "middle",
                "hiv_tested": "no",
                "sexual_partners": "2",
                "condom_use": "sometimes"
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
        "prediction_capability": "probabilities" if hasattr(predictor.model, 'predict_proba') else "binary only" if predictor.model else "none"
    }

    if predictor.model:
        try:
            # Try to get some model attributes
            if hasattr(predictor.model, 'n_estimators'):
                model_info['n_estimators'] = predictor.model.n_estimators
            if hasattr(predictor.model, 'max_depth'):
                model_info['max_depth'] = predictor.model.max_depth
            if hasattr(predictor.model, 'get_params'):
                params = predictor.model.get_params()
                # Include only relevant parameters
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
        "age": "35",
        "marital_status": "married",
        "education": "secondary",
        "wealth_index": "middle",
        "hiv_tested": "no",
        "sexual_partners": "2",
        "condom_use": "sometimes"
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

@app.get("/debug/model-loading", summary="Debug model loading status")
async def debug_model_loading():
    """Debug endpoint to check model loading status"""
    available_files = []
    for root, dirs, files in os.walk('.'):
        for file in files:
            if any(ext in file for ext in ['.pkl', '.joblib', '.json', '.model']):
                available_files.append(os.path.join(root, file))

    return {
        "model_loaded": predictor.model is not None,
        "model_type": str(type(predictor.model)) if predictor.model else None,
        "feature_columns": predictor.feature_columns,
        "available_model_files": available_files,
        "current_directory": os.getcwd(),
        "timestamp": datetime.now().isoformat()
    }

if __name__ == "__main__":
    import uvicorn

    # Development server
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info",
        reload=True  # Enable auto-reload for development
    )