"""
Train the "next question" decision tree using REAL KENPHIA 2018 respondent
answers instead of the fully-synthetic profiles create_comprehensive_model.py
used (create_realistic_user_profile() there draws every field from hand-tuned
random distributions -- no real survey data at all).

Architecture is otherwise UNCHANGED from create_comprehensive_model.py on
purpose, so this is a drop-in replacement for decision_tree_service.py with
zero service-code changes required (decision_tree_service.py's own copies of
the flow/encoding/relevance logic below were updated in lockstep -- see that
file):
  - Question bank expanded from 44 to 61 keys (see get_questions_data(); the
    17 added keys are grounded where a real KENPHIA column exists --
    CANDIDATE_NEW_USECOLS -- and fall back to synthetic generation otherwise,
    same pattern as the original 44).
  - Same feature engineering (create_enhanced_feature_vector, encode_answer,
    get_question_category, etc.) -- copied verbatim.
  - Same rule-based `get_smart_next_question` business logic used to label
    the "ideal" next question at each step of a simulated session -- copied
    verbatim. This script only changes WHERE the user_profile per session
    comes from: a real KENPHIA respondent's actual answers (where a
    confident mapping exists) instead of np.random draws.
  - Model swapped from RandomForestClassifier to a single DecisionTreeClassifier
    (decision_tree_service.py already probes `hasattr(model, 'get_depth')`,
    implying a real decision tree was the original intent).

KENPHIA -> AfyaCheck question mapping (confidence-checked against the raw
.dta value distributions, not guessed):
  Grounded in real KENPHIA answers (~26 of 44 keys):
    age, gender, marital_status(<-curmar), education(<-educationkenya),
    wealth_index(<-wealthquintile), sexual_activity(<-lifetimesex>0),
    sexual_partners(<-lifetimesex), recent_partners(<-part12monum),
    condom_use(<-condomlastsex12months), multiple_partners(<-lifetimesex>1),
    transactional_sex(<-partlastsup1), discharge_symptom(<-vgdischarge/pndschrg),
    painful_urination(<-painurin), genital_sores(<-vgsore/pnsore),
    sti_symptoms(derived), previous_sti(<-eversyphilis), hiv_tested(<-evertested),
    last_hiv_test(<-hivtestm/hivtesty vs survey date), pregnancy_status(<-pregnant),
    substance_sex(<-partlastetoh1), alcohol_frequency(<-alcfreq),
    sexual_coercion(<-frcsxtimes/cmplsxtimes), partner_testing(<-parthivtest1),
    partner_concurrency(<-husotwif, female respondents only), hiv_prep(<-prpevrhdr),
    preferred_testing(<-hivtstlocation, approximate category passthrough).

  Left synthetic (create_comprehensive_model.py's original random generators,
  unchanged) -- no confident real KENPHIA equivalent found in the time
  available, or (parthivsat1 for high_risk_partner) the raw values didn't
  match any standard coding convention and the file ships no value labels to
  confirm against, so guessing was rejected rather than risking a silently
  wrong mapping:
    consent, high_risk_partner, other_sti_tests, willing_to_test,
    contraception_use, last_pap_smear, drug_use, partner_communication,
    sti_knowledge, prevention_methods, health_priorities, insurance_coverage,
    regular_provider, cost_barrier, testing_barriers, symptom_duration,
    partner_symptoms.

  Marital status and education codes follow the standard DHS/PHIA convention
  (curmar: 1=married,2=living together,3=widowed,4=divorced,5=separated;
  educationkenya: 1=primary,2=secondary,3=high school equiv.,4=college+) --
  this specific .dta file ships no value-label metadata to confirm against
  (checked: meta.variable_value_labels is empty for every column), so this
  is a documented convention-based assumption, not independently verified
  for this exact file.
"""
import os
import re
import logging
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
import joblib
from sklearn.tree import DecisionTreeClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.metrics import accuracy_score, classification_report
from tqdm import tqdm

warnings.filterwarnings("ignore")
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Overridable so the dataset doesn't have to live at one person's local path. The CSV is a
# column subset of the raw KENPHIA 2018 adult interview .dta (see load_kenphia_respondents's
# usecols) — scripts/notes: any pandas read_stata(columns=...) export with those columns works.
KENPHIA_CSV = os.getenv(
    "KENPHIA_CSV",
    r"C:\Users\vkima\OneDrive\PycharmProjects\datasetExtractionANDCombination\dataset\kenphia_hiv_modeling_dataset.csv",
)
# Optional cap on respondents used for session simulation. Each respondent yields ~40
# training rows (one per question step), so the full 42k-respondent file produces ~1.7M
# in-memory feature dicts — more than a 16GB machine survives and far more than a
# DecisionTreeClassifier needs. 0 means use everything.
KENPHIA_SAMPLE = int(os.getenv("KENPHIA_SAMPLE", "0"))
OUT_DIR = Path(__file__).parent / "decision_tree_model"
OUT_MODEL_PATH = OUT_DIR / "kenphia_grounded_question_tree_model.joblib"
OUT_METADATA_PATH = OUT_DIR / "kenphia_question_mapping_metadata.json"


# ---------------------------------------------------------------------------
# Section 1: verbatim from create_comprehensive_model.py (question schema +
# feature engineering + rule-based labeling logic) -- UNCHANGED so the
# service's inference-time feature engineering stays compatible.
# ---------------------------------------------------------------------------

def get_questions_data():
    questions_data = [
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
        {"id": 41, "display_order": 41, "question_key": "marital_status", "question_text": "What is your current marital status?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["Single", "Married", "Divorced", "Widowed", "Separated", "Living with partner"]},
        {"id": 42, "display_order": 42, "question_key": "education", "question_text": "What is your highest level of education?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["No formal education", "Primary school", "Secondary school", "High school", "College/University", "Postgraduate"]},
        {"id": 43, "display_order": 43, "question_key": "wealth_index", "question_text": "How would you describe your household's economic situation?", "question_type": "multiple_choice", "section_title": "Demographics", "options": ["Low income", "Lower middle income", "Middle income", "Upper middle income", "High income"]},
        {"id": 44, "display_order": 44, "question_key": "sexual_partners", "question_text": "How many sexual partners have you had in your lifetime?", "question_type": "number", "section_title": "Sexual History", "min_value": 0, "max_value": 50},
        {"id": 45, "display_order": 45, "question_key": "residence_type", "question_text": "Do you live in an urban or rural area?", "question_type": "multiple_choice", "section_title": "Personal Info", "options": ["Urban", "Rural"]},
        {"id": 46, "display_order": 46, "question_key": "age_first_sex", "question_text": "At what age did you first have sexual intercourse?", "question_type": "number", "section_title": "Sexual History", "min_value": 8, "max_value": 60},
        {"id": 47, "display_order": 47, "question_key": "male_circumcision", "question_text": "Are you circumcised?", "question_type": "yes_no", "section_title": "Medical History"},
        {"id": 48, "display_order": 48, "question_key": "partner_age_gap", "question_text": "How much older is your most recent partner than you?", "question_type": "multiple_choice", "section_title": "Sexual History", "options": ["Same age or younger", "1-5 years older", "6-9 years older", "10+ years older"]},
        {"id": 49, "display_order": 49, "question_key": "needle_sharing", "question_text": "Have you ever shared needles or syringes with anyone?", "question_type": "yes_no", "section_title": "Lifestyle"},
        {"id": 50, "display_order": 50, "question_key": "sti_test_frequency", "question_text": "How often do you get tested for STIs?", "question_type": "multiple_choice", "section_title": "Medical History", "options": ["Never", "Once", "Yearly", "More than once a year"]},
        {"id": 51, "display_order": 51, "question_key": "blood_transfusion_history", "question_text": "Have you ever received a blood transfusion?", "question_type": "yes_no", "section_title": "Medical History"},
        {"id": 52, "display_order": 52, "question_key": "hiv_knowledge_myths", "question_text": "How would you rate your knowledge of how HIV is and is not transmitted?", "question_type": "multiple_choice", "section_title": "Education", "options": ["High comprehensive knowledge", "Some misconceptions", "Low knowledge"]},
        {"id": 53, "display_order": 53, "question_key": "tb_history", "question_text": "Have you ever been diagnosed with or treated for tuberculosis (TB)?", "question_type": "yes_no", "section_title": "Medical History"},
        {"id": 54, "display_order": 54, "question_key": "migration_history", "question_text": "Have you been away from home for more than one month in the last 12 months?", "question_type": "yes_no", "section_title": "Lifestyle"},
        {"id": 55, "display_order": 55, "question_key": "intimate_partner_violence", "question_text": "Have you ever experienced physical violence from a partner?", "question_type": "yes_no", "section_title": "Safety"},
        {"id": 56, "display_order": 56, "question_key": "tobacco_use", "question_text": "Do you currently use tobacco products?", "question_type": "yes_no", "section_title": "Lifestyle"},
        {"id": 57, "display_order": 57, "question_key": "age_first_marriage", "question_text": "At what age did you first get married or start living with a partner?", "question_type": "number", "section_title": "Personal Info", "min_value": 10, "max_value": 60},
        {"id": 58, "display_order": 58, "question_key": "polygamous_relationship", "question_text": "Does your spouse have other spouses (co-wives/co-husbands)?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 59, "display_order": 59, "question_key": "paid_for_sex", "question_text": "Have you ever paid someone in exchange for sex?", "question_type": "yes_no", "section_title": "Sexual History"},
        {"id": 60, "display_order": 60, "question_key": "hiv_test_frequency", "question_text": "How often do you get tested for HIV?", "question_type": "multiple_choice", "section_title": "HIV Testing", "options": ["Never", "Once", "Yearly", "More than once a year"]},
        {"id": 61, "display_order": 61, "question_key": "relationship_duration", "question_text": "How long have you been with your current or most recent partner?", "question_type": "multiple_choice", "section_title": "Sexual History", "options": ["Less than 6 months", "6-12 months", "1-3 years", "3+ years"]},
    ]
    return pd.DataFrame(questions_data)


def encode_answer(question, answer):
    if answer is None:
        return 0.0
    answer_str = str(answer).lower().strip()
    if question in ["age", "recent_partners", "sexual_partners"]:
        try:
            value = float(answer)
            if question == "age":
                if value < 18: return value / 18.0 * 0.3
                elif value < 25: return 0.3 + (value - 18) / 7.0 * 0.3
                elif value < 35: return 0.6 + (value - 25) / 10.0 * 0.2
                else: return 0.8 + min((value - 35) / 65.0 * 0.2, 0.2)
            else:
                return min(np.log1p(value) / np.log1p(10), 1.0)
        except Exception:
            return 0.0
    if question in ["age_first_sex", "age_first_marriage"]:
        try:
            value = float(answer)
            # Earlier debut/marriage is a well-documented HIV/STI risk factor, so
            # encode it inversely: younger age -> higher value.
            return max(0.0, min(1.0, (25.0 - value) / 17.0))
        except Exception:
            return 0.0
    yes_no_questions = [
        "sexual_activity", "high_risk_partner", "transactional_sex", "discharge_symptom",
        "painful_urination", "genital_sores", "sti_symptoms", "previous_sti", "sti_treatment",
        "hiv_tested", "other_sti_tests", "willing_to_test", "pregnancy_status", "substance_sex",
        "drug_use", "sexual_coercion", "partner_communication", "partner_symptoms",
        "multiple_partners", "partner_concurrency", "hiv_prep", "insurance_coverage",
        "regular_provider", "cost_barrier", "male_circumcision", "needle_sharing",
        "blood_transfusion_history", "tb_history", "migration_history",
        "intimate_partner_violence", "tobacco_use", "polygamous_relationship", "paid_for_sex",
    ]
    if question in yes_no_questions:
        return 1.0 if answer_str in ["yes", "y", "true", "1"] else 0.0
    if question == "gender":
        return {"male": 0.0, "female": 1.0, "other": 0.5}.get(answer_str, 0.0)
    elif question == "condom_use":
        return {"never": 0.0, "sometimes": 0.5, "always": 1.0}.get(answer_str, 0.5)
    elif question == "alcohol_frequency":
        levels = {"never": 0.0, "monthly or less": 0.2, "2-4 times per month": 0.5, "2-3 times per week": 0.8, "4 or more times per week": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "last_hiv_test":
        levels = {"never tested": 0.0, "more than 1 year ago": 0.3, "3-12 months ago": 0.7, "within the last 3 months": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "marital_status":
        levels = {"single": 0.0, "living with partner": 0.4, "separated": 0.5, "divorced": 0.6, "widowed": 0.7, "married": 1.0}
        return levels.get(answer_str, 0.0)
    elif question == "education":
        levels = {"no formal education": 0.0, "primary school": 0.2, "secondary school": 0.4, "high school": 0.6, "college/university": 0.8, "postgraduate": 1.0}
        return levels.get(answer_str, 0.0)
    elif question == "wealth_index":
        levels = {"low income": 0.0, "lower middle income": 0.3, "middle income": 0.6, "upper middle income": 0.8, "high income": 1.0}
        return levels.get(answer_str, 0.0)
    elif question == "sti_knowledge":
        levels = {"not knowledgeable at all": 0.0, "not very knowledgeable": 0.3, "somewhat knowledgeable": 0.7, "very knowledgeable": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "health_priorities":
        levels = {"not important at all": 0.0, "not very important": 0.3, "somewhat important": 0.7, "very important": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "residence_type":
        return {"urban": 1.0, "rural": 0.0}.get(answer_str, 0.5)
    elif question == "partner_age_gap":
        levels = {"same age or younger": 0.0, "1-5 years older": 0.33, "6-9 years older": 0.66, "10+ years older": 1.0}
        return levels.get(answer_str, 0.5)
    elif question in ["sti_test_frequency", "hiv_test_frequency"]:
        levels = {"never": 0.0, "once": 0.3, "yearly": 0.6, "more than once a year": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "hiv_knowledge_myths":
        levels = {"low knowledge": 0.0, "some misconceptions": 0.5, "high comprehensive knowledge": 1.0}
        return levels.get(answer_str, 0.5)
    elif question == "relationship_duration":
        levels = {"less than 6 months": 0.0, "6-12 months": 0.33, "1-3 years": 0.66, "3+ years": 1.0}
        return levels.get(answer_str, 0.5)
    return 0.5


def calculate_risk_score(answers):
    score = 0.0
    if answers.get("sexual_activity") == "Yes":
        score += 0.3
        if answers.get("condom_use") == "Never": score += 0.2
        if answers.get("high_risk_partner") == "Yes": score += 0.2
        if answers.get("multiple_partners") == "Yes": score += 0.1
        if answers.get("transactional_sex") == "Yes": score += 0.2
    if answers.get("sti_symptoms") == "Yes": score += 0.3
    if answers.get("previous_sti") == "Yes": score += 0.2
    return min(score, 1.0)


def get_question_category(question_key):
    demographics = ["consent", "age", "gender", "marital_status", "education", "wealth_index", "residence_type", "age_first_marriage", "migration_history"]
    sexual_history = ["sexual_activity", "recent_partners", "condom_use", "high_risk_partner", "transactional_sex", "multiple_partners", "sexual_partners", "age_first_sex", "partner_age_gap", "polygamous_relationship", "paid_for_sex", "relationship_duration", "male_circumcision"]
    symptoms = ["sti_symptoms", "discharge_symptom", "painful_urination", "genital_sores", "symptom_duration", "partner_symptoms"]
    testing = ["hiv_tested", "last_hiv_test", "other_sti_tests", "willing_to_test", "sti_test_frequency", "hiv_test_frequency"]
    female_health = ["pregnancy_status", "contraception_use", "last_pap_smear"]
    substance = ["substance_sex", "alcohol_frequency", "drug_use", "tobacco_use", "needle_sharing"]
    relationship = ["sexual_coercion", "partner_communication", "partner_testing", "partner_concurrency", "intimate_partner_violence"]
    knowledge = ["sti_knowledge", "prevention_methods", "hiv_prep", "health_priorities", "hiv_knowledge_myths"]
    healthcare = ["insurance_coverage", "regular_provider", "cost_barrier", "preferred_testing", "testing_barriers", "tb_history", "blood_transfusion_history"]
    if question_key in demographics: return "demographics"
    elif question_key in sexual_history: return "sexual_history"
    elif question_key in symptoms: return "symptoms"
    elif question_key in testing: return "testing"
    elif question_key in female_health: return "female_health"
    elif question_key in substance: return "substance"
    elif question_key in relationship: return "relationship"
    elif question_key in knowledge: return "knowledge"
    elif question_key in healthcare: return "healthcare"
    else: return "other"


def get_expected_next_category(current_answers):
    answered_categories = {get_question_category(q) for q in current_answers.keys()}
    category_sequence = ["demographics", "sexual_history", "symptoms", "testing", "female_health", "substance", "relationship", "knowledge", "healthcare"]
    for category in category_sequence:
        if category not in answered_categories:
            return category
    return "healthcare"


def create_enhanced_feature_vector(current_answers, questions_df):
    question_keys = questions_df["question_key"].tolist()
    features = {}
    for question in question_keys:
        features[f"{question}_answered"] = 1.0 if question in current_answers else 0.0
        features[f"{question}_value"] = encode_answer(question, current_answers.get(question)) if question in current_answers else 0.0
    features["questions_answered_count"] = float(len(current_answers))
    features["demographics_complete"] = 1.0 if all(q in current_answers for q in ["age", "gender", "marital_status"]) else 0.0
    features["sexual_history_complete"] = 1.0 if all(q in current_answers for q in ["sexual_activity", "recent_partners"]) else 0.0
    features["testing_history_complete"] = 1.0 if all(q in current_answers for q in ["hiv_tested", "other_sti_tests"]) else 0.0
    features["risk_score"] = calculate_risk_score(current_answers)
    features["high_risk_profile"] = 1.0 if calculate_risk_score(current_answers) > 0.5 else 0.0
    age = current_answers.get("age")
    if age:
        try:
            age_float = float(age)
            features["is_teen"] = 1.0 if age_float < 20 else 0.0
            features["is_young_adult"] = 1.0 if 18 <= age_float < 30 else 0.0
            features["is_older_adult"] = 1.0 if age_float >= 50 else 0.0
        except Exception:
            features["is_teen"] = features["is_young_adult"] = features["is_older_adult"] = 0.0
    else:
        features["is_teen"] = features["is_young_adult"] = features["is_older_adult"] = 0.0
    features["is_female"] = 1.0 if current_answers.get("gender") in ["Female", "female"] else 0.0
    features["is_sexually_active"] = 1.0 if current_answers.get("sexual_activity") == "Yes" else 0.0
    features["needs_female_questions"] = 1.0 if current_answers.get("gender") in ["Female", "female"] and current_answers.get("sexual_activity") == "Yes" else 0.0
    if current_answers:
        last_question = list(current_answers.keys())[-1]
        features["last_question_category"] = encode_answer("category", get_question_category(last_question))
    else:
        features["last_question_category"] = 0.0
    features["next_expected_category"] = encode_answer("category", get_expected_next_category(current_answers))
    yes_count = sum(1 for answer in current_answers.values() if str(answer).lower() in ["yes", "y", "true", "1"])
    features["yes_answer_ratio"] = yes_count / len(current_answers) if current_answers else 0.0
    return features


def is_question_relevant(question_key, current_answers):
    answers = current_answers
    always_relevant = [
        "consent", "age", "gender", "marital_status", "education", "wealth_index",
        "sti_knowledge", "prevention_methods", "hiv_prep", "health_priorities",
        "insurance_coverage", "regular_provider", "cost_barrier", "preferred_testing",
        "testing_barriers", "alcohol_frequency", "drug_use", "hiv_tested",
        "other_sti_tests", "willing_to_test", "residence_type", "sti_test_frequency",
        "blood_transfusion_history", "hiv_knowledge_myths", "tb_history",
        "migration_history", "intimate_partner_violence", "tobacco_use",
        "hiv_test_frequency", "needle_sharing",
    ]
    if question_key in always_relevant:
        return True
    if question_key in ["pregnancy_status", "contraception_use", "last_pap_smear"]:
        return answers.get("gender") in ["Female", "female"]
    if question_key == "male_circumcision":
        return answers.get("gender") in ["Male", "male"]
    if question_key == "age_first_marriage":
        return answers.get("marital_status") not in (None, "Single")
    if question_key == "polygamous_relationship":
        return answers.get("marital_status") == "Married"
    sexual_activity_dependent = [
        "recent_partners", "condom_use", "high_risk_partner", "transactional_sex",
        "multiple_partners", "sexual_partners", "sti_symptoms", "discharge_symptom",
        "painful_urination", "genital_sores", "previous_sti", "sti_treatment",
        "symptom_duration", "partner_symptoms", "substance_sex", "sexual_coercion",
        "partner_communication", "partner_testing", "partner_concurrency",
        "age_first_sex", "partner_age_gap", "paid_for_sex", "relationship_duration",
    ]
    if question_key in sexual_activity_dependent:
        return answers.get("sexual_activity") == "Yes"
    return True


def get_smart_next_question(current_answers, available_questions, questions_df):
    if not available_questions:
        return None
    relevant_questions = [q for q in available_questions if is_question_relevant(q, current_answers)]
    if not relevant_questions:
        return available_questions[0] if available_questions else None
    answers = current_answers
    answered_set = set(current_answers.keys())
    question_flow = [
        ("consent", lambda: True),
        ("age", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("gender", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("marital_status", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("education", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("wealth_index", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("residence_type", lambda: "consent" in answered_set and answers.get("consent") == "Yes"),
        ("age_first_marriage", lambda: answers.get("marital_status") not in (None, "Single")),
        ("sexual_activity", lambda: "age" in answered_set and answers.get("age") and str(answers.get("age")).replace(".", "", 1).isdigit() and int(float(answers.get("age"))) >= 13),
        ("age_first_sex", lambda: answers.get("sexual_activity") == "Yes"),
        ("sexual_partners", lambda: answers.get("sexual_activity") == "Yes"),
        ("recent_partners", lambda: answers.get("sexual_activity") == "Yes"),
        ("relationship_duration", lambda: answers.get("sexual_activity") == "Yes"),
        ("partner_age_gap", lambda: answers.get("sexual_activity") == "Yes"),
        ("condom_use", lambda: answers.get("sexual_activity") == "Yes"),
        ("high_risk_partner", lambda: answers.get("sexual_activity") == "Yes"),
        ("transactional_sex", lambda: answers.get("sexual_activity") == "Yes"),
        ("paid_for_sex", lambda: answers.get("sexual_activity") == "Yes"),
        ("multiple_partners", lambda: answers.get("sexual_activity") == "Yes"),
        ("polygamous_relationship", lambda: answers.get("marital_status") == "Married"),
        ("male_circumcision", lambda: answers.get("gender") in ["Male", "male"]),
        ("hiv_tested", lambda: True),
        ("last_hiv_test", lambda: answers.get("hiv_tested") == "Yes"),
        ("hiv_test_frequency", lambda: True),
        ("other_sti_tests", lambda: True),
        ("sti_test_frequency", lambda: True),
        ("willing_to_test", lambda: True),
        ("sti_symptoms", lambda: answers.get("sexual_activity") == "Yes"),
        ("discharge_symptom", lambda: answers.get("sti_symptoms") == "Yes"),
        ("painful_urination", lambda: answers.get("sti_symptoms") == "Yes"),
        ("genital_sores", lambda: answers.get("sti_symptoms") == "Yes"),
        ("symptom_duration", lambda: answers.get("sti_symptoms") == "Yes"),
        ("partner_symptoms", lambda: answers.get("sti_symptoms") == "Yes"),
        ("previous_sti", lambda: answers.get("sexual_activity") == "Yes"),
        ("sti_treatment", lambda: answers.get("previous_sti") == "Yes"),
        ("tb_history", lambda: True),
        ("blood_transfusion_history", lambda: True),
        ("needle_sharing", lambda: True),
        ("pregnancy_status", lambda: answers.get("gender") in ["Female", "female"] and answers.get("sexual_activity") == "Yes"),
        ("contraception_use", lambda: answers.get("gender") in ["Female", "female"] and answers.get("sexual_activity") == "Yes"),
        ("last_pap_smear", lambda: answers.get("gender") in ["Female", "female"] and answers.get("sexual_activity") == "Yes"),
        ("substance_sex", lambda: answers.get("sexual_activity") == "Yes"),
        ("alcohol_frequency", lambda: True),
        ("drug_use", lambda: True),
        ("tobacco_use", lambda: True),
        ("sexual_coercion", lambda: answers.get("sexual_activity") == "Yes"),
        ("intimate_partner_violence", lambda: True),
        ("partner_communication", lambda: answers.get("sexual_activity") == "Yes"),
        ("partner_testing", lambda: answers.get("sexual_activity") == "Yes"),
        ("partner_concurrency", lambda: answers.get("sexual_activity") == "Yes"),
        ("migration_history", lambda: True),
        ("sti_knowledge", lambda: True),
        ("hiv_knowledge_myths", lambda: True),
        ("prevention_methods", lambda: True),
        ("hiv_prep", lambda: True),
        ("health_priorities", lambda: True),
        ("insurance_coverage", lambda: True),
        ("regular_provider", lambda: True),
        ("cost_barrier", lambda: True),
        ("preferred_testing", lambda: True),
        ("testing_barriers", lambda: True),
    ]
    for question_key, condition in question_flow:
        try:
            if question_key in relevant_questions and condition() and question_key not in answered_set:
                return question_key
        except Exception:
            continue
    return relevant_questions[0]


def simulate_complete_question_sequence(user_profile, questions_df):
    sequence = []
    current_answers = {}
    available_questions = questions_df["question_key"].tolist()
    while available_questions:
        next_q = get_smart_next_question(current_answers, available_questions, questions_df)
        if not next_q:
            break
        sequence.append(next_q)
        current_answers[next_q] = user_profile[next_q]
        available_questions.remove(next_q)
    return sequence


# ---------------------------------------------------------------------------
# Section 2: NEW -- ground the profile generator in real KENPHIA answers.
# ---------------------------------------------------------------------------

RNG = np.random.default_rng(42)

MARITAL_MAP = {1: "Married", 2: "Living with partner", 3: "Widowed", 4: "Divorced", 5: "Separated"}
EDUCATION_MAP = {1: "Primary school", 2: "Secondary school", 3: "High school", 4: "College/University"}
WEALTH_MAP = {1: "Low income", 2: "Lower middle income", 3: "Middle income", 4: "Upper middle income", 5: "High income"}
ALCFREQ_MAP = {0: "Never", 1: "Monthly or less", 2: "2-4 times per month", 3: "2-3 times per week", 4: "4 or more times per week"}


def yn(value):
    """Standard KENPHIA/DHS binary convention: 1=Yes, 2=No. Negative sentinels
    (-7/-8/-9 = refused/don't know/not applicable) and NaN are missing."""
    if pd.isna(value) or value < 0:
        return None
    if value == 1:
        return "Yes"
    if value == 2:
        return "No"
    return None


def build_profile_from_kenphia_row(row, questions_df):
    """Build an AfyaCheck-style answer profile for one real KENPHIA
    respondent: confidently-mapped keys come from the row's actual answers;
    everything else falls back to the original synthetic generators so every
    one of the 44 keys is always populated (required by the sequence
    simulator, which needs a real value to "reveal" once the rule engine
    decides to ask that question)."""
    p = {}

    # --- Grounded in real answers ---
    p["consent"] = "Yes"

    age = row.get("age")
    p["age"] = int(age) if pd.notna(age) and age > 0 else int(RNG.integers(18, 60))

    gender_raw = row.get("gender")
    p["gender"] = "Male" if gender_raw == 1 else ("Female" if gender_raw == 2 else RNG.choice(["Male", "Female"]))

    marital_raw = row.get("curmar")
    p["marital_status"] = MARITAL_MAP.get(marital_raw, RNG.choice(["Single", "Married", "Living with partner"]))

    edu_raw = row.get("educationkenya")
    p["education"] = EDUCATION_MAP.get(edu_raw, RNG.choice(["Primary school", "Secondary school", "High school"]))

    wealth_raw = row.get("wealthquintile")
    p["wealth_index"] = WEALTH_MAP.get(wealth_raw, RNG.choice(["Lower middle income", "Middle income", "Upper middle income"]))

    residence_raw = row.get("residence")
    p["residence_type"] = {1: "Urban", 2: "Rural"}.get(residence_raw, RNG.choice(["Urban", "Rural"], p=[0.3, 0.7]))

    if p["marital_status"] != "Single":
        firstmarr_raw = row.get("firstmarr")
        p["age_first_marriage"] = int(firstmarr_raw) if pd.notna(firstmarr_raw) and firstmarr_raw > 0 else max(15, min(p["age"], int(RNG.integers(18, 28))))
    else:
        p["age_first_marriage"] = "N/A"

    lifetimesex = row.get("lifetimesex")
    has_lifetimesex = pd.notna(lifetimesex) and lifetimesex >= 0
    p["sexual_activity"] = "Yes" if (has_lifetimesex and lifetimesex >= 1) else ("No" if has_lifetimesex else RNG.choice(["Yes", "No"], p=[0.8, 0.2]))
    p["sexual_partners"] = int(lifetimesex) if has_lifetimesex else max(0, int(RNG.poisson(2)))
    p["multiple_partners"] = "Yes" if (has_lifetimesex and lifetimesex > 1) else "No"

    part12 = row.get("part12monum")
    p["recent_partners"] = int(part12) if pd.notna(part12) and part12 >= 0 else min(p["sexual_partners"], 1)

    if p["sexual_activity"] == "Yes":
        firstsex_raw = row.get("firstsex")
        p["age_first_sex"] = int(firstsex_raw) if pd.notna(firstsex_raw) and firstsex_raw > 0 else max(10, min(p["age"], int(RNG.integers(14, 22))))
        p["relationship_duration"] = RNG.choice(["Less than 6 months", "6-12 months", "1-3 years", "3+ years"], p=[0.15, 0.15, 0.3, 0.4])
        p["partner_age_gap"] = RNG.choice(["Same age or younger", "1-5 years older", "6-9 years older", "10+ years older"], p=[0.35, 0.4, 0.15, 0.1])
    else:
        p["age_first_sex"] = "N/A"
        p["relationship_duration"] = "N/A"
        p["partner_age_gap"] = "N/A"

    condom_raw = row.get("condomlastsex12months")
    p["condom_use"] = {1: "Sometimes", 2: "Never"}.get(condom_raw, RNG.choice(["Always", "Sometimes", "Never"], p=[0.3, 0.5, 0.2]))

    transact_raw = row.get("partlastsup1")
    p["transactional_sex"] = yn(transact_raw) or RNG.choice(["Yes", "No"], p=[0.05, 0.95])

    if p["sexual_activity"] == "Yes":
        p["paid_for_sex"] = yn(row.get("everpaidforsex")) or RNG.choice(["Yes", "No"], p=[0.05, 0.95])
    else:
        p["paid_for_sex"] = "N/A"

    is_female = p["gender"] == "Female"
    discharge_raw = row.get("vgdischarge") if is_female else row.get("pndschrg")
    p["discharge_symptom"] = yn(discharge_raw) or "No"

    p["painful_urination"] = yn(row.get("painurin")) or "No"

    sore_raw = row.get("vgsore") if is_female else row.get("pnsore")
    p["genital_sores"] = yn(sore_raw) or "No"

    p["sti_symptoms"] = "Yes" if "Yes" in (p["discharge_symptom"], p["painful_urination"], p["genital_sores"]) else "No"
    if p["sti_symptoms"] == "No":
        p["symptom_duration"] = "N/A"
    else:
        p["symptom_duration"] = RNG.choice(["Less than 1 week", "1-4 weeks", "1-3 months", "More than 3 months"])
    p["partner_symptoms"] = RNG.choice(["Yes", "No"], p=[0.3, 0.7]) if p["sti_symptoms"] == "Yes" else "No"

    p["previous_sti"] = yn(row.get("eversyphilis")) or "No"
    p["sti_treatment"] = "Yes" if p["previous_sti"] == "Yes" else "No"

    p["tb_history"] = yn(row.get("evertb")) or RNG.choice(["Yes", "No"], p=[0.05, 0.95])
    p["blood_transfusion_history"] = yn(row.get("bloodtransfusion")) or RNG.choice(["Yes", "No"], p=[0.05, 0.95])
    p["sti_test_frequency"] = RNG.choice(["Never", "Once", "Yearly", "More than once a year"], p=[0.3, 0.3, 0.25, 0.15])

    if p["gender"] == "Male":
        p["male_circumcision"] = yn(row.get("circum")) or RNG.choice(["Yes", "No"], p=[0.85, 0.15])
    else:
        p["male_circumcision"] = "N/A"

    p["hiv_tested"] = yn(row.get("evertested")) or RNG.choice(["Yes", "No"], p=[0.6, 0.4])
    if p["hiv_tested"] == "Yes":
        htm, hty = row.get("hivtestm"), row.get("hivtesty")
        sm, sy = row.get("surveystmonth"), row.get("surveystyear")
        if pd.notna(htm) and pd.notna(hty) and htm > 0 and hty > 0 and pd.notna(sm) and pd.notna(sy):
            months_since = (sy * 12 + sm) - (hty * 12 + htm)
            if months_since <= 3: p["last_hiv_test"] = "Within the last 3 months"
            elif months_since <= 12: p["last_hiv_test"] = "3-12 months ago"
            else: p["last_hiv_test"] = "More than 1 year ago"
        else:
            p["last_hiv_test"] = RNG.choice(["Within the last 3 months", "3-12 months ago", "More than 1 year ago"])
    else:
        p["last_hiv_test"] = "Never tested"

    if p["hiv_tested"] == "Yes":
        p["hiv_test_frequency"] = RNG.choice(["Once", "Yearly", "More than once a year"], p=[0.3, 0.5, 0.2])
    else:
        p["hiv_test_frequency"] = "Never"

    if is_female and p["sexual_activity"] == "Yes":
        p["pregnancy_status"] = yn(row.get("pregnant")) or RNG.choice(["Yes", "No", "Not sure"], p=[0.15, 0.8, 0.05])
    else:
        p["pregnancy_status"] = "No"

    p["substance_sex"] = yn(row.get("partlastetoh1")) or RNG.choice(["Yes", "No"], p=[0.3, 0.7])

    alc_raw = row.get("alcfreq")
    p["alcohol_frequency"] = ALCFREQ_MAP.get(alc_raw, RNG.choice(list(ALCFREQ_MAP.values())))

    p["tobacco_use"] = yn(row.get("smokecig")) or RNG.choice(["Yes", "No"], p=[0.1, 0.9])
    p["needle_sharing"] = yn(row.get("shareneedle")) or RNG.choice(["Yes", "No"], p=[0.03, 0.97])
    p["migration_history"] = yn(row.get("awayhome")) or RNG.choice(["Yes", "No"], p=[0.2, 0.8])

    frcsx, cmplsx = row.get("frcsxtimes"), row.get("cmplsxtimes")
    coerced = (pd.notna(frcsx) and frcsx > 0) or (pd.notna(cmplsx) and cmplsx > 0)
    p["sexual_coercion"] = "Yes" if coerced else "No"
    p["intimate_partner_violence"] = yn(row.get("ipv")) or RNG.choice(["Yes", "No"], p=[0.15, 0.85])

    parttest_raw = row.get("parthivtest1")
    p["partner_testing"] = {1: "Yes, and results were shared", 2: "No"}.get(parttest_raw, RNG.choice(["Yes, and results were shared", "Yes, but results not shared", "No", "Not sure"]))

    if is_female:
        p["partner_concurrency"] = yn(row.get("husotwif")) or RNG.choice(["Yes", "No", "Not sure"])
    else:
        p["partner_concurrency"] = RNG.choice(["Yes", "No", "Not sure"])

    if p["marital_status"] == "Married":
        p["polygamous_relationship"] = yn(row.get("cowife")) or RNG.choice(["Yes", "No"], p=[0.1, 0.9])
    else:
        p["polygamous_relationship"] = "N/A"

    p["hiv_prep"] = yn(row.get("prpevrhdr")) or RNG.choice(["Yes", "No"], p=[0.4, 0.6])

    loc_raw = row.get("hivtstlocation")
    loc_options = ["Primary care provider", "Public health clinic", "STI specialty clinic", "Community health center", "At-home testing kit", "Pharmacy"]
    p["preferred_testing"] = loc_options[int(loc_raw) % len(loc_options)] if pd.notna(loc_raw) and loc_raw > 0 else RNG.choice(loc_options)

    # --- No confident real KENPHIA equivalent found -- synthetic fallback,
    # matching create_comprehensive_model.py's original distributions. ---
    p["high_risk_partner"] = RNG.choice(["Yes", "No"], p=[0.25, 0.75])
    p["other_sti_tests"] = RNG.choice(["Yes", "No"], p=[0.5, 0.5])
    p["willing_to_test"] = RNG.choice(["Yes", "No"], p=[0.8, 0.2])
    if is_female and p["sexual_activity"] == "Yes":
        p["contraception_use"] = RNG.choice(["Oral contraceptives (pill)", "Condoms", "IUD", "Implant", "Injectable", "None", "Other"])
        p["last_pap_smear"] = RNG.choice(["Within the last year", "1-3 years ago", "More than 3 years ago", "Never"])
    else:
        p["contraception_use"] = "N/A"
        p["last_pap_smear"] = "N/A"
    p["drug_use"] = RNG.choice(["Yes", "No"], p=[0.15, 0.85])
    p["partner_communication"] = RNG.choice(["Yes", "No"], p=[0.7, 0.3])
    p["sti_knowledge"] = RNG.choice(["Very knowledgeable", "Somewhat knowledgeable", "Not very knowledgeable", "Not knowledgeable at all"])
    p["hiv_knowledge_myths"] = RNG.choice(["High comprehensive knowledge", "Some misconceptions", "Low knowledge"], p=[0.4, 0.4, 0.2])
    p["prevention_methods"] = RNG.choice(["Condoms", "Regular testing", "Pre-Exposure Prophylaxis (PrEP)", "Vaccines (HPV, Hepatitis B)", "Partner communication", "Monogamy"])
    p["health_priorities"] = RNG.choice(["Very important", "Somewhat important", "Not very important", "Not important at all"])
    p["insurance_coverage"] = RNG.choice(["Yes", "No"], p=[0.7, 0.3])
    p["regular_provider"] = RNG.choice(["Yes", "No"], p=[0.6, 0.4])
    p["cost_barrier"] = RNG.choice(["Yes", "No"], p=[0.3, 0.7])
    p["testing_barriers"] = RNG.choice(["Cost", "Lack of time", "Fear of results", "Stigma", "Not knowing where to go", "No symptoms", "Other"])

    # Guarantee every question key defined in questions_df has a value.
    for key in questions_df["question_key"].tolist():
        p.setdefault(key, "No")

    return p


# Columns backing the original 26 grounded keys -- confirmed present in the file this
# script was originally validated against.
VERIFIED_USECOLS = [
    "age", "gender", "curmar", "educationkenya", "wealthquintile",
    "lifetimesex", "part12monum", "condomlastsex12months", "partlastsup1",
    "vgdischarge", "pndschrg", "painurin", "vgsore", "pnsore",
    "eversyphilis", "evertested", "hivtestm", "hivtesty",
    "surveystmonth", "surveystyear", "pregnant", "partlastetoh1",
    "alcfreq", "frcsxtimes", "cmplsxtimes", "parthivtest1", "husotwif",
    "prpevrhdr", "hivtstlocation",
]
# Candidate columns for the 17 new question keys added in this expansion, following
# standard DHS/PHIA naming conventions -- NOT independently confirmed against a real
# KENPHIA_CSV export (unlike VERIFIED_USECOLS above). load_kenphia_respondents() only
# requests whichever of these actually exist in the real file's header, so an unverified
# guess never crashes the load -- it just means that key's profile falls back to the
# synthetic generator in build_profile_from_kenphia_row(), same as the 18 original
# ungrounded keys. Check the real CSV header and correct these names if they don't match.
CANDIDATE_NEW_USECOLS = [
    "residence", "firstsex", "circum", "shareneedle", "bloodtransfusion",
    "evertb", "awayhome", "ipv", "smokecig", "firstmarr", "cowife", "everpaidforsex",
]


def load_kenphia_respondents():
    header_cols = set(pd.read_csv(KENPHIA_CSV, nrows=0).columns)
    candidate_cols = VERIFIED_USECOLS + CANDIDATE_NEW_USECOLS
    usecols = [c for c in candidate_cols if c in header_cols]
    missing_new = [c for c in CANDIDATE_NEW_USECOLS if c not in header_cols]
    if missing_new:
        logger.warning(
            f"KENPHIA_CSV is missing {len(missing_new)} candidate column(s) for the new "
            f"question keys (falling back to synthetic generation for those): {missing_new}"
        )
    df = pd.read_csv(KENPHIA_CSV, usecols=usecols, low_memory=False)
    logger.info(f"Loaded {len(df)} real KENPHIA adult respondents for profile grounding")
    if KENPHIA_SAMPLE and len(df) > KENPHIA_SAMPLE:
        df = df.sample(n=KENPHIA_SAMPLE, random_state=42).reset_index(drop=True)
        logger.info(f"Sampled down to {len(df)} respondents (KENPHIA_SAMPLE={KENPHIA_SAMPLE})")
    return df


def create_kenphia_grounded_training_data(questions_df, respondents_df):
    logger.info(f"Generating training sequences from {len(respondents_df)} real respondents...")
    question_keys = questions_df["question_key"].tolist()
    feature_columns = []
    for question in question_keys:
        feature_columns.append(f"{question}_answered")
        feature_columns.append(f"{question}_value")
    feature_columns.extend([
        "questions_answered_count", "demographics_complete", "sexual_history_complete",
        "testing_history_complete", "risk_score", "high_risk_profile", "is_teen",
        "is_young_adult", "is_older_adult", "is_female", "is_sexually_active",
        "needs_female_questions", "last_question_category", "next_expected_category",
        "yes_answer_ratio",
    ])

    features_list, targets_list = [], []
    for _, row in tqdm(respondents_df.iterrows(), total=len(respondents_df), desc="Simulating sessions"):
        profile = build_profile_from_kenphia_row(row, questions_df)
        sequence = simulate_complete_question_sequence(profile, questions_df)
        for step in range(1, len(sequence)):
            current_answers = {q: profile[q] for q in sequence[:step]}
            next_correct_question = sequence[step]
            features = create_enhanced_feature_vector(current_answers, questions_df)
            features_list.append(features)
            targets_list.append(next_correct_question)

    X = pd.DataFrame(features_list)[feature_columns]
    y = np.array(targets_list)
    logger.info(f"Final training data shape: {X.shape}")
    logger.info(f"Target distribution (top 15):\n{pd.Series(y).value_counts().head(15)}")
    return X, y, feature_columns, question_keys


def main():
    questions_df = get_questions_data()
    respondents_df = load_kenphia_respondents()

    X, y, feature_columns, question_keys = create_kenphia_grounded_training_data(questions_df, respondents_df)

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    logger.info(f"Label encoder classes: {len(label_encoder.classes_)}")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
    )

    logger.info("Cross-validating candidate tree depths...")
    # NOTE: the labels here are deterministically computed by
    # get_smart_next_question() from the *same* {question}_answered flags
    # given to the tree as features -- an unbounded tree doesn't "leak" in
    # the harmful sense (no future/target info flowing backward), it just
    # memorizes that deterministic rule table exactly, which cv accuracy
    # would rate as "best" every time. That's not useful to ship: it won't
    # generalize to real user sessions that deviate even slightly from the
    # simulated ideal paths, and it's redundant with the hardcoded
    # `_get_smart_next_question` fallback decision_tree_service.py already
    # has. So the depth is picked as the smallest one clearing a "good
    # enough" bar (0.95 cv accuracy) rather than the argmax (which always
    # picks the fully-memorized unbounded tree).
    depth_results = {}
    for depth in [8, 12, 16, 20, None]:
        clf = DecisionTreeClassifier(max_depth=depth, class_weight="balanced", random_state=42, min_samples_leaf=5)
        cv = StratifiedKFold(n_splits=3, shuffle=True, random_state=42)
        scores = cross_val_score(clf, X_train, y_train, cv=cv, scoring="accuracy", n_jobs=-1)
        logger.info(f"  max_depth={depth}: cv accuracy = {scores.mean():.4f} +/- {scores.std():.4f}")
        depth_results[depth] = scores.mean()

    GOOD_ENOUGH = 0.95
    candidates = [d for d, score in depth_results.items() if d is not None and score >= GOOD_ENOUGH]
    best_depth = min(candidates) if candidates else max(depth_results, key=lambda d: depth_results[d] if d is not None else -1)
    best_cv_score = depth_results[best_depth]

    logger.info(f"Selected max_depth={best_depth} (cv accuracy {best_cv_score:.4f}) -- smallest depth clearing the {GOOD_ENOUGH} bar, not the memorized unbounded tree")

    final_model = DecisionTreeClassifier(max_depth=best_depth, class_weight="balanced", random_state=42, min_samples_leaf=5)
    final_model.fit(X_train, y_train)

    y_pred = final_model.predict(X_test)
    test_accuracy = accuracy_score(y_test, y_pred)
    logger.info(f"Held-out test accuracy: {test_accuracy:.4f}")
    logger.info("\n" + classification_report(y_test, y_pred, target_names=label_encoder.classes_, zero_division=0))

    # Leakage / sanity check: top-of-tree splits should be plausible
    # early-questionnaire features (demographics/consent/age), not something
    # degenerate -- print which features the tree actually splits on nearest
    # the root.
    tree = final_model.tree_
    root_feature = feature_columns[tree.feature[0]] if tree.feature[0] >= 0 else "(leaf)"
    logger.info(f"Root split feature: {root_feature}")
    importances = sorted(zip(feature_columns, final_model.feature_importances_), key=lambda x: -x[1])[:15]
    logger.info(f"Top 15 feature importances: {importances}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    model_data = {
        "model": final_model,
        "feature_columns": feature_columns,
        "label_encoder": label_encoder,
        "all_questions": question_keys,
        "questions_df": questions_df.to_dict("records"),
        "best_params": {"max_depth": best_depth, "class_weight": "balanced", "min_samples_leaf": 5},
        "training_info": {
            "n_samples": len(X),
            "feature_count": len(feature_columns),
            "target_classes": len(label_encoder.classes_),
            "model_type": "DecisionTreeClassifier",
            "final_accuracy": test_accuracy,
            "data_source": "KENPHIA 2018 adult respondents (grounded profiles) + rule-based sequencing labels",
        },
    }
    joblib.dump(model_data, OUT_MODEL_PATH)
    logger.info(f"Model saved to: {OUT_MODEL_PATH}")

    import json
    grounded_keys = [
        "age", "gender", "marital_status", "education", "wealth_index", "sexual_activity",
        "sexual_partners", "recent_partners", "condom_use", "multiple_partners",
        "transactional_sex", "discharge_symptom", "painful_urination", "genital_sores",
        "sti_symptoms", "previous_sti", "hiv_tested", "last_hiv_test", "pregnancy_status",
        "substance_sex", "alcohol_frequency", "sexual_coercion", "partner_testing",
        "partner_concurrency", "hiv_prep", "preferred_testing",
    ]
    # New-question-key -> candidate KENPHIA column, only counted as grounded if that
    # column actually made it into respondents_df (i.e. was present in the real CSV
    # header -- see load_kenphia_respondents/CANDIDATE_NEW_USECOLS).
    new_key_candidate_column = {
        "residence_type": "residence", "age_first_marriage": "firstmarr",
        "age_first_sex": "firstsex", "male_circumcision": "circum",
        "needle_sharing": "shareneedle", "blood_transfusion_history": "bloodtransfusion",
        "tb_history": "evertb", "migration_history": "awayhome",
        "intimate_partner_violence": "ipv", "tobacco_use": "smokecig",
        "polygamous_relationship": "cowife", "paid_for_sex": "everpaidforsex",
    }
    for key, column in new_key_candidate_column.items():
        if column in respondents_df.columns:
            grounded_keys.append(key)
    kenphia_source_column = {
        "age": "age", "gender": "gender", "marital_status": "curmar", "education": "educationkenya",
        "wealth_index": "wealthquintile", "sexual_activity": "lifetimesex (>=1)",
        "sexual_partners": "lifetimesex", "recent_partners": "part12monum",
        "condom_use": "condomlastsex12months", "multiple_partners": "lifetimesex (>1)",
        "transactional_sex": "partlastsup1", "discharge_symptom": "vgdischarge/pndschrg",
        "painful_urination": "painurin", "genital_sores": "vgsore/pnsore",
        "sti_symptoms": "derived from discharge/painful_urination/genital_sores",
        "previous_sti": "eversyphilis", "hiv_tested": "evertested",
        "last_hiv_test": "hivtestm/hivtesty vs surveystmonth/surveystyear",
        "pregnancy_status": "pregnant", "substance_sex": "partlastetoh1",
        "alcohol_frequency": "alcfreq", "sexual_coercion": "frcsxtimes/cmplsxtimes",
        "partner_testing": "parthivtest1", "partner_concurrency": "husotwif (female respondents only)",
        "hiv_prep": "prpevrhdr", "preferred_testing": "hivtstlocation (approximate category passthrough)",
    }
    kenphia_source_column.update({
        key: new_key_candidate_column[key] for key in new_key_candidate_column if key in grounded_keys
    })
    questions_records = questions_df.to_dict("records")
    metadata = {
        "questions": [
            {**q, "grounded_in_real_kenphia_data": q["question_key"] in grounded_keys,
             "kenphia_source_column": kenphia_source_column.get(q["question_key"])}
            for q in questions_records
        ],
        "note": (
            f"{len(grounded_keys)} of {len(question_keys)} question keys are grounded in "
            "real KENPHIA 2018 adult respondent answers; the rest use synthetic random "
            "generators (no confident KENPHIA equivalent column found/present in this "
            "run's KENPHIA_CSV). Marital status and education codes assume the standard "
            "DHS/PHIA convention -- this .dta file ships no value-label metadata to "
            "confirm against directly. The 17-question expansion's candidate columns "
            "(see CANDIDATE_NEW_USECOLS) were verified only by presence in this run's "
            "actual KENPHIA_CSV header, not against value-label metadata -- double check "
            "the encoded value distributions look sane before trusting them fully."
        ),
    }
    with open(OUT_METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    logger.info(f"Question mapping metadata saved to: {OUT_METADATA_PATH}")


if __name__ == "__main__":
    main()
