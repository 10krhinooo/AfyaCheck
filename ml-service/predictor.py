# HIV risk prediction model: loading, feature encoding, and inference.
# Split out of app.py, which now only wires this up behind FastAPI routes.
import json
import logging
import os
from typing import Any, Dict, List

import numpy as np

logger = logging.getLogger(__name__)


class HIVRiskPredictor:
    def __init__(self, model_path: str = 'xgboost_model.json'):
        self.model = self.load_model(model_path)

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

        # Define categorical value mappings
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
        """Convert form answers to model features with enhanced zero-encoding"""
        features = {}

        # CRITICAL: Check if user is sexually active
        sexual_activity = answers.get('sexual_activity', 'no').lower()
        is_not_sexually_active = sexual_activity == 'no'

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
            logger.info("Zeroing sexual_partners due to no sexual activity")
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
            logger.info(f"Sexual partners: {features['sexual_partners']} (from input: {sexual_partners})")

        # Condom Use Frequency (categorical)
        condom_use = answers.get('condom_use', 'always').lower()
        if is_not_sexually_active:
            features['condom_use'] = 0  # Force lowest risk (always)
            logger.info("Zeroing condom_use due to no sexual activity")
        else:
            features['condom_use'] = self.condom_use_mapping.get(condom_use, 0)
            logger.info(f"Condom use: {features['condom_use']} (from input: {condom_use})")

        # Sexual Activity (categorical)
        features['sexual_activity'] = self.yes_no_mapping.get(sexual_activity, 0)
        logger.info(f"Sexual activity encoded: {features['sexual_activity']}")

        # Recent Partners (numeric/categorical)
        recent_partners = answers.get('recent_partners', '0')
        if is_not_sexually_active:
            features['recent_partners'] = 0
            logger.info("Zeroing recent_partners due to no sexual activity")
        else:
            try:
                features['recent_partners'] = int(recent_partners) if recent_partners.isdigit() else 0
            except ValueError:
                features['recent_partners'] = 0
            logger.info(f"Recent partners: {features['recent_partners']}")

        # High Risk Partner (categorical)
        high_risk_partner = answers.get('high_risk_partner', 'no').lower()
        if is_not_sexually_active:
            features['high_risk_partner'] = 0
            logger.info("Zeroing high_risk_partner due to no sexual activity")
        else:
            features['high_risk_partner'] = self.yes_no_mapping.get(high_risk_partner, 0)
            logger.info(f"High risk partner: {features['high_risk_partner']}")

        # STI Symptoms (categorical)
        sti_symptoms = answers.get('sti_symptoms', 'no').lower()
        if is_not_sexually_active:
            features['sti_symptoms'] = 0
            logger.info("Zeroing sti_symptoms due to no sexual activity")
        else:
            features['sti_symptoms'] = self.yes_no_mapping.get(sti_symptoms, 0)
            logger.info(f"STI symptoms: {features['sti_symptoms']}")

        # Previous STI (categorical)
        previous_sti = answers.get('previous_sti', 'no').lower()
        if is_not_sexually_active:
            features['previous_sti'] = 0
            logger.info("Zeroing previous_sti due to no sexual activity")
        else:
            features['previous_sti'] = self.yes_no_mapping.get(previous_sti, 0)
            logger.info(f"Previous STI: {features['previous_sti']}")

        # Transactional Sex (categorical)
        transactional_sex = answers.get('transactional_sex', 'no').lower()
        if is_not_sexually_active:
            features['transactional_sex'] = 0
            logger.info("Zeroing transactional_sex due to no sexual activity")
        else:
            features['transactional_sex'] = self.yes_no_mapping.get(transactional_sex, 0)
            logger.info(f"Transactional sex: {features['transactional_sex']}")

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
            features = self.preprocess_features(answers)
            features_array = [features]  # Convert to 2D array

            # Check if model has predict_proba method
            if hasattr(self.model, 'predict_proba'):
                prediction = self.model.predict_proba(features_array)[0]
                hiv_probability = float(prediction[1])  # Probability of HIV positive
                confidence = float(np.max(prediction))
                logger.info(f"Model probabilities: {prediction}")
            else:
                # Fallback for models without predict_proba
                prediction = self.model.predict(features_array)[0]
                hiv_probability = float(prediction)
                confidence = 0.85
                logger.info(f"Model raw prediction: {prediction}")

            # CRITICAL FIX: If not sexually active, cap the probability
            is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'
            if is_not_sexually_active:
                original_probability = hiv_probability
                hiv_probability = min(hiv_probability, 0.1)  # Max 10% probability if not sexually active
                logger.info(f"USER NOT SEXUALLY ACTIVE - Capping probability from {original_probability:.3f} to {hiv_probability:.3f}")

            risk_score = int(hiv_probability * 100)

            logger.info(f"FINAL ML PREDICTION - Probability: {hiv_probability:.3f}, Risk Score: {risk_score}, Confidence: {confidence:.3f}")

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
        """Fallback rule-based prediction with enhanced zero-encoding"""
        logger.info("Using enhanced fallback rule-based prediction")

        # CRITICAL: Check if user is sexually active
        is_not_sexually_active = answers.get('sexual_activity', 'no').lower() == 'no'

        if is_not_sexually_active:
            logger.info("USER NOT SEXUALLY ACTIVE - Returning minimal risk score")
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
