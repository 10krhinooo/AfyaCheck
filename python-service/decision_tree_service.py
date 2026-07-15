# fastapi_production_ready.py (Final Production Version with Enhanced Zero Encoding)
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import logging
from datetime import datetime
import os
from pathlib import Path
import warnings
warnings.filterwarnings('ignore', category=UserWarning)

from models import QuestionRequest, QuestionResponse
from question_sequencer import SmartQuestionnaire

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Configuration & Path Management ---

MODEL_DIR = os.getenv("MODEL_DIRECTORY", "decision_tree_model")
MODEL_FILENAME = os.getenv("MODEL_FILENAME", "cpu_optimized_sti_question_tree_model_100k.joblib")


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

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
            enough_info=prediction['enough_info']
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
