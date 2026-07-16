# ml_service_fixed.py (Zero-Encoding Version for Port 8000)
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Optional, Any
import pandas as pd
import numpy as np
from datetime import datetime
import os
import re
import logging
import json
import joblib

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="HIV Risk Prediction API",
    description="ML Service for HIV Risk Prediction with Zero-Encoding",
    version="2.0.0"  # Updated version
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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
    def __init__(self):
        # Primary model: the full KENPHIA 2018 XGBoost+CatBoost soft-voting
        # ensemble (744 one-hot/numeric features). Falls back to the legacy
        # small hand-encoded model if the KENPHIA artifacts aren't present.
        self.kenphia_schema = None
        self.model, self.kenphia_schema = self.load_kenphia_model()

        if self.model is not None:
            self.mode = 'kenphia'
            self.feature_columns = self.kenphia_schema['feature_columns']
            logger.info(f"Loaded KENPHIA ensemble model: {len(self.feature_columns)} features "
                        f"({len(self.kenphia_schema['numeric_cols'])} numeric, "
                        f"{len(self.kenphia_schema['categorical_cols'])} categorical)")
        else:
            self.mode = 'legacy'
            logger.warning("KENPHIA ensemble not found (kenphia_ensemble.pkl / "
                            "kenphia_feature_schema.pkl) -- falling back to legacy model")

            # EXPANDED feature columns to include sexual_activity and other risk factors
            self.feature_columns = [
                'age', 'marital_status', 'education', 'wealth_index',
                'hiv_tested', 'sexual_partners', 'condom_use', 'sexual_activity',
                'recent_partners', 'high_risk_partner', 'sti_symptoms', 'previous_sti', 'transactional_sex'
            ]

            # Try to load feature columns from model info
            try:
                with open('model_info.json', 'r') as f:
                    model_info = json.load(f)
                    self.feature_columns = model_info.get('feature_columns', self.feature_columns)
                    logger.info(f"Loaded feature columns from model_info.json: {self.feature_columns}")
            except Exception as e:
                logger.warning(f"Could not load model_info.json: {e}. Using expanded feature columns.")

            legacy_paths = [
                'xgboost_model.json', 'xgboost_model.joblib',
                'xgboost_non_imputed.pkl', 'model.pkl', 'model.joblib'
            ]
            for path in legacy_paths:
                self.model = self.load_model(path)
                if self.model is not None:
                    logger.info(f"Legacy model loaded from {path}")
                    break

        # Define categorical value mappings -- used both by the legacy
        # preprocess/fallback paths below, and to crosswalk AfyaCheck's
        # answer vocabulary into KENPHIA's numeric codes in
        # build_kenphia_feature_row().
        self.marital_status_mapping = {
            'never_married': 0, 'married': 1, 'divorced': 2, 'widowed': 3, 'separated': 4,
            'single': 0, 'living with partner': 1
        }
        self.education_mapping = {
            'no_education': 0, 'primary': 1, 'secondary': 2, 'higher': 3,
            'no formal education': 0, 'primary school': 1, 'secondary school': 2,
            'high school': 2, 'college/university': 3, 'postgraduate': 3
        }
        self.wealth_index_mapping = {
            'poorest': 0, 'poorer': 1, 'middle': 2, 'richer': 3, 'richest': 4,
            'low income': 0, 'lower middle income': 1, 'middle income': 2,
            'upper middle income': 3, 'high income': 4
        }
        self.hiv_tested_mapping = {'no': 0, 'yes': 1}
        self.sexual_partners_mapping = {'0': 0, '1': 1, '2': 2, '3+': 3}
        self.condom_use_mapping = {'never': 2, 'sometimes': 1, 'always': 0}  # FIXED: Always = lowest risk
        self.yes_no_mapping = {'no': 0, 'yes': 1}

        logger.info(f"HIVRiskPredictor initialized in '{self.mode}' mode")
        logger.info(f"Model loaded: {self.model is not None}")
        logger.info(f"Feature columns: {len(self.feature_columns)}")

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

    def load_kenphia_model(self):
        """Load the full KENPHIA 2018 ensemble + its feature schema, if present."""
        model_path = 'kenphia_ensemble.pkl'
        schema_path = 'kenphia_feature_schema.pkl'
        try:
            if os.path.exists(model_path) and os.path.exists(schema_path):
                model = joblib.load(model_path)
                schema = joblib.load(schema_path)
                return model, schema
        except Exception as e:
            logger.error(f"Failed to load KENPHIA ensemble/schema: {e}")
        return None, None

    def build_kenphia_feature_row(self, answers: Dict[str, str]) -> pd.DataFrame:
        """Build a single-row DataFrame matching the KENPHIA ensemble's exact
        744-column training schema, from AfyaCheck's small answer vocabulary.

        Numeric columns default to NaN (the ensemble's XGBoost/CatBoost
        members handle missing values natively -- this mirrors training,
        where KENPHIA columns AfyaCheck simply has no equivalent for are
        genuinely missing, not zero). Categorical columns default to the
        'MISSING' category used at training time for the same reason.
        Only fields with a confirmed KENPHIA codebook mapping are populated;
        everything else is left as a legitimate missing value rather than
        guessed.
        """
        schema = self.kenphia_schema
        numeric_cols = schema['numeric_cols']
        categorical_cols = schema['categorical_cols']
        feature_columns = schema['feature_columns']

        row = {col: np.nan for col in numeric_cols}

        def get(*keys):
            for k in keys:
                v = answers.get(k)
                if v not in (None, ''):
                    return str(v).strip().lower()
            return None

        age = get('age')
        if age is not None:
            try:
                row['age'] = int(float(age))
            except ValueError:
                pass

        # AfyaCheck's current questionnaire has no gender/urban question --
        # these are only populated if the caller happens to pass extra keys.
        gender = get('gender', 'sex')
        if gender in ('male', 'm', '1'):
            row['gender'] = 1
        elif gender in ('female', 'f', '2'):
            row['gender'] = 2

        urban = get('urban', 'residence', 'area_type')
        if urban in ('urban', '1'):
            row['urban'] = 1
        elif urban in ('rural', '2'):
            row['urban'] = 2

        marital_status = get('marital_status')
        # curmar: 1=MARRIED, 2=LIVING TOGETHER, 3=WIDOWED, 4=DIVORCED, 5=SEPARATED
        curmar_map = {
            'married': 1, 'living with partner': 2, 'living_together': 2,
            'widowed': 3, 'divorced': 4, 'separated': 5,
        }
        if marital_status in curmar_map:
            row['curmar'] = curmar_map[marital_status]
            row['evermar'] = 1  # ever married/lived together = yes
        elif marital_status in ('never_married', 'single'):
            row['evermar'] = 2  # no

        education = get('education')
        # educationkenya: 1=No primary, 2=Incomplete Primary, 3=Complete
        # Primary, 4=Complete Secondary -- there is no "higher education"
        # category in KENPHIA's coding, so AfyaCheck's higher-ed answers
        # collapse (lossily) into 4, the highest available code.
        educationkenya_map = {
            'no_education': 1, 'no formal education': 1,
            'primary': 3, 'primary school': 3,
            'secondary': 4, 'secondary school': 4, 'high school': 4,
            'higher': 4, 'college/university': 4, 'postgraduate': 4,
        }
        if education in educationkenya_map:
            row['educationkenya'] = educationkenya_map[education]

        wealth_index = get('wealth_index')
        # wealthquintile: 1(poorest)-5(richest)
        wealthquintile_map = {
            'poorest': 1, 'poorer': 2, 'middle': 3, 'richer': 4, 'richest': 5,
            'low income': 1, 'lower middle income': 2, 'middle income': 3,
            'upper middle income': 4, 'high income': 5,
        }
        if wealth_index in wealthquintile_map:
            row['wealthquintile'] = wealthquintile_map[wealth_index]

        hiv_tested = get('hiv_tested')
        # evertested / hivtstever: 1=Ever tested/YES, 2=Never tested/NO
        if hiv_tested == 'yes':
            row['evertested'] = 1
            row['hivtstever'] = 1
        elif hiv_tested == 'no':
            row['evertested'] = 2
            row['hivtstever'] = 2

        sexual_activity = get('sexual_activity')
        is_not_sexually_active = sexual_activity == 'no'

        sexual_partners = get('sexual_partners')
        # lifetimesex: integer count, topcoded at 20. AfyaCheck's field isn't
        # explicitly lifetime vs. recent, so this is a best-effort crosswalk.
        if is_not_sexually_active:
            row['lifetimesex'] = 0
        elif sexual_partners is not None:
            if sexual_partners.isdigit():
                row['lifetimesex'] = min(int(sexual_partners), 20)
            elif sexual_partners == '3+':
                row['lifetimesex'] = 20

        condom_use = get('condom_use')
        # condomlastsex12months: 1=used condom at last sex in past 12mo,
        # 2=did not, 3=no sex in past 12mo -- a last-instance indicator, not
        # a frequency scale like AfyaCheck's never/sometimes/always, so
        # 'sometimes' is approximated as "used at last sex".
        if is_not_sexually_active:
            row['condomlastsex12months'] = 3
        elif condom_use in ('always', 'sometimes'):
            row['condomlastsex12months'] = 1
        elif condom_use == 'never':
            row['condomlastsex12months'] = 2

        numeric_row_df = pd.DataFrame([row])

        if categorical_cols:
            cat_row = pd.DataFrame([{col: 'MISSING' for col in categorical_cols}])
            cat_encoded = pd.get_dummies(cat_row, prefix=categorical_cols)
        else:
            cat_encoded = pd.DataFrame(index=numeric_row_df.index)

        full_row = pd.concat([numeric_row_df, cat_encoded], axis=1)
        full_row.columns = [re.sub(r"[\[\]<]", "_", str(c)) for c in full_row.columns]
        full_row = full_row.reindex(columns=feature_columns, fill_value=0)
        return full_row

    def preprocess_features(self, answers: Dict[str, str]) -> List[int]:
        """Convert form answers to model features with enhanced zero-encoding"""
        features = {}

        # CRITICAL: Check if user is sexually active
        sexual_activity = answers.get('sexual_activity', 'no').lower()
        is_not_sexually_active = sexual_activity == 'no'

        # List of sexual risk questions that should be zeroed if not sexually active
        SEXUAL_RISK_KEYS = [
            'sexual_partners', 'recent_partners', 'condom_use', 'high_risk_partner',
            'sti_symptoms', 'previous_sti', 'transactional_sex'
        ]

        logger.info(f"=== ZERO-ENCODING DEBUG ===")
        logger.info(f"User sexual_activity: {sexual_activity}")
        logger.info(f"Is NOT sexually active: {is_not_sexually_active}")

        # Age (numeric)
        age = answers.get('age', '30')
        try:
            features['age'] = int(age) if age.isdigit() else 30
        except ValueError:
            features['age'] = 30
            logger.warning(f"Invalid age value: {age}, using default: 30")

        # Marital Status (categorical)
        marital_status = answers.get('marital_status', 'single').lower()
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

        # --- CRITICAL FIX: Zero out sexual risk factors if not sexually active ---
        # Number of Sexual Partners (categorical) - FIXED FOR NUMERIC VALUES
        sexual_partners = answers.get('sexual_partners', '0').lower()
        if is_not_sexually_active:
            features['sexual_partners'] = 0  # Force zero partners
            logger.info("🔒 ZEROING sexual_partners due to no sexual activity")
        else:
            # FIX: Handle numeric values above the mapping
            if sexual_partners.isdigit():
                partners_count = int(sexual_partners)
                if partners_count >= 3:
                    features['sexual_partners'] = 3  # Map to '3+'
                else:
                    features['sexual_partners'] = partners_count
            else:
                features['sexual_partners'] = self.sexual_partners_mapping.get(sexual_partners, 0)
            logger.info(f"✅ Sexual partners: {features['sexual_partners']} (from input: {sexual_partners})")

        # Condom Use Frequency (categorical)
        condom_use = answers.get('condom_use', 'always').lower()
        if is_not_sexually_active:
            features['condom_use'] = 0  # Force lowest risk (always)
            logger.info("🔒 ZEROING condom_use due to no sexual activity")
        else:
            features['condom_use'] = self.condom_use_mapping.get(condom_use, 0)
            logger.info(f"✅ Condom use: {features['condom_use']} (from input: {condom_use})")

        # Sexual Activity (categorical)
        features['sexual_activity'] = self.yes_no_mapping.get(sexual_activity, 0)
        logger.info(f"✅ Sexual activity encoded: {features['sexual_activity']}")

        # Recent Partners (numeric/categorical)
        recent_partners = answers.get('recent_partners', '0')
        if is_not_sexually_active:
            features['recent_partners'] = 0
            logger.info("🔒 ZEROING recent_partners due to no sexual activity")
        else:
            try:
                features['recent_partners'] = int(recent_partners) if recent_partners.isdigit() else 0
            except ValueError:
                features['recent_partners'] = 0
            logger.info(f"✅ Recent partners: {features['recent_partners']}")

        # High Risk Partner (categorical)
        high_risk_partner = answers.get('high_risk_partner', 'no').lower()
        if is_not_sexually_active:
            features['high_risk_partner'] = 0
            logger.info("🔒 ZEROING high_risk_partner due to no sexual activity")
        else:
            features['high_risk_partner'] = self.yes_no_mapping.get(high_risk_partner, 0)
            logger.info(f"✅ High risk partner: {features['high_risk_partner']}")

        # STI Symptoms (categorical)
        sti_symptoms = answers.get('sti_symptoms', 'no').lower()
        if is_not_sexually_active:
            features['sti_symptoms'] = 0
            logger.info("🔒 ZEROING sti_symptoms due to no sexual activity")
        else:
            features['sti_symptoms'] = self.yes_no_mapping.get(sti_symptoms, 0)
            logger.info(f"✅ STI symptoms: {features['sti_symptoms']}")

        # Previous STI (categorical)
        previous_sti = answers.get('previous_sti', 'no').lower()
        if is_not_sexually_active:
            features['previous_sti'] = 0
            logger.info("🔒 ZEROING previous_sti due to no sexual activity")
        else:
            features['previous_sti'] = self.yes_no_mapping.get(previous_sti, 0)
            logger.info(f"✅ Previous STI: {features['previous_sti']}")

        # Transactional Sex (categorical)
        transactional_sex = answers.get('transactional_sex', 'no').lower()
        if is_not_sexually_active:
            features['transactional_sex'] = 0
            logger.info("🔒 ZEROING transactional_sex due to no sexual activity")
        else:
            features['transactional_sex'] = self.yes_no_mapping.get(transactional_sex, 0)
            logger.info(f"✅ Transactional sex: {features['transactional_sex']}")

        # Ensure all feature columns are present in correct order
        feature_vector = [features.get(col, 0) for col in self.feature_columns]

        logger.info(f"Final feature vector: {feature_vector}")
        logger.info(f"=== END ZERO-ENCODING DEBUG ===")
        return feature_vector

    def predict(self, answers: Dict[str, str]) -> Dict[str, Any]:
        """Make prediction using the ML model with enhanced logic"""
        if self.model is None:
            logger.warning("ML model not available, using fallback prediction")
            return self.fallback_prediction(answers)

        try:
            if self.mode == 'kenphia':
                X = self.build_kenphia_feature_row(answers)
                prediction = self.model.predict_proba(X)[0]
                hiv_probability = float(prediction[1])
                confidence = float(np.max(prediction))
                # How many of the model's numeric fields actually received a
                # real (non-missing) value from this answer set, out of the
                # model's total numeric feature space -- a rough signal of
                # how much of the KENPHIA feature space AfyaCheck's smaller
                # questionnaire is able to populate.
                numeric_cols = self.kenphia_schema['numeric_cols']
                features_used = int(X[numeric_cols].notna().sum(axis=1).iloc[0])
                logger.info(f"KENPHIA model probabilities: {prediction}, "
                            f"numeric features populated: {features_used}/{len(numeric_cols)}")
            elif hasattr(self.model, 'predict_proba'):
                features = self.preprocess_features(answers)
                features_array = [features]  # Convert to 2D array
                prediction = self.model.predict_proba(features_array)[0]
                hiv_probability = float(prediction[1])  # Probability of HIV positive
                confidence = float(np.max(prediction))
                features_used = len(features)
                logger.info(f"Model probabilities: {prediction}")
            else:
                # Fallback for models without predict_proba
                features = self.preprocess_features(answers)
                features_array = [features]
                prediction = self.model.predict(features_array)[0]
                hiv_probability = float(prediction)
                confidence = 0.85
                features_used = len(features)
                logger.info(f"Model raw prediction: {prediction}")

            # CRITICAL FIX: If not sexually active, cap the probability
            is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'
            if is_not_sexually_active:
                original_probability = hiv_probability
                hiv_probability = min(hiv_probability, 0.1)  # Max 10% probability if not sexually active
                logger.info(f"🔒 USER NOT SEXUALLY ACTIVE - Capping probability from {original_probability:.3f} to {hiv_probability:.3f}")

            risk_score = int(hiv_probability * 100)

            logger.info(f"🎯 FINAL ML PREDICTION - Probability: {hiv_probability:.3f}, Risk Score: {risk_score}, Confidence: {confidence:.3f}")

            return {
                'hivProbability': hiv_probability,
                'riskScore': risk_score,
                'confidence': confidence,
                'features_used': features_used,
                'model_used': True
            }
        except Exception as e:
            logger.error(f"Prediction error: {e}")
            return self.fallback_prediction(answers)

    def fallback_prediction(self, answers: Dict[str, str]) -> Dict[str, Any]:
        """Fallback rule-based prediction with enhanced zero-encoding"""
        logger.info("Using enhanced fallback rule-based prediction")

        # CRITICAL: Check if user is sexually active
        is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'

        if is_not_sexually_active:
            logger.info("🔒 USER NOT SEXUALLY ACTIVE - Returning minimal risk score")
            return {
                'hivProbability': 0.05,  # 5% baseline probability
                'riskScore': 5,
                'confidence': 0.90,
                'features_used': 0,
                'model_used': False
            }

        # Only calculate risk if sexually active
        risk_score = 0

        # Simple rule-based scoring (only for sexually active users)
        age = answers.get('age', '30')
        try:
            age_int = int(age)
            if age_int >= 35:
                risk_score += 20
        except ValueError:
            pass

        # FIXED: Handle numeric sexual partners in fallback
        sexual_partners = answers.get('sexual_partners', '1')
        if sexual_partners.isdigit():
            partners_count = int(sexual_partners)
            if partners_count >= 3:
                risk_score += 30
            elif partners_count == 2:
                risk_score += 15
        else:
            if sexual_partners == '3+':
                risk_score += 30
            elif sexual_partners == '2':
                risk_score += 15

        condom_use = answers.get('condom_use', 'sometimes')
        if condom_use == 'never':
            risk_score += 25
        elif condom_use == 'always':
            risk_score -= 10  # Reward consistent condom use

        hiv_tested = answers.get('hiv_tested', 'no')
        if hiv_tested == 'no':
            risk_score += 10

        # Additional risk factors
        high_risk_partner = answers.get('high_risk_partner', 'no')
        if high_risk_partner == 'yes':
            risk_score += 20

        sti_symptoms = answers.get('sti_symptoms', 'no')
        if sti_symptoms == 'yes':
            risk_score += 25

        previous_sti = answers.get('previous_sti', 'no')
        if previous_sti == 'yes':
            risk_score += 15

        risk_score = max(0, min(risk_score, 100))  # Ensure between 0-100

        logger.info(f"Fallback prediction - Risk Score: {risk_score}")

        return {
            'hivProbability': risk_score / 100.0,
            'riskScore': risk_score,
            'confidence': 0.75,
            'features_used': 0,
            'model_used': False
        }

# Initialize predictor -- tries the full KENPHIA ensemble first, then falls
# back to legacy model formats/paths, then to rule-based fallback.
logger.info("Initializing HIV Risk Predictor...")
predictor = HIVRiskPredictor()

if predictor.model is not None:
    logger.info(f"✅ Predictor ready in '{predictor.mode}' mode "
                f"({len(predictor.feature_columns)} features)")
else:
    logger.warning("❌ No model could be loaded. Using rule-based fallback only.")

def get_risk_level(score: int) -> str:
    """Determine risk level based on score"""
    if score >= 50:
        return "High"
    elif score >= 25:
        return "Medium"
    else:
        return "Low"

def generate_recommendations(risk_score: int, answers: Dict[str, str]) -> List[str]:
    """Generate recommendations based on risk score and answers"""
    recommendations = []

    # Check if user is sexually active
    is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'

    if is_not_sexually_active:
        recommendations.extend([
            "Low risk profile: No current sexual activity reported",
            "Consider baseline HIV testing for future reference",
            "Regular health check-ups support overall wellness",
            "Open communication with future partners about sexual health"
        ])
        return recommendations

    # Base recommendations based on risk score (only for sexually active users)
    if risk_score >= 50:
        recommendations.extend([
            "High risk detected: Consider immediate HIV testing and counseling",
            "Consult healthcare provider for comprehensive STI screening",
            "Discuss PrEP (Pre-Exposure Prophylaxis) options with your doctor",
            "Regular testing every 3-6 months strongly recommended"
        ])
    elif risk_score >= 25:
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
    if sexual_partners.isdigit():
        if int(sexual_partners) >= 3:
            recommendations.append("Multiple partners increase risk; consider more frequent testing")
    elif sexual_partners == '3+':
        recommendations.append("Multiple partners increase risk; consider more frequent testing")

    hiv_tested = answers.get('hiv_tested', '').lower()
    if hiv_tested == 'no':
        recommendations.append("Getting tested provides important health information and peace of mind")

    return recommendations

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
    if predictor.mode == 'kenphia':
        model_name = "KENPHIA 2018 XGBoost+CatBoost Ensemble"
    elif predictor.model is not None:
        model_name = "XGBoost (Legacy)"
    else:
        model_name = "Rule-Based Fallback"

    return HealthResponse(
        status="healthy",
        model_loaded=predictor.model is not None,
        model_name=model_name,
        features=predictor.feature_columns,
        timestamp=datetime.now().isoformat()
    )

@app.post("/predict", response_model=PredictionResponse, summary="Predict HIV risk")
async def predict(request: PredictionRequest):
    """Predict HIV risk based on user answers with enhanced zero-encoding"""
    try:
        answers = request.answers
        logger.info(f"📥 Received prediction request with answers: {answers}")

        # Log critical sexual activity status
        sexual_activity = answers.get('sexual_activity', 'unknown')
        logger.info(f"🔍 CRITICAL - User sexual_activity: {sexual_activity}")

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

        logger.info(f"✅ PREDICTION COMPLETED - Risk Score: {prediction['riskScore']}, Level: {risk_level}, Model Used: {prediction['model_used']}")
        return response

    except Exception as e:
        logger.error(f"❌ Unexpected error in prediction: {e}")
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
    response = {
        "model_mode": predictor.mode,
        "required_features": (
            "AfyaCheck answer keys accepted (crosswalked into the KENPHIA schema below)"
            if predictor.mode == 'kenphia' else predictor.feature_columns
        ),
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
                "sexual_activity": "no",  # This will zero all sexual risk factors
                "recent_partners": "0",
                "high_risk_partner": "no",
                "sti_symptoms": "no",
                "previous_sti": "no",
                "transactional_sex": "no"
            }
        }
    }

    if predictor.mode == 'kenphia':
        response["kenphia_feature_space"] = {
            "total_features": len(predictor.feature_columns),
            "numeric_features": len(predictor.kenphia_schema['numeric_cols']),
            "categorical_dummy_features": len(predictor.kenphia_schema['categorical_cols']),
            "mapped_kenphia_fields": [
                "age", "gender (not in AfyaCheck questionnaire, defaults missing)",
                "urban (not in AfyaCheck questionnaire, defaults missing)",
                "curmar/evermar (from marital_status)",
                "educationkenya (from education, collapses higher-ed into 'Complete Secondary')",
                "wealthquintile (from wealth_index)",
                "evertested/hivtstever (from hiv_tested)",
                "lifetimesex (from sexual_partners)",
                "condomlastsex12months (from condom_use)"
            ],
            "note": "All other KENPHIA columns are left as genuine missing values, "
                    "not guessed -- the ensemble's tree models handle missingness natively."
        }

    return response

@app.get("/model-info", summary="Get model information")
async def get_model_info():
    """Get information about the loaded ML model"""
    model_info = {
        "model_loaded": predictor.model is not None,
        "model_mode": predictor.mode,
        "model_type": str(type(predictor.model).__name__) if predictor.model else None,
        "feature_columns": predictor.feature_columns if predictor.mode != 'kenphia' else None,
        "feature_count": len(predictor.feature_columns),
        "zero_encoding_enabled": True,
        "max_risk_non_active": "10%",
        "numeric_partners_handling": True,
        "prediction_capability": "probabilities" if hasattr(predictor.model, 'predict_proba') else "binary only" if predictor.model else "none"
    }

    if predictor.mode == 'kenphia':
        model_info["ensemble_members"] = (
            [name for name, _ in predictor.model.estimators]
            if hasattr(predictor.model, 'estimators') else None
        )
        model_info["training_source"] = "KENPHIA 2018 adult dataset (42,452 rows, 28,393 with biomarker results)"

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
        "age": "20",
        "marital_status": "single",
        "education": "college/university",
        "wealth_index": "middle income",
        "hiv_tested": "no",
        "sexual_partners": "12",  # Test numeric value
        "condom_use": "always",
        "sexual_activity": "no",  # This should trigger zero-encoding
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
        port=8000,  # This matches your Java ML service configuration
        log_level="info",
        reload=True
    )