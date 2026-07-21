# enhanced_massive_training.py
import pandas as pd
import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import GridSearchCV, cross_val_score, StratifiedKFold
from pathlib import Path
import warnings
warnings.filterwarnings('ignore')
from tqdm import tqdm
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def get_questions_data():
    """Get the complete questions dataset with all 44 questions"""
    questions_data = [
        # Original 40 questions
        {"id": 1, "display_order": 1, "question_key": "consent", "question_text": "Do you consent to participate in this STI risk assessment?", "question_type": "yes_no", "section_title": "Consent and Demographics"},
        {"id": 2, "display_order": 2, "question_key": "age", "question_text": "What is your age?", "question_type": "number", "section_title": "Consent and Demographics"},
        {"id": 3, "display_order": 3, "question_key": "gender", "question_text": "What is your gender?", "question_type": "multiple_choice", "section_title": "Consent and Demographics"},
        {"id": 4, "display_order": 4, "question_key": "sexual_activity", "question_text": "Are you currently sexually active?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 5, "display_order": 5, "question_key": "recent_partners", "question_text": "How many sexual partners have you had in the past 12 months?", "question_type": "number", "section_title": "Sexual History"},
        {"id": 6, "display_order": 6, "question_key": "condom_use", "question_text": "How often do you use condoms or other barrier methods?", "question_type": "multiple_choice", "section_title": "Sexual History"},
        {"id": 7, "display_order": 7, "question_key": "high_risk_partner", "question_text": "Have you had sexual contact with a partner who you know has an STI or engages in high-risk behaviors?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 8, "display_order": 8, "question_key": "transactional_sex", "question_text": "Have you ever exchanged sex for money, drugs, or other resources?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 9, "display_order": 9, "question_key": "discharge_symptom", "question_text": "Are you experiencing any unusual genital discharge?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 10, "display_order": 10, "question_key": "painful_urination", "question_text": "Are you experiencing pain or burning during urination?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 11, "display_order": 11, "question_key": "genital_sores", "question_text": "Do you have any sores, bumps, or rashes in the genital area?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 12, "display_order": 12, "question_key": "sti_symptoms", "question_text": "Are you currently experiencing any STI symptoms?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 13, "display_order": 13, "question_key": "previous_sti", "question_text": "Have you ever been diagnosed with an STI before?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 14, "display_order": 14, "question_key": "sti_treatment", "question_text": "If you had a previous STI, did you complete the full treatment?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 15, "display_order": 15, "question_key": "hiv_tested", "question_text": "Have you ever been tested for HIV?", "question_type": "yes_no", "section_title": "Testing History"},
        {"id": 16, "display_order": 16, "question_key": "last_hiv_test", "question_text": "When was your last HIV test?", "question_type": "multiple_choice", "section_title": "Testing History"},
        {"id": 17, "display_order": 17, "question_key": "other_sti_tests", "question_text": "Have you been tested for other STIs (like chlamydia, gonorrhea, syphilis) in the past year?", "question_type": "yes_no", "section_title": "Testing History"},
        {"id": 18, "display_order": 18, "question_key": "willing_to_test", "question_text": "Would you be willing to get tested for STIs if recommended?", "question_type": "yes_no", "section_title": "Testing History"},
        {"id": 19, "display_order": 19, "question_key": "pregnancy_status", "question_text": "Is there any chance you could be pregnant?", "question_type": "yes_no", "section_title": "Female Reproductive Health"},
        {"id": 20, "display_order": 20, "question_key": "contraception_use", "question_text": "What method of contraception do you currently use, if any?", "question_type": "multiple_choice", "section_title": "Female Reproductive Health"},
        {"id": 21, "display_order": 21, "question_key": "last_pap_smear", "question_text": "When was your last Pap smear or cervical cancer screening?", "question_type": "multiple_choice", "section_title": "Female Reproductive Health"},
        {"id": 22, "display_order": 22, "question_key": "substance_sex", "question_text": "Do you ever use alcohol or drugs before or during sexual activity?", "question_type": "yes_no", "section_title": "Substance Use"},
        {"id": 23, "display_order": 23, "question_key": "alcohol_frequency", "question_text": "How often do you drink alcohol?", "question_type": "multiple_choice", "section_title": "Substance Use"},
        {"id": 24, "display_order": 24, "question_key": "drug_use", "question_text": "Have you used any recreational drugs in the past 3 months?", "question_type": "yes_no", "section_title": "Substance Use"},
        {"id": 25, "display_order": 25, "question_key": "sexual_coercion", "question_text": "Have you ever felt pressured or forced into sexual activity against your will?", "question_type": "yes_no", "section_title": "Relationship and Safety"},
        {"id": 26, "display_order": 26, "question_key": "partner_communication", "question_text": "Do you feel comfortable discussing sexual health with your partners?", "question_type": "yes_no", "section_title": "Relationship and Safety"},
        {"id": 27, "display_order": 27, "question_key": "partner_testing", "question_text": "Do you know if your current partner(s) have been tested for STIs?", "question_type": "multiple_choice", "section_title": "Relationship and Safety"},
        {"id": 28, "display_order": 28, "question_key": "sti_knowledge", "question_text": "How would you rate your knowledge about STI prevention?", "question_type": "multiple_choice", "section_title": "Knowledge and Prevention"},
        {"id": 29, "display_order": 29, "question_key": "prevention_methods", "question_text": "Which STI prevention methods are you familiar with?", "question_type": "multiple_choice", "section_title": "Knowledge and Prevention"},
        {"id": 30, "display_order": 30, "question_key": "hiv_prep", "question_text": "Have you heard of PrEP (Pre-Exposure Prophylaxis) for HIV prevention?", "question_type": "yes_no", "section_title": "Knowledge and Prevention"},
        {"id": 31, "display_order": 31, "question_key": "insurance_coverage", "question_text": "Do you have health insurance that covers STI testing?", "question_type": "yes_no", "section_title": "Healthcare Access"},
        {"id": 32, "display_order": 32, "question_key": "regular_provider", "question_text": "Do you have a regular healthcare provider?", "question_type": "yes_no", "section_title": "Healthcare Access"},
        {"id": 33, "display_order": 33, "question_key": "cost_barrier", "question_text": "Has cost ever prevented you from getting STI testing or treatment?", "question_type": "yes_no", "section_title": "Healthcare Access"},
        {"id": 34, "display_order": 34, "question_key": "preferred_testing", "question_text": "Where would you prefer to get STI testing if needed?", "question_type": "multiple_choice", "section_title": "Healthcare Access"},
        {"id": 35, "display_order": 35, "question_key": "multiple_partners", "question_text": "Have you had more than one sexual partner in the past year?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 36, "display_order": 36, "question_key": "partner_concurrency", "question_text": "Have any of your partners had other partners during your relationship?", "question_type": "yes_no", "section_title": "Relationship and Safety"},
        {"id": 37, "display_order": 37, "question_key": "symptom_duration", "question_text": "How long have you been experiencing these symptoms?", "question_type": "multiple_choice", "section_title": "Symptoms and Health History"},
        {"id": 38, "display_order": 38, "question_key": "partner_symptoms", "question_text": "Is your partner experiencing any similar symptoms?", "question_type": "yes_no", "section_title": "Symptoms and Health History"},
        {"id": 39, "display_order": 39, "question_key": "testing_barriers", "question_text": "What are the main barriers to getting tested for you?", "question_type": "multiple_choice", "section_title": "Healthcare Access"},
        {"id": 40, "display_order": 40, "question_key": "health_priorities", "question_text": "How important is sexual health to your overall wellbeing?", "question_type": "multiple_choice", "section_title": "Knowledge and Prevention"},

        # New questions for risk assessment model
        {"id": 41, "display_order": 41, "question_key": "marital_status", "question_text": "What is your current marital status?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["Single", "Married", "Divorced", "Widowed", "Separated", "Living with partner"]},
        {"id": 42, "display_order": 42, "question_key": "education", "question_text": "What is your highest level of education?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["No formal education", "Primary school", "Secondary school", "High school", "College/University", "Postgraduate"]},
        {"id": 43, "display_order": 43, "question_key": "wealth_index", "question_text": "How would you describe your household's economic situation?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["Low income", "Lower middle income", "Middle income", "Upper middle income", "High income"]},
        {"id": 44, "display_order": 44, "question_key": "sexual_partners", "question_text": "How many sexual partners have you had in your lifetime?", "question_type": "number", "section_title": "Sexual History", "min_value": 0, "max_value": 50},
    ]
    return pd.DataFrame(questions_data)

def encode_answer(question, answer):
    """Enhanced encoding with more granular values"""
    if answer is None:
        return 0.0

    answer_str = str(answer).lower().strip()

    # Numeric questions with better normalization
    if question in ['age', 'recent_partners', 'sexual_partners']:
        try:
            value = float(answer)
            if question == 'age':
                # More granular age encoding
                if value < 18: return value / 18.0 * 0.3
                elif value < 25: return 0.3 + (value - 18) / 7.0 * 0.3
                elif value < 35: return 0.6 + (value - 25) / 10.0 * 0.2
                else: return 0.8 + min((value - 35) / 65.0 * 0.2, 0.2)
            else:
                # Partner count with logarithmic scaling
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

    # Enhanced categorical encoding with more granular values
    if question == 'gender':
        return {'male': 0.0, 'female': 1.0, 'other': 0.5}.get(answer_str, 0.0)
    elif question == 'condom_use':
        return {'never': 0.0, 'sometimes': 0.5, 'always': 1.0}.get(answer_str, 0.5)
    elif question == 'alcohol_frequency':
        levels = {'never': 0.0, 'monthly or less': 0.2, '2-4 times per month': 0.5,
                  '2-3 times per week': 0.8, '4 or more times per week': 1.0}
        return levels.get(answer_str, 0.5)
    elif question == 'last_hiv_test':
        levels = {'never tested': 0.0, 'more than 1 year ago': 0.3, '3-12 months ago': 0.7,
                  'within the last 3 months': 1.0}
        return levels.get(answer_str, 0.5)
    elif question == 'marital_status':
        levels = {'single': 0.0, 'living with partner': 0.4, 'separated': 0.5,
                  'divorced': 0.6, 'widowed': 0.7, 'married': 1.0}
        return levels.get(answer_str, 0.0)
    elif question == 'education':
        levels = {'no formal education': 0.0, 'primary school': 0.2,
                  'secondary school': 0.4, 'high school': 0.6,
                  'college/university': 0.8, 'postgraduate': 1.0}
        return levels.get(answer_str, 0.0)
    elif question == 'wealth_index':
        levels = {'low income': 0.0, 'lower middle income': 0.3,
                  'middle income': 0.6, 'upper middle income': 0.8,
                  'high income': 1.0}
        return levels.get(answer_str, 0.0)
    elif question == 'sti_knowledge':
        levels = {'not knowledgeable at all': 0.0, 'not very knowledgeable': 0.3,
                  'somewhat knowledgeable': 0.7, 'very knowledgeable': 1.0}
        return levels.get(answer_str, 0.5)
    elif question == 'health_priorities':
        levels = {'not important at all': 0.0, 'not very important': 0.3,
                  'somewhat important': 0.7, 'very important': 1.0}
        return levels.get(answer_str, 0.5)

    return 0.5

def calculate_risk_score(answers):
    """Calculate comprehensive risk score"""
    score = 0.0

    # Sexual behavior risk
    if answers.get('sexual_activity') == 'Yes':
        score += 0.3
        if answers.get('condom_use') == 'Never':
            score += 0.2
        if answers.get('high_risk_partner') == 'Yes':
            score += 0.2
        if answers.get('multiple_partners') == 'Yes':
            score += 0.1
        if answers.get('transactional_sex') == 'Yes':
            score += 0.2

    # Symptoms risk
    if answers.get('sti_symptoms') == 'Yes':
        score += 0.3

    # History risk
    if answers.get('previous_sti') == 'Yes':
        score += 0.2

    return min(score, 1.0)

def get_question_category(question_key):
    """Categorize questions for sequence patterns"""
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

def get_expected_next_category(current_answers):
    """Predict which category should come next"""
    answered_categories = set()
    for q in current_answers.keys():
        answered_categories.add(get_question_category(q))

    # Define category sequence
    category_sequence = ['demographics', 'sexual_history', 'symptoms', 'testing',
                         'female_health', 'substance', 'relationship', 'knowledge', 'healthcare']

    for category in category_sequence:
        if category not in answered_categories:
            return category

    return 'healthcare'  # Default

def create_enhanced_feature_vector(current_answers, questions_df):
    """Create feature vector with enhanced features"""
    question_keys = questions_df['question_key'].tolist()
    features = {}

    # Basic features: answered status and values
    for question in question_keys:
        features[f'{question}_answered'] = 1.0 if question in current_answers else 0.0
        features[f'{question}_value'] = encode_answer(question, current_answers.get(question)) if question in current_answers else 0.0

    # Enhanced: Interaction and completion features
    features['questions_answered_count'] = float(len(current_answers))
    features['demographics_complete'] = 1.0 if all(q in current_answers for q in ['age', 'gender', 'marital_status']) else 0.0
    features['sexual_history_complete'] = 1.0 if all(q in current_answers for q in ['sexual_activity', 'recent_partners']) else 0.0
    features['testing_history_complete'] = 1.0 if all(q in current_answers for q in ['hiv_tested', 'other_sti_tests']) else 0.0

    # Enhanced: Risk profile
    features['risk_score'] = calculate_risk_score(current_answers)
    features['high_risk_profile'] = 1.0 if calculate_risk_score(current_answers) > 0.5 else 0.0

    # Enhanced: User characteristics
    age = current_answers.get('age')
    if age:
        try:
            age_float = float(age)
            features['is_teen'] = 1.0 if age_float < 20 else 0.0
            features['is_young_adult'] = 1.0 if 18 <= age_float < 30 else 0.0
            features['is_older_adult'] = 1.0 if age_float >= 50 else 0.0
        except:
            features['is_teen'] = 0.0
            features['is_young_adult'] = 0.0
            features['is_older_adult'] = 0.0
    else:
        features['is_teen'] = 0.0
        features['is_young_adult'] = 0.0
        features['is_older_adult'] = 0.0

    features['is_female'] = 1.0 if current_answers.get('gender') in ['Female', 'female'] else 0.0
    features['is_sexually_active'] = 1.0 if current_answers.get('sexual_activity') == 'Yes' else 0.0
    features['needs_female_questions'] = 1.0 if current_answers.get('gender') in ['Female', 'female'] and current_answers.get('sexual_activity') == 'Yes' else 0.0

    # Enhanced: Sequence patterns
    if current_answers:
        last_question = list(current_answers.keys())[-1]
        features['last_question_category'] = encode_answer('category', get_question_category(last_question))
    else:
        features['last_question_category'] = 0.0

    features['next_expected_category'] = encode_answer('category', get_expected_next_category(current_answers))

    # Enhanced: Answer patterns
    yes_count = sum(1 for answer in current_answers.values() if str(answer).lower() in ['yes', 'y', 'true', '1'])
    features['yes_answer_ratio'] = yes_count / len(current_answers) if current_answers else 0.0

    return features

def is_question_relevant(question_key, current_answers):
    """Enhanced relevance checking"""
    answers = current_answers

    # Always relevant questions
    always_relevant = [
        'consent', 'age', 'gender', 'marital_status', 'education', 'wealth_index',
        'sti_knowledge', 'prevention_methods', 'hiv_prep', 'health_priorities',
        'insurance_coverage', 'regular_provider', 'cost_barrier', 'preferred_testing',
        'testing_barriers', 'alcohol_frequency', 'drug_use', 'hiv_tested',
        'other_sti_tests', 'willing_to_test'
    ]

    if question_key in always_relevant:
        return True

    # Gender-specific questions
    if question_key in ['pregnancy_status', 'contraception_use', 'last_pap_smear']:
        return answers.get('gender') in ['Female', 'female']

    # Sexual activity dependent questions
    sexual_activity_dependent = [
        'recent_partners', 'condom_use', 'high_risk_partner', 'transactional_sex',
        'multiple_partners', 'sexual_partners', 'sti_symptoms', 'discharge_symptom',
        'painful_urination', 'genital_sores', 'previous_sti', 'sti_treatment',
        'symptom_duration', 'partner_symptoms', 'substance_sex', 'sexual_coercion',
        'partner_communication', 'partner_testing', 'partner_concurrency'
    ]

    if question_key in sexual_activity_dependent:
        return answers.get('sexual_activity') == 'Yes'

    return True

def get_smart_next_question(current_answers, available_questions, questions_df):
    """Enhanced smart question sequencing"""
    if not available_questions:
        return None

    # Filter to only relevant questions
    relevant_questions = [q for q in available_questions if is_question_relevant(q, current_answers)]

    if not relevant_questions:
        return available_questions[0] if available_questions else None

    answers = current_answers
    answered_set = set(current_answers.keys())

    # Enhanced question flow with better logic
    question_flow = [
        # Phase 1: Core Demographics
        ('consent', lambda: True),
        ('age', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
        ('gender', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
        ('marital_status', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
        ('education', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),
        ('wealth_index', lambda: 'consent' in answered_set and answers.get('consent') == 'Yes'),

        # Phase 2: Sexual Activity (gatekeeper)
        ('sexual_activity', lambda: 'age' in answered_set and answers.get('age') and int(answers.get('age', 0)) >= 13),

        # Phase 3A: If sexually active - detailed history
        ('sexual_partners', lambda: answers.get('sexual_activity') == 'Yes'),
        ('recent_partners', lambda: answers.get('sexual_activity') == 'Yes'),
        ('condom_use', lambda: answers.get('sexual_activity') == 'Yes'),
        ('high_risk_partner', lambda: answers.get('sexual_activity') == 'Yes'),
        ('transactional_sex', lambda: answers.get('sexual_activity') == 'Yes'),
        ('multiple_partners', lambda: answers.get('sexual_activity') == 'Yes'),

        # Phase 3B: Testing history (for everyone)
        ('hiv_tested', lambda: True),
        ('last_hiv_test', lambda: answers.get('hiv_tested') == 'Yes'),
        ('other_sti_tests', lambda: True),
        ('willing_to_test', lambda: True),

        # Phase 4: Symptoms (only if sexually active)
        ('sti_symptoms', lambda: answers.get('sexual_activity') == 'Yes'),
        ('discharge_symptom', lambda: answers.get('sti_symptoms') == 'Yes'),
        ('painful_urination', lambda: answers.get('sti_symptoms') == 'Yes'),
        ('genital_sores', lambda: answers.get('sti_symptoms') == 'Yes'),
        ('symptom_duration', lambda: answers.get('sti_symptoms') == 'Yes'),
        ('partner_symptoms', lambda: answers.get('sti_symptoms') == 'Yes'),

        # Phase 5: STI History (only if sexually active)
        ('previous_sti', lambda: answers.get('sexual_activity') == 'Yes'),
        ('sti_treatment', lambda: answers.get('previous_sti') == 'Yes'),

        # Phase 6: Female Reproductive Health
        ('pregnancy_status', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),
        ('contraception_use', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),
        ('last_pap_smear', lambda: answers.get('gender') in ['Female', 'female'] and answers.get('sexual_activity') == 'Yes'),

        # Phase 7: Substance Use
        ('substance_sex', lambda: answers.get('sexual_activity') == 'Yes'),
        ('alcohol_frequency', lambda: True),
        ('drug_use', lambda: True),

        # Phase 8: Relationship and Safety
        ('sexual_coercion', lambda: answers.get('sexual_activity') == 'Yes'),
        ('partner_communication', lambda: answers.get('sexual_activity') == 'Yes'),
        ('partner_testing', lambda: answers.get('sexual_activity') == 'Yes'),
        ('partner_concurrency', lambda: answers.get('sexual_activity') == 'Yes'),

        # Phase 9: Knowledge and Prevention
        ('sti_knowledge', lambda: True),
        ('prevention_methods', lambda: True),
        ('hiv_prep', lambda: True),
        ('health_priorities', lambda: True),

        # Phase 10: Healthcare Access
        ('insurance_coverage', lambda: True),
        ('regular_provider', lambda: True),
        ('cost_barrier', lambda: True),
        ('preferred_testing', lambda: True),
        ('testing_barriers', lambda: True),
    ]

    for question_key, condition in question_flow:
        try:
            if (question_key in relevant_questions and
                    condition() and
                    question_key not in answered_set):
                return question_key
        except:
            continue

    return relevant_questions[0]

def create_realistic_user_profile():
    """Create highly realistic user profiles"""
    profile = {}

    # Demographics with realistic distributions
    profile['consent'] = 'Yes'  # 99% consent rate

    # Age distribution weighted toward young adults (higher STI risk)
    age_choices = list(range(15, 20)) * 3 + list(range(20, 30)) * 5 + list(range(30, 50)) * 2 + list(range(50, 70))
    profile['age'] = np.random.choice(age_choices)

    # Gender distribution
    profile['gender'] = np.random.choice(['Male', 'Female'], p=[0.49, 0.51])

    # Marital status correlated with age
    if profile['age'] < 25:
        profile['marital_status'] = np.random.choice(['Single', 'Living with partner'], p=[0.8, 0.2])
    else:
        profile['marital_status'] = np.random.choice(['Single', 'Married', 'Living with partner', 'Divorced'],
                                                     p=[0.3, 0.5, 0.15, 0.05])

    # Education correlated with age
    if profile['age'] < 25:
        profile['education'] = np.random.choice(['High school', 'College/University'], p=[0.4, 0.6])
    else:
        profile['education'] = np.random.choice(['High school', 'College/University', 'Postgraduate'],
                                                p=[0.3, 0.5, 0.2])

    # Wealth index
    profile['wealth_index'] = np.random.choice(['Lower middle income', 'Middle income', 'Upper middle income'],
                                               p=[0.4, 0.4, 0.2])

    # Sexual activity - strongly age-dependent
    if profile['age'] < 16:
        sexual_active_prob = 0.2
    elif profile['age'] < 20:
        sexual_active_prob = 0.7
    elif profile['age'] < 30:
        sexual_active_prob = 0.9
    else:
        sexual_active_prob = 0.8

    profile['sexual_activity'] = np.random.choice(['Yes', 'No'], p=[sexual_active_prob, 1-sexual_active_prob])

    # Generate detailed profile based on sexual activity
    if profile['sexual_activity'] == 'Yes':
        generate_sexually_active_profile(profile)
    else:
        generate_non_sexually_active_profile(profile)

    return profile

def generate_sexually_active_profile(profile):
    """Generate profile for sexually active users"""
    # Partner counts with realistic distributions
    profile['sexual_partners'] = max(1, int(np.random.exponential(2.0)))
    profile['recent_partners'] = min(profile['sexual_partners'], max(1, int(np.random.poisson(1.5))))

    # Condom use correlated with age and education
    if profile['age'] < 25 or profile['education'] in ['College/University', 'Postgraduate']:
        profile['condom_use'] = np.random.choice(['Always', 'Sometimes', 'Never'], p=[0.4, 0.5, 0.1])
    else:
        profile['condom_use'] = np.random.choice(['Always', 'Sometimes', 'Never'], p=[0.2, 0.5, 0.3])

    # Risk factors
    profile['high_risk_partner'] = np.random.choice(['Yes', 'No'], p=[0.25, 0.75])
    profile['transactional_sex'] = np.random.choice(['Yes', 'No'], p=[0.05, 0.95])
    profile['multiple_partners'] = 'Yes' if profile['sexual_partners'] > 1 else 'No'

    # Symptoms based on risk factors
    risk_score = (0.3 if profile['condom_use'] == 'Never' else 0.0 +
                                                               0.3 if profile['high_risk_partner'] == 'Yes' else 0.0 +
                                                                                                                 0.2 if profile['transactional_sex'] == 'Yes' else 0.0 +
                                                                                                                                                                   0.1 if profile['sexual_partners'] > 3 else 0.0)

    symptom_prob = min(0.7, 0.1 + risk_score)
    profile['sti_symptoms'] = np.random.choice(['Yes', 'No'], p=[symptom_prob, 1-symptom_prob])

    if profile['sti_symptoms'] == 'Yes':
        profile['discharge_symptom'] = np.random.choice(['Yes', 'No'], p=[0.8, 0.2])
        profile['painful_urination'] = np.random.choice(['Yes', 'No'], p=[0.7, 0.3])
        profile['genital_sores'] = np.random.choice(['Yes', 'No'], p=[0.4, 0.6])
        profile['symptom_duration'] = np.random.choice(['Less than 1 week', '1-4 weeks', '1-3 months', 'More than 3 months'])
        profile['partner_symptoms'] = np.random.choice(['Yes', 'No'], p=[0.3, 0.7])
    else:
        for symptom_q in ['discharge_symptom', 'painful_urination', 'genital_sores', 'partner_symptoms']:
            profile[symptom_q] = 'No'
        profile['symptom_duration'] = 'N/A'

    # STI history correlated with risk
    previous_sti_prob = min(0.3, 0.05 + risk_score * 0.5)
    profile['previous_sti'] = np.random.choice(['Yes', 'No'], p=[previous_sti_prob, 1-previous_sti_prob])
    profile['sti_treatment'] = 'Yes' if profile['previous_sti'] == 'Yes' else 'No'

    # Relationship factors
    profile['sexual_coercion'] = np.random.choice(['Yes', 'No'], p=[0.1, 0.9])
    profile['partner_communication'] = np.random.choice(['Yes', 'No'], p=[0.7, 0.3])
    profile['partner_testing'] = np.random.choice(['Yes, and results were shared', 'Yes, but results not shared', 'No', 'Not sure'])
    profile['partner_concurrency'] = np.random.choice(['Yes', 'No', 'Not sure'])
    profile['substance_sex'] = np.random.choice(['Yes', 'No'], p=[0.3, 0.7])

def generate_non_sexually_active_profile(profile):
    """Generate profile for non-sexually active users"""
    non_sexual_defaults = {
        'sexual_partners': 0, 'recent_partners': 0, 'condom_use': 'N/A',
        'high_risk_partner': 'No', 'transactional_sex': 'No', 'multiple_partners': 'No',
        'sti_symptoms': 'No', 'discharge_symptom': 'No', 'painful_urination': 'No',
        'genital_sores': 'No', 'previous_sti': 'No', 'sti_treatment': 'No',
        'symptom_duration': 'N/A', 'partner_symptoms': 'No', 'sexual_coercion': 'No',
        'partner_communication': 'N/A', 'partner_testing': 'N/A', 'partner_concurrency': 'N/A',
        'substance_sex': 'No'
    }
    profile.update(non_sexual_defaults)

def simulate_complete_question_sequence(user_profile, questions_df):
    """Simulate the IDEAL question sequence for training"""
    sequence = []
    current_answers = {}
    available_questions = questions_df['question_key'].tolist()

    # Always follow exact business logic for training
    while available_questions:
        next_q = get_smart_next_question(current_answers, available_questions, questions_df)
        if not next_q:
            break

        sequence.append(next_q)
        current_answers[next_q] = user_profile[next_q]
        available_questions.remove(next_q)

    return sequence

def create_enhanced_training_data(questions_df, n_samples=1000000):
    """Create massive, high-quality training data"""
    logger.info(f"Generating {n_samples:,} training samples...")

    features_list = []
    targets_list = []

    question_keys = questions_df['question_key'].tolist()

    # Create feature columns
    feature_columns = []
    for question in question_keys:
        feature_columns.append(f'{question}_answered')
        feature_columns.append(f'{question}_value')
    feature_columns.extend([
        'questions_answered_count', 'demographics_complete', 'sexual_history_complete',
        'testing_history_complete', 'risk_score', 'high_risk_profile', 'is_teen',
        'is_young_adult', 'is_older_adult', 'is_female', 'is_sexually_active',
        'needs_female_questions', 'last_question_category', 'next_expected_category',
        'yes_answer_ratio'
    ])

    with tqdm(total=n_samples, desc="Generating training data") as pbar:
        for i in range(n_samples):
            if i > 0 and i % 100000 == 0:
                logger.info(f"Generated {i:,} samples...")

            # Create realistic user profile
            user_profile = create_realistic_user_profile()

            # Generate testing and knowledge questions for everyone
            user_profile['hiv_tested'] = np.random.choice(['Yes', 'No'], p=[0.6, 0.4])
            user_profile['last_hiv_test'] = np.random.choice(['Within the last 3 months', '3-12 months ago', 'More than 1 year ago', 'Never tested'])
            user_profile['other_sti_tests'] = np.random.choice(['Yes', 'No'], p=[0.5, 0.5])
            user_profile['willing_to_test'] = np.random.choice(['Yes', 'No'], p=[0.8, 0.2])

            # Female reproductive health
            if user_profile['gender'] == 'Female' and user_profile['sexual_activity'] == 'Yes':
                user_profile['pregnancy_status'] = np.random.choice(['Yes', 'No', 'Not sure'], p=[0.15, 0.8, 0.05])
                user_profile['contraception_use'] = np.random.choice(['Oral contraceptives (pill)', 'Condoms', 'IUD', 'Implant', 'Injectable', 'None', 'Other'])
                user_profile['last_pap_smear'] = np.random.choice(['Within the last year', '1-3 years ago', 'More than 3 years ago', 'Never'])
            else:
                user_profile['pregnancy_status'] = 'No'
                user_profile['contraception_use'] = 'N/A'
                user_profile['last_pap_smear'] = 'N/A'

            # Substance use
            user_profile['alcohol_frequency'] = np.random.choice(['Never', 'Monthly or less', '2-4 times per month', '2-3 times per week', '4 or more times per week'])
            user_profile['drug_use'] = np.random.choice(['Yes', 'No'], p=[0.15, 0.85])

            # Knowledge and prevention
            user_profile['sti_knowledge'] = np.random.choice(['Very knowledgeable', 'Somewhat knowledgeable', 'Not very knowledgeable', 'Not knowledgeable at all'])
            user_profile['prevention_methods'] = np.random.choice(['Condoms', 'Regular testing', 'Pre-Exposure Prophylaxis (PrEP)', 'Vaccines (HPV, Hepatitis B)', 'Partner communication', 'Monogamy'])
            user_profile['hiv_prep'] = np.random.choice(['Yes', 'No'], p=[0.4, 0.6])
            user_profile['health_priorities'] = np.random.choice(['Very important', 'Somewhat important', 'Not very important', 'Not important at all'])

            # Healthcare access
            user_profile['insurance_coverage'] = np.random.choice(['Yes', 'No'], p=[0.7, 0.3])
            user_profile['regular_provider'] = np.random.choice(['Yes', 'No'], p=[0.6, 0.4])
            user_profile['cost_barrier'] = np.random.choice(['Yes', 'No'], p=[0.3, 0.7])
            user_profile['preferred_testing'] = np.random.choice(['Primary care provider', 'Public health clinic', 'STI specialty clinic', 'Community health center', 'At-home testing kit', 'Pharmacy'])
            user_profile['testing_barriers'] = np.random.choice(['Cost', 'Lack of time', 'Fear of results', 'Stigma', 'Not knowing where to go', 'No symptoms', 'Other'])

            # Simulate complete question sequence
            sequence = simulate_complete_question_sequence(user_profile, questions_df)

            # Create training examples from each step of the sequence
            for step in range(1, len(sequence)):
                current_answers = {q: user_profile[q] for q in sequence[:step]}
                next_correct_question = sequence[step]

                # Create enhanced feature vector
                features = create_enhanced_feature_vector(current_answers, questions_df)
                features_list.append(features)
                targets_list.append(next_correct_question)

            pbar.update(1)

    # Convert to DataFrame
    X = pd.DataFrame(features_list)[feature_columns]
    y = np.array(targets_list)

    logger.info(f"Final training data shape: {X.shape}")
    logger.info(f"Target distribution:\n{pd.Series(y).value_counts().head(15)}")

    return X, y, feature_columns, question_keys

def hyperparameter_tuning(X, y):
    """Enhanced hyperparameter tuning for confidence"""
    logger.info("Performing enhanced hyperparameter tuning...")

    param_grid = {
        'n_estimators': [200, 300],
        'max_depth': [25, 30, None],
        'min_samples_split': [5, 10],
        'min_samples_leaf': [3, 5],
        'max_features': ['sqrt', 'log2', 0.8],
        'bootstrap': [True]
    }

    cv = StratifiedKFold(n_splits=3, shuffle=True, random_state=42)  # Fewer folds for speed

    grid_search = GridSearchCV(
        RandomForestClassifier(random_state=42, class_weight='balanced', n_jobs=-1),
        param_grid,
        cv=cv,
        scoring='accuracy',  # Focus on accuracy for confidence
        n_jobs=1,  # Avoid nested parallelism
        verbose=2
    )

    grid_search.fit(X, y)

    logger.info(f"Best parameters: {grid_search.best_params_}")
    logger.info(f"Best cross-validation score: {grid_search.best_score_:.4f}")

    return grid_search.best_estimator_, grid_search.best_params_

def cross_validate_model(model, X, y):
    """Enhanced cross-validation"""
    logger.info("Performing cross-validation...")

    cv = StratifiedKFold(n_splits=3, shuffle=True, random_state=42)

    cv_accuracy = cross_val_score(model, X, y, cv=cv, scoring='accuracy')
    cv_precision = cross_val_score(model, X, y, cv=cv, scoring='precision_weighted')
    cv_recall = cross_val_score(model, X, y, cv=cv, scoring='recall_weighted')
    cv_f1 = cross_val_score(model, X, y, cv=cv, scoring='f1_weighted')

    logger.info(f"Cross-validation results:")
    logger.info(f"  Accuracy:  {cv_accuracy.mean():.4f} ± {cv_accuracy.std():.4f}")
    logger.info(f"  Precision: {cv_precision.mean():.4f} ± {cv_precision.std():.4f}")
    logger.info(f"  Recall:    {cv_recall.mean():.4f} ± {cv_recall.std():.4f}")
    logger.info(f"  F1-score:  {cv_f1.mean():.4f} ± {cv_f1.std():.4f}")

    return {
        'accuracy': (cv_accuracy.mean(), cv_accuracy.std()),
        'precision': (cv_precision.mean(), cv_precision.std()),
        'recall': (cv_recall.mean(), cv_recall.std()),
        'f1': (cv_f1.mean(), cv_f1.std())
    }

def create_and_save_enhanced_model():
    """Create and save the massively enhanced model"""
    logger.info("Loading questions data...")
    questions_df = get_questions_data()

    logger.info(f"Training model with {len(questions_df)} questions")
    logger.info("Generating MASSIVE training data (1,000,000 samples)...")

    # Create enhanced training data
    X, y, feature_columns, question_keys = create_enhanced_training_data(
        questions_df,
        n_samples=1000000  # 1 MILLION samples
    )

    # Train label encoder
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)

    logger.info(f"Label encoder classes: {len(label_encoder.classes_)}")

    # Hyperparameter tuning
    best_model, best_params = hyperparameter_tuning(X, y_encoded)

    # Cross-validation
    cv_results = cross_validate_model(best_model, X, y_encoded)

    # Train final model
    logger.info("Training final Random Forest model with best parameters...")
    final_model = RandomForestClassifier(**best_params, random_state=42, n_jobs=-1)
    final_model.fit(X, y_encoded)

    # Calculate feature importance
    feature_importance = dict(sorted(
        zip(feature_columns, final_model.feature_importances_),
        key=lambda x: x[1],
        reverse=True
    )[:20])  # Top 20 features

    logger.info(f"Final model trained with {len(final_model.estimators_)} trees")
    logger.info(f"Top 5 features: {list(feature_importance.keys())[:5]}")

    # Prepare model data
    model_data = {
        'model': final_model,
        'feature_columns': feature_columns,
        'label_encoder': label_encoder,
        'all_questions': question_keys,
        'questions_df': questions_df.to_dict('records'),
        'best_params': best_params,
        'cv_results': cv_results,
        'feature_importance': feature_importance,
        'training_info': {
            'n_samples': len(X),
            'feature_count': len(feature_columns),
            'target_classes': len(label_encoder.classes_),
            'n_estimators': len(final_model.estimators_),
            'model_type': 'RandomForest'
        }
    }

    # Save model
    model_dir = Path(r"C:\afyacheck\python-service\decision_tree_model")
    model_dir.mkdir(parents=True, exist_ok=True)
    model_path = model_dir / "enhanced_sti_question_tree_model.joblib"

    joblib.dump(model_data, model_path)
    logger.info(f"Enhanced model saved to: {model_path}")

    # Final diagnostics
    logger.info("\n=== FINAL MODEL DIAGNOSTICS ===")
    logger.info(f"Questions in encoder: {len(label_encoder.classes_)}")
    logger.info(f"Features in model: {len(feature_columns)}")
    logger.info(f"Training accuracy: {final_model.score(X, y_encoded):.4f}")
    logger.info(f"Best parameters: {best_params}")
    logger.info(f"Cross-validation Accuracy: {cv_results['accuracy'][0]:.4f} ± {cv_results['accuracy'][1]:.4f}")
    logger.info(f"Cross-validation F1: {cv_results['f1'][0]:.4f} ± {cv_results['f1'][1]:.4f}")

    # Check coverage
    missing_in_encoder = set(question_keys) - set(label_encoder.classes_)
    if missing_in_encoder:
        logger.warning(f"Questions missing in encoder: {missing_in_encoder}")
    else:
        logger.info("✓ All questions are included in the label encoder")

    logger.info("=== TRAINING COMPLETE ===")

if __name__ == "__main__":
    create_and_save_enhanced_model()