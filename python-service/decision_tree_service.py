# fastapi_production_ready.py (Final Production Version with Enhanced Zero Encoding)
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, List, Optional, Any
import pandas as pd
import numpy as np
import joblib
import logging
from datetime import datetime
import os
from pathlib import Path
from sklearn.preprocessing import LabelEncoder
import warnings
warnings.filterwarnings('ignore', category=UserWarning)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Configuration & Path Management ---

MODEL_DIR = os.getenv("MODEL_DIRECTORY", "decision_tree_model")
MODEL_FILENAME = os.getenv("MODEL_FILENAME", "kenphia_grounded_question_tree_model.joblib")

def get_model_path() -> Path:
    """Constructs the absolute path to the model file."""
    model_dir_path = Path(MODEL_DIR)
    model_dir_path.mkdir(parents=True, exist_ok=True)
    return model_dir_path / MODEL_FILENAME

def model_exists() -> bool:
    """Check if the specific model file exists"""
    return get_model_path().is_file()

# --- FastAPI App Setup ---

app = FastAPI(
    title="Smart Question Sequencing API",
    description="ML Service for Adaptive Question Sequencing with Smart Logic",
    version="2.0.0"
)

# Server-to-server only (called from Spring Boot's DecisionTreeClient, see
# decision.tree.service.url) -- allow_credentials=False since it's never a browser client and
# can't legally pair with a wildcard origin anyway.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Pydantic Schemas ---

class QuestionRequest(BaseModel):
    current_answers: Dict[str, Any]
    available_questions: List[str]

class QuestionResponse(BaseModel):
    success: bool
    next_question: Optional[str] = None
    progress: Dict[str, Any]
    confidence: float
    remaining_questions: List[str]
    timestamp: str
    enough_info: bool = False
    model_version: str = "unknown"
    error: Optional[str] = None

# --- Core Business Logic Class (Integrated Utilities) ---

class SmartQuestionnaire:
    def __init__(self, model_path: str):
        """Initialize with the trained decision tree model and integrated utilities."""
        try:
            self.model_path = model_path
            self.artifacts = joblib.load(model_path)

            self.model = self.artifacts.get('model')
            self.feature_columns = self.artifacts.get('feature_columns', [])
            self.label_encoder = self.artifacts.get('label_encoder')
            self.all_questions = self.artifacts.get('all_questions', [])
            self.questions_df_data = self.artifacts.get('questions_df', [])
            self.best_params = self.artifacts.get('best_params', {})
            self.training_info = self.artifacts.get('training_info', {})
            self.cv_results = self.training_info.get('final_accuracy', 'N/A')

            if self.model is None or self.label_encoder is None:
                raise ValueError("Critical model components (model/encoder) missing.")

            self.questions_df = pd.DataFrame(self.questions_df_data) if self.questions_df_data else pd.DataFrame([
                {'question_key': q, 'display_order': i+1}
                for i, q in enumerate(self.all_questions)
            ])

            # Derived from the loaded artifact's file mtime rather than the bundled
            # training_info (which can lag -- e.g. model_metadata_20251124_113924.json on
            # disk describes an older training run than whatever MODEL_FILENAME currently
            # points at), so this always reflects the model actually in use.
            self.model_version = self._compute_model_version()

            logger.info(f"Smart model loaded: {self.model_path}")
            logger.info(f"Model version: {self.model_version}")

        except Exception as e:
            logger.error(f"Failed to load smart model from {model_path}: {e}")
            raise

    def _compute_model_version(self) -> str:
        try:
            mtime = os.path.getmtime(self.model_path)
            return f"decision-tree-{datetime.fromtimestamp(mtime).strftime('%Y%m%d')}"
        except OSError:
            return "decision-tree-unknown"

    # --- Feature Engineering Utility Functions (From Training Script) ---

    def _get_question_category(self, question_key):
        """Categorize questions for sequence patterns (matches training logic)"""
        demographics = ['consent', 'age', 'gender', 'marital_status', 'education', 'wealth_index']
        sexual_history = ['sexual_activity', 'recent_partners', 'condom_use', 'high_risk_partner',
                          'transactional_sex', 'multiple_partners', 'sexual_partners']
        symptoms = ['sti_symptoms', 'discharge_symptom', 'painful_urination', 'genital_sores',
                    'symptom_duration', 'partner_symptoms']
        testing = ['hiv_tested', 'last_hiv_test', 'other_sti_tests', 'willing_to_test']
        female_health = ['pregnancy_status', 'contraception_use', 'last_pap_smear']
        substance = ['substance_sex', 'alcohol_frequency', 'drug_use']
        relationship = ['sexual_coercion', 'partner_communication', 'partner_testing', 'partner_concurrency']
        knowledge = ['sti_knowledge', 'prevention_methods', 'hiv_prep', 'health_priorities']
        healthcare = ['insurance_coverage', 'regular_provider', 'cost_barrier', 'preferred_testing', 'testing_barriers']

        if question_key in demographics: return 'demographics'
        elif question_key in sexual_history: return 'sexual_history'
        elif question_key in symptoms: return 'symptoms'
        elif question_key in testing: return 'testing'
        elif question_key in female_health: return 'female_health'
        elif question_key in substance: return 'substance'
        elif question_key in relationship: return 'relationship'
        elif question_key in knowledge: return 'knowledge'
        elif question_key in healthcare: return 'healthcare'
        else: return 'other'

    def _get_expected_next_category(self, current_answers):
        """Predict which category should come next (matches training logic)"""
        answered_categories = set(self._get_question_category(q) for q in current_answers.keys())
        category_sequence = ['demographics', 'sexual_history', 'symptoms', 'testing',
                             'female_health', 'substance', 'relationship', 'knowledge', 'healthcare']
        for category in category_sequence:
            if category not in answered_categories:
                return category
        return 'healthcare'

    def _calculate_risk_score(self, answers):
        """Calculate comprehensive risk score (matches training logic)"""
        score = 0.0

        # CRITICAL: If not sexually active, risk should be minimal
        if answers.get('sexual_activity') != 'Yes':
            return 0.0

        if answers.get('sexual_activity') == 'Yes':
            score += 0.3
            if answers.get('condom_use') == 'Never': score += 0.2
            if answers.get('high_risk_partner') == 'Yes': score += 0.2
            if answers.get('multiple_partners') == 'Yes': score += 0.1
            if answers.get('transactional_sex') == 'Yes': score += 0.2
        if answers.get('sti_symptoms') == 'Yes': score += 0.3
        if answers.get('previous_sti') == 'Yes': score += 0.2
        return min(score, 1.0)

    def _encode_answer(self, question: str, answer: Any) -> float:
        """
        Encode answers for the decision tree model (matches training logic).
        FIX: Treats the 'N/A_ML' placeholder as 0.0 (No risk/Unanswered).
        """
        if answer is None or answer == 'N/A':
            return 0.0

        answer_str = str(answer).lower().strip()

        # --- FIX: Treat N/A_ML Placeholder as Unanswered (0.0) ---
        if answer_str == 'n/a_ml':
            return 0.0
        # --------------------------------------------------------

        # Numeric questions
        if question in ['age', 'recent_partners', 'sexual_partners']:
            try:
                value = float(answer)
                if question == 'age':
                    if value < 18: return value / 18.0 * 0.3
                    elif value < 25: return 0.3 + (value - 18) / 7.0 * 0.3
                    elif value < 35: return 0.6 + (value - 25) / 10.0 * 0.2
                    else: return 0.8 + min((value - 35) / 65.0 * 0.2, 0.2)
                else:
                    return min(np.log1p(value) / np.log1p(10), 1.0)
            except:
                return 0.0

        # Boolean-like questions
        yes_no_questions = [
            'sexual_activity', 'high_risk_partner', 'transactional_sex', 'discharge_symptom',
            'painful_urination', 'genital_sores', 'sti_symptoms', 'previous_sti', 'sti_treatment',
            'hiv_tested', 'other_sti_tests', 'willing_to_test', 'pregnancy_status', 'substance_sex',
            'drug_use', 'sexual_coercion', 'partner_communication', 'partner_symptoms',
            'multiple_partners', 'partner_concurrency', 'hiv_prep', 'insurance_coverage',
            'regular_provider', 'cost_barrier'
        ]
        if question in yes_no_questions:
            return 1.0 if answer_str in ['yes', 'y', 'true', '1'] else 0.0

        # Categorical questions
        if question == 'gender':
            return {'male': 0.0, 'female': 1.0, 'other': 0.5}.get(answer_str, 0.0)
        elif question == 'condom_use':
            return {'never': 1.0, 'sometimes': 0.5, 'always': 0.0}.get(answer_str, 0.5)  # FIXED: Always = 0.0 risk
        elif question == 'alcohol_frequency':
            levels = {'never': 0.0, 'monthly or less': 0.2, '2-4 times per month': 0.5, '2-3 times per week': 0.8, '4 or more times per week': 1.0}
            return levels.get(answer_str, 0.5)
        elif question == 'last_hiv_test':
            levels = {'never tested': 0.0, 'more than 1 year ago': 0.3, '3-12 months ago': 0.7, 'within the last 3 months': 1.0}
            return levels.get(answer_str, 0.5)
        elif question == 'marital_status':
            levels = {'single': 0.0, 'living with partner': 0.4, 'separated': 0.5, 'divorced': 0.6, 'widowed': 0.7, 'married': 1.0}
            return levels.get(answer_str, 0.0)
        elif question == 'education':
            levels = {'no formal education': 0.0, 'primary school': 0.2, 'secondary school': 0.4, 'high school': 0.6, 'college/university': 0.8, 'postgraduate': 1.0}
            return levels.get(answer_str, 0.0)
        elif question == 'wealth_index':
            levels = {'low income': 0.0, 'lower middle income': 0.3, 'middle income': 0.6, 'upper middle income': 0.8, 'high income': 1.0}
            return levels.get(answer_str, 0.0)
        elif question == 'sti_knowledge':
            levels = {'not knowledgeable at all': 0.0, 'not very knowledgeable': 0.3, 'somewhat knowledgeable': 0.7, 'very knowledgeable': 1.0}
            return levels.get(answer_str, 0.5)
        elif question == 'health_priorities':
            levels = {'not important at all': 0.0, 'not very important': 0.3, 'somewhat important': 0.7, 'very important': 1.0}
            return levels.get(answer_str, 0.5)
        elif question == 'last_pap_smear':
            levels = {'never': 0.0, 'more than 3 years ago': 0.3, '1-3 years ago': 0.7, 'within the last year': 1.0}
            return levels.get(answer_str, 0.5)

        # Encoding for categories (needed for sequence features)
        elif question == 'category':
            levels = {'demographics': 0.1, 'sexual_history': 0.3, 'symptoms': 0.5, 'testing': 0.7,
                      'female_health': 0.75, 'substance': 0.8, 'relationship': 0.85, 'knowledge': 0.9, 'healthcare': 1.0}
            return levels.get(answer_str, 0.0)

        return 0.5

    # --- Relevance and Logic ---

    def _is_question_relevant(self, question_key: str, current_answers: Dict[str, Any]) -> bool:
        """Check if a question is relevant based on current answers"""
        answers = current_answers

        always_relevant = [
            'consent', 'age', 'gender', 'marital_status', 'education', 'wealth_index',
            'sti_knowledge', 'prevention_methods', 'hiv_prep', 'health_priorities',
            'insurance_coverage', 'regular_provider', 'cost_barrier', 'preferred_testing',
            'testing_barriers', 'alcohol_frequency', 'drug_use', 'hiv_tested',
            'other_sti_tests', 'willing_to_test'
        ]

        if question_key in always_relevant:
            if question_key == 'last_hiv_test': return answers.get('hiv_tested') == 'Yes'
            return True

        if question_key in ['pregnancy_status', 'contraception_use', 'last_pap_smear']:
            return answers.get('gender') in ['Female', 'female']

        sexual_activity_dependent = [
            'recent_partners', 'condom_use', 'high_risk_partner', 'transactional_sex',
            'multiple_partners', 'sexual_partners', 'sti_symptoms', 'discharge_symptom',
            'painful_urination', 'genital_sores', 'previous_sti', 'sti_treatment',
            'symptom_duration', 'partner_symptoms', 'substance_sex', 'sexual_coercion',
            'partner_communication', 'partner_testing', 'partner_concurrency'
        ]

        if question_key in sexual_activity_dependent:
            return answers.get('sexual_activity') == 'Yes'

        if question_key in ['discharge_symptom', 'painful_urination', 'genital_sores', 'symptom_duration', 'partner_symptoms']:
            return answers.get('sti_symptoms') == 'Yes'
        if question_key == 'sti_treatment':
            return answers.get('previous_sti') == 'Yes'

        return True

    def _has_enough_information(self, current_answers: Dict[str, Any]) -> bool:
        """
        Determine if we have enough information to make a confident risk assessment.
        """
        answered_count = len(current_answers)

        critical_questions = ['consent', 'age', 'gender', 'sexual_activity']
        if not all(q in current_answers for q in critical_questions):
            return False

        key_risk_questions = [
            'recent_partners', 'condom_use', 'high_risk_partner',
            'sti_symptoms', 'previous_sti', 'hiv_tested', 'other_sti_tests'
        ]
        risk_factors_answered = sum(1 for q in key_risk_questions if q in current_answers)
        has_sexual_activity_info = current_answers.get('sexual_activity') == 'Yes'

        # DECISION LOGIC (REVISED)
        if has_sexual_activity_info:
            return risk_factors_answered >= 4 or answered_count >= 12
        else:
            return answered_count >= 6

    def _debug_current_answers(self, current_answers: Dict[str, Any]):
        """Debug method to log current answer state"""
        logger.info("=== DEBUG CURRENT ANSWERS ===")
        for key, value in current_answers.items():
            logger.info(f"  {key}: {value}")

        sexual_activity = current_answers.get('sexual_activity')
        logger.info(f"Sexual activity: {sexual_activity}")
        logger.info(f"Is NOT sexually active: {sexual_activity == 'No'}")
        logger.info("=============================")

    def _get_smart_next_question(self, current_answers: Dict[str, Any], available_questions: List[str]) -> str:
        """
        Gets the next question using smart business logic.
        (FIXED: Symptoms prioritized before deep sexual history.)
        """
        if not available_questions:
            return None

        answers = current_answers
        answered_set = set(current_answers.keys())

        # *** GUARANTEED FIX FOR CONSENT FIRST ***
        if not answered_set and 'consent' in available_questions:
            return 'consent'
        # *****************************************

        relevant_questions = [q for q in available_questions if self._is_question_relevant(q, current_answers)]
        if not relevant_questions:
            return available_questions[0] if available_questions else None

        question_flow = [
            # --- PHASE 1: CORE DEMOGRAPHICS ---
            ('consent', lambda: True),
            ('age', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
            ('gender', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
            ('marital_status', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
            ('education', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
            ('wealth_index', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),

            # --- PHASE 2: ACTIVITY GATEKEEPER ---
            ('sexual_activity', lambda: 'age' in answered_set and answers.get('age') and str(answers.get('age')).replace('.', '', 1).isdigit() and int(float(answers.get('age'))) >= 13),

            # *** PHASE 3: UNIVERSAL RISK (TESTING) - ABSOLUTE PRIORITY ***
            ('hiv_tested', lambda: True),
            ('last_hiv_test', lambda: answers.get('hiv_tested') == 'Yes'),
            ('other_sti_tests', lambda: True),
            ('willing_to_test', lambda: True),
            # ***************************************************************

            # --- FIX: PHASE 4: SYMPTOMS (PRIORITIZED ACTIONABLE DATA) ---
            ('sti_symptoms', lambda: answers.get('sexual_activity') == 'Yes'),
            ('discharge_symptom', lambda: answers.get('sti_symptoms') == 'Yes'),
            ('painful_urination', lambda: answers.get('sti_symptoms') == 'Yes'),
            ('genital_sores', lambda: answers.get('sti_symptoms') == 'Yes'),
            ('symptom_duration', lambda: answers.get('sti_symptoms') == 'Yes'),
            ('partner_symptoms', lambda: answers.get('sti_symptoms') == 'Yes'),
            # -----------------------------------------------------------

            # --- PHASE 5: DETAILED SEXUAL HISTORY ---
            ('sexual_partners', lambda: answers.get('sexual_activity') == 'Yes'),
            ('recent_partners', lambda: answers.get('sexual_activity') == 'Yes'),
            ('condom_use', lambda: answers.get('sexual_activity') == 'Yes'),
            ('high_risk_partner', lambda: answers.get('sexual_activity') == 'Yes'),
            ('transactional_sex', lambda: answers.get('sexual_activity') == 'Yes'),
            ('multiple_partners', lambda: answers.get('sexual_activity') == 'Yes'),

            # --- PHASE 6: STI HISTORY ---
            ('previous_sti', lambda: answers.get('sexual_activity') == 'Yes'),
            ('sti_treatment', lambda: answers.get('previous_sti') == 'Yes'),

            # --- PHASE 7: FEMALE HEALTH ---
            ('pregnancy_status', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),
            ('contraception_use', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),
            ('last_pap_smear', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),

            # --- PHASE 8: SUBSTANCE USE ---
            ('alcohol_frequency', lambda: True),
            ('drug_use', lambda: True),
            ('substance_sex', lambda: answers.get('sexual_activity') == 'Yes'),

            # --- PHASE 9: RELATIONSHIP/SAFETY ---
            ('sexual_coercion', lambda: answers.get('sexual_activity') == 'Yes'),
            ('partner_communication', lambda: answers.get('sexual_activity') == 'Yes'),
            ('partner_testing', lambda: answers.get('sexual_activity') == 'Yes'),
            ('partner_concurrency', lambda: answers.get('sexual_activity') == 'Yes'),

            # --- PHASE 10: KNOWLEDGE AND ACCESS ---
            ('sti_knowledge', lambda: True),
            ('prevention_methods', lambda: True),
            ('hiv_prep', lambda: True),
            ('health_priorities', lambda: True),
            ('insurance_coverage', lambda: True),
            ('regular_provider', lambda: True),
            ('cost_barrier', lambda: True),
            ('preferred_testing', lambda: True),
            ('testing_barriers', lambda: True),
        ]

        for question_key, condition in question_flow:
            try:
                if (question_key in relevant_questions and condition() and question_key not in answered_set):
                    return question_key
            except Exception:
                continue

        return relevant_questions[0]

    # --- Main Prediction Logic (Enhanced Zero-Encoding Fix) ---

    def get_next_question(self, current_answers: Dict[str, Any], available_questions: List[str]) -> Dict[str, Any]:
        """Get the optimal next question using Model prediction + smart logic."""

        # TEMPORARY DEBUG
        self._debug_current_answers(current_answers)

        # --- Check for termination ---
        unanswered_available = [q for q in available_questions if q not in current_answers]
        relevant_unanswered = [q for q in unanswered_available if self._is_question_relevant(q, current_answers)]
        enough_info = self._has_enough_information(current_answers)

        if not relevant_unanswered:
            logger.info("Questionnaire terminated: No more relevant questions available to ask.")
            return {
                'next_question': None, 'confidence': 1.0,
                'recommended_question': None, 'was_fallback': False,
                'enough_info': True
            }

        if enough_info:
            logger.info("Questionnaire terminated: Enough critical information gathered (Heuristic met).")
            return {
                'next_question': None, 'confidence': 1.0,
                'recommended_question': None, 'was_fallback': False,
                'enough_info': True
            }
        # --- END Check for termination ---

        # --- ENHANCED FIX: Define sexual risk keys for conditional zeroing ---
        is_not_sexually_active = current_answers.get('sexual_activity') == 'No'
        SEXUAL_RISK_KEYS = [
            'recent_partners', 'sexual_partners', 'condom_use', 'high_risk_partner',
            'transactional_sex', 'multiple_partners', 'sti_symptoms', 'discharge_symptom',
            'painful_urination', 'genital_sores', 'previous_sti', 'sti_treatment',
            'partner_concurrency', 'partner_symptoms', 'substance_sex', 'sexual_coercion',
            'partner_communication', 'partner_testing', 'symptom_duration'
        ]
        # -----------------------------------------------------------

        # --- FEATURE ENGINEERING (ENHANCED ZEROING LOGIC) ---
        features = {col: 0.0 for col in self.feature_columns}

        for question in self.all_questions:
            if question in current_answers:
                answer = current_answers[question]

                # ENHANCED FIX: If user is not sexually active AND question is a sexual risk key, force 0.0
                if is_not_sexually_active and question in SEXUAL_RISK_KEYS:
                    encoded_value = 0.0
                    logger.info(f"Zeroing sexual risk question due to no sexual activity: {question}")
                else:
                    encoded_value = self._encode_answer(question, answer)

                # Set answered status and value
                features[f'{question}_answered'] = 1.0
                features[f'{question}_value'] = encoded_value

            else:
                features[f'{question}_answered'] = 0.0
                features[f'{question}_value'] = 0.0 # Standard default for unanswered

        features['questions_answered_count'] = float(len(current_answers))

        # Completion features
        features['demographics_complete'] = 1.0 if all(q in current_answers for q in ['age', 'gender', 'marital_status']) else 0.0
        features['sexual_history_complete'] = 1.0 if all(q in current_answers for q in ['sexual_activity', 'recent_partners']) else 0.0
        features['testing_history_complete'] = 1.0 if all(q in current_answers for q in ['hiv_tested', 'other_sti_tests']) else 0.0

        # Risk profile
        risk_score = self._calculate_risk_score(current_answers)
        features['risk_score'] = risk_score
        features['high_risk_profile'] = 1.0 if risk_score > 0.5 else 0.0

        # User characteristics (Age and Gender)
        age = current_answers.get('age')
        age_float = float(age) if age and str(age).replace('.', '', 1).isdigit() else 0.0
        features['is_teen'] = 1.0 if age_float < 20 else 0.0
        features['is_young_adult'] = 1.0 if 18 <= age_float < 30 else 0.0
        features['is_older_adult'] = 1.0 if age_float >= 50 else 0.0
        features['is_female'] = 1.0 if current_answers.get('gender') in ['Female', 'female'] else 0.0
        is_sexually_active = 1.0 if current_answers.get('sexual_activity') == 'Yes' else 0.0
        features['is_sexually_active'] = is_sexually_active
        features['needs_female_questions'] = 1.0 if features['is_female'] == 1.0 and is_sexually_active == 1.0 else 0.0

        # Sequence patterns
        last_question = list(current_answers.keys())[-1] if current_answers else None
        if last_question:
            features['last_question_category'] = self._encode_answer('category', self._get_question_category(last_question))
        else:
            features['last_question_category'] = 0.0

        features['next_expected_category'] = self._encode_answer('category', self._get_expected_next_category(current_answers))

        # Answer patterns
        yes_count = sum(1 for answer in current_answers.values() if str(answer).lower() in ['yes', 'y', 'true', '1'])
        features['yes_answer_ratio'] = yes_count / len(current_answers) if current_answers else 0.0

        # Add logging to debug feature values
        logger.info(f"User sexual_activity: {current_answers.get('sexual_activity')}")
        logger.info(f"Is not sexually active: {is_not_sexually_active}")
        logger.info(f"Calculated risk_score: {risk_score}")
        logger.info(f"Sample encoded values - recent_partners_value: {features.get('recent_partners_value', 'N/A')}")
        logger.info(f"Sample encoded values - condom_use_value: {features.get('condom_use_value', 'N/A')}")

        # --- Model Prediction ---
        predicted_question, confidence = None, 0.5
        final_question = self._get_smart_next_question(current_answers, unanswered_available)
        was_fallback = True

        try:
            input_data = [features.get(col, 0.0) for col in self.feature_columns]
            input_array = np.array([input_data])

            predicted_encoded = self.model.predict(input_array)[0]
            predicted_question = self.label_encoder.inverse_transform([predicted_encoded])[0]

            if hasattr(self.model, 'predict_proba'):
                probabilities = self.model.predict_proba(input_array)[0]
                confidence = float(np.max(probabilities))
            else:
                confidence = 0.85

            if (predicted_question and
                    predicted_question in unanswered_available and
                    self._is_question_relevant(predicted_question, current_answers)):
                final_question = predicted_question
                was_fallback = False

        except Exception as e:
            logger.warning(f"Model prediction failed: {e}. Using rule-based fallback.")
            confidence = 0.7

        return {
            'next_question': final_question,
            'confidence': confidence,
            'recommended_question': predicted_question,
            'was_fallback': was_fallback,
            'enough_info': False
        }

# --- Initialization ---
try:
    model_path = get_model_path()
    if model_exists():
        questionnaire = SmartQuestionnaire(str(model_path))
    else:
        logger.error(f"Model file not found at: {model_path}. Running the service in degraded mode.")
        questionnaire = None
except Exception as e:
    logger.error(f"Failed to initialize questionnaire service: {e}")
    questionnaire = None

# --- API Endpoints ---

@app.get("/")
async def root():
    model_status = "loaded" if questionnaire else "degraded (model missing)"
    return {
        "message": "Smart Question Sequencing API",
        "version": "2.0.0",
        "model_loaded": questionnaire is not None,
        "model_status": model_status,
        "model_path_used": str(get_model_path()),
        "endpoints": ["/question/next", "/health", "/model/info", "/questions/all"]
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy" if questionnaire else "degraded",
        "model_loaded": questionnaire is not None,
        "model_exists": model_exists(),
        "timestamp": datetime.now().isoformat()
    }

@app.post("/question/next", response_model=QuestionResponse)
async def get_next_question_api(request: QuestionRequest):
    """Get the next optimal question based on current answers"""
    if not questionnaire:
        raise HTTPException(status_code=503, detail=f"Questionnaire service unavailable. Model '{MODEL_FILENAME}' not loaded.")

    try:
        unanswered_available = [q for q in request.available_questions if q not in request.current_answers]
        prediction = questionnaire.get_next_question(
            current_answers=request.current_answers,
            available_questions=unanswered_available
        )

        total_questions = len(questionnaire.all_questions)
        answered_count = len(request.current_answers)
        progress_percentage = (answered_count / max(total_questions, 1)) * 100

        remaining_questions = [
            q for q in unanswered_available
            if q != prediction['next_question']
        ] if prediction['next_question'] else []

        return QuestionResponse(
            success=True,
            next_question=prediction['next_question'],
            progress={
                "answered": answered_count, "total": total_questions,
                "percentage": round(progress_percentage, 1), "remaining": len(remaining_questions)
            },
            confidence=prediction['confidence'],
            remaining_questions=remaining_questions,
            timestamp=datetime.now().isoformat(),
            enough_info=prediction['enough_info'],
            model_version=questionnaire.model_version
        )

    except Exception as e:
        logger.error(f"Error processing question request: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/questions/all")
async def get_all_questions():
    if not questionnaire:
        raise HTTPException(status_code=503, detail="Questionnaire service not available")
    return {"questions": questionnaire.all_questions, "count": len(questionnaire.all_questions), "timestamp": datetime.now().isoformat()}

@app.get("/model/info")
async def get_model_info():
    if not questionnaire:
        raise HTTPException(status_code=503, detail="Questionnaire service not available")

    feature_importance = {}
    if hasattr(questionnaire.model, 'feature_importances_'):
        important_features = sorted(
            zip(questionnaire.feature_columns, questionnaire.model.feature_importances_),
            key=lambda x: x[1], reverse=True
        )[:10]
        feature_importance = {k: float(v) for k, v in important_features}

    return {
        "model_loaded": True,
        "model_name": MODEL_FILENAME,
        "model_version": questionnaire.model_version,
        "feature_count": len(questionnaire.feature_columns),
        "question_count": len(questionnaire.all_questions),
        "tree_depth": questionnaire.model.get_depth() if hasattr(questionnaire.model, 'get_depth') else 'N/A',
        "best_parameters": questionnaire.best_params,
        "training_accuracy": questionnaire.cv_results,
        "top_features": feature_importance,
        "model_path": questionnaire.model_path
    }

# --- Uvicorn Server Execution ---

if __name__ == "__main__":
    import uvicorn
    host = os.getenv('HOST', '0.0.0.0')
    port = int(os.getenv('PORT', '8001'))

    logger.info("=" * 50)
    logger.info("SMART QUESTION SEQUENCING API STARTUP")
    logger.info("=" * 50)

    if questionnaire is None:
        logger.error("FATAL: Service starting in degraded (no model) mode.")
    else:
        logger.info(f"Service running successfully with model: {questionnaire.model_path}")

    uvicorn.run(app, host=host, port=port, log_level="info", reload=True)