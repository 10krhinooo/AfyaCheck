# save_model_compatible.py
import pandas as pd
import numpy as np
import joblib
from sklearn.tree import DecisionTreeClassifier
from sklearn.preprocessing import LabelEncoder
from pathlib import Path

def create_compatible_model():
    """Create and save a model in the format expected by the FastAPI service"""

    # Create sample data structure that matches FastAPI expectations
    model_data = {
        'model': None,  # This will be our decision tree
        'feature_columns': [],
        'label_encoder': None,
        'all_questions': []
    }

    # Define the questions from your original data
    questions = [
        "consent", "age", "gender", "sexual_activity", "recent_partners",
        "condom_use", "high_risk_partner", "transactional_sex", "discharge_symptom",
        "painful_urination", "genital_sores", "sti_symptoms", "previous_sti",
        "sti_treatment", "hiv_test", "last_hiv_test", "other_sti_tests",
        "willing_to_test", "pregnancy_status", "contraception_use",
        "last_pap_smear", "substance_sex", "alcohol_frequency", "drug_use",
        "sexual_coercion", "partner_communication", "partner_testing",
        "sti_knowledge", "hiv_prep", "insurance_coverage", "regular_provider",
        "cost_barrier"
    ]

    # Create feature columns as expected by FastAPI
    feature_columns = []
    for question in questions:
        feature_columns.append(f'{question}_answered')
        feature_columns.append(f'{question}_value')

    # Add derived features
    feature_columns.extend([
        'questions_answered_count',
        'is_young_adult',
        'is_older_adult'
    ])

    # Create a simple decision tree model
    # Generate synthetic training data
    np.random.seed(42)
    n_samples = 1000

    X = np.random.rand(n_samples, len(feature_columns))
    y = np.random.choice(questions, n_samples)

    # Train the model
    model = DecisionTreeClassifier(max_depth=10, random_state=42)
    model.fit(X, y)

    # Create label encoder
    label_encoder = LabelEncoder()
    label_encoder.fit(questions)

    # Prepare the model data
    model_data['model'] = model
    model_data['feature_columns'] = feature_columns
    model_data['label_encoder'] = label_encoder
    model_data['all_questions'] = questions

    return model_data

def save_compatible_model():
    """Save the model in the correct format"""
    model_dir = Path(r"C:\afyacheck\python-service\decision_tree_model")
    model_dir.mkdir(parents=True, exist_ok=True)

    model_path = model_dir / "sti_question_tree_model.joblib"

    print("Creating compatible model...")
    model_data = create_compatible_model()

    print("Saving model...")
    joblib.dump(model_data, model_path)

    print(f"Model saved successfully to: {model_path}")
    print(f"Model components:")
    print(f"  - Model type: {type(model_data['model'])}")
    print(f"  - Feature columns: {len(model_data['feature_columns'])}")
    print(f"  - Questions: {len(model_data['all_questions'])}")
    print(f"  - Tree depth: {model_data['model'].get_depth()}")

if __name__ == "__main__":
    save_compatible_model()