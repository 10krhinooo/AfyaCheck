# Pydantic request/response schemas for the question sequencing API.
from pydantic import BaseModel
from typing import Any, Dict, List, Optional


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
    error: Optional[str] = None
