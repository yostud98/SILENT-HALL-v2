"""
Silent Hall — On-Demand Answer Explanation Generator
=====================================================
Generates detailed AI explanations for quiz answers using the Claude API.
Called on-demand when a student clicks "Explain with AI" on the result detail page.
NOTE: Explanations are generated per-question on request, NOT automatically after submission.

Usage:
    python explain.py --attempt-id 1 --token YOUR_JWT --api-key YOUR_ANTHROPIC_KEY

Or import and call generate_explanation() directly for a single question.
"""

import json
import argparse
import requests
from typing import Optional


ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
BACKEND_URL = "http://localhost:8080/api"
MODEL = "claude-sonnet-4-20250514"


def generate_explanation(
    question_text: str,
    option_a: str,
    option_b: str,
    option_c: str,
    option_d: str,
    correct_answer: str,  # "A", "B", "C", or "D"
    student_answer: Optional[str],  # "A", "B", "C", "D" or None
    course: str = "",
    api_key: str = ""
) -> dict:
    """
    Generate AI explanation for a quiz question answer.
    Returns dict with correctExplanation and wrongExplanation.
    """
    opt_map = {"A": option_a, "B": option_b, "C": option_c, "D": option_d}
    correct_text = opt_map.get(correct_answer, correct_answer)
    student_text = opt_map.get(student_answer, "Not answered") if student_answer else "Not answered"
    is_correct = student_answer and student_answer.upper() == correct_answer.upper()

    prompt = f"""You are an academic tutor at Silent Hall / UMAT (University of Mines and Technology, Ghana).
A student just completed a {course} quiz question. Generate concise, educational explanations.

Question: "{question_text}"
Options:
  A) {option_a}
  B) {option_b}
  C) {option_c}
  D) {option_d}
Correct Answer: {correct_answer}) {correct_text}
Student's Answer: {f"{student_answer}) {student_text}" if student_answer else "Not answered"}
Result: {"CORRECT ✓" if is_correct else "INCORRECT ✗"}

Provide a JSON response with:
1. "correctExplanation": 1-2 sentences explaining WHY option {correct_answer} is correct. Be specific and educational.
2. "wrongExplanation": {f'1-2 sentences explaining WHY option {student_answer} ({student_text}) is incorrect. Reference the specific misconception.' if not is_correct and student_answer else 'Empty string since student answered correctly.'}

Response format (JSON only, no markdown, no extra text):
{{"correctExplanation": "...", "wrongExplanation": "..."}}"""

    try:
        response = requests.post(
            ANTHROPIC_API_URL,
            headers={
                "Content-Type": "application/json",
                "x-api-key": api_key,
                "anthropic-version": "2023-06-01"
            },
            json={
                "model": MODEL,
                "max_tokens": 350,
                "messages": [{"role": "user", "content": prompt}]
            },
            timeout=15
        )
        data = response.json()
        text = data.get("content", [{}])[0].get("text", "{}")
        # Strip any markdown fences
        text = text.replace("```json", "").replace("```", "").strip()
        result = json.loads(text)
        return {
            "correctExplanation": result.get("correctExplanation", ""),
            "wrongExplanation": result.get("wrongExplanation", "") if not is_correct else ""
        }

    except Exception as e:
        print(f"[AI] Explanation failed for Q: {question_text[:40]}... Error: {e}")
        # Fallback explanation
        return {
            "correctExplanation": f"Option {correct_answer} ({correct_text}) is the correct answer for this question.",
            "wrongExplanation": (f"Option {student_answer} ({student_text}) is not correct. "
                                  f"The correct answer is {correct_answer}) {correct_text}."
                                 ) if not is_correct and student_answer else ""
        }


def generate_for_attempt(attempt_id: int, jwt_token: str, api_key: str) -> dict:
    """
    Fetch attempt results from backend and generate explanations for all questions.
    Then POST explanations back to backend.
    """
    headers = {
        "Authorization": f"Bearer {jwt_token}",
        "Content-Type": "application/json"
    }

    # Fetch attempt details
    try:
        res = requests.get(f"{BACKEND_URL}/quiz/my-results/0", headers=headers, timeout=10)
        # Find this specific attempt
        results = res.json()
        attempt = next((r for r in results if r.get("attemptId") == attempt_id), None)
        if not attempt:
            print(f"[AI] Attempt {attempt_id} not found in results.")
            return {"error": "Attempt not found"}
    except Exception as e:
        print(f"[AI] Failed to fetch attempt: {e}")
        return {"error": str(e)}

    questions = attempt.get("questions", [])
    course = attempt.get("course", "")
    explanations = []

    print(f"[AI] Generating explanations for {len(questions)} questions in attempt {attempt_id}...")

    for q in questions:
        print(f"  → Q{questions.index(q)+1}: {q['questionText'][:50]}...")
        exp = generate_explanation(
            question_text=q["questionText"],
            option_a=q["optionA"],
            option_b=q["optionB"],
            option_c=q["optionC"],
            option_d=q["optionD"],
            correct_answer=q["correctAnswer"],
            student_answer=q.get("studentAnswer"),
            course=course,
            api_key=api_key
        )
        explanations.append({
            "questionId": q["questionId"],
            "studentAnswer": q.get("studentAnswer"),
            "correctAnswer": q["correctAnswer"],
            "wasCorrect": q.get("isCorrect", False),
            "correctExplanation": exp["correctExplanation"],
            "wrongExplanation": exp["wrongExplanation"]
        })

    # POST explanations back to backend
    try:
        res = requests.post(
            f"{BACKEND_URL}/quiz/save-explanations",
            headers=headers,
            json={"attemptId": attempt_id, "explanations": explanations},
            timeout=10
        )
        result = res.json()
        print(f"[AI] Saved {result.get('saved', 0)} explanations to backend.")
        return {"success": True, "saved": result.get("saved", 0), "explanations": explanations}
    except Exception as e:
        print(f"[AI] Failed to save explanations: {e}")
        return {"success": False, "error": str(e), "explanations": explanations}


def generate_batch_explanations(questions_data: list, course: str, api_key: str) -> list:
    """
    Generate explanations for a list of question dicts.
    Used when calling from another Python module.
    """
    return [
        {
            "questionId": q.get("questionId", q.get("id")),
            **generate_explanation(
                question_text=q["questionText"],
                option_a=q["optionA"],
                option_b=q["optionB"],
                option_c=q["optionC"],
                option_d=q["optionD"],
                correct_answer=q["correctAnswer"],
                student_answer=q.get("studentAnswer"),
                course=course,
                api_key=api_key
            )
        }
        for q in questions_data
    ]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="UMAT AI Explanation Generator")
    parser.add_argument("--attempt-id", type=int, required=True, help="Quiz attempt ID")
    parser.add_argument("--token", type=str, required=True, help="JWT auth token")
    parser.add_argument("--api-key", type=str, required=True, help="Anthropic API key")
    args = parser.parse_args()

    result = generate_for_attempt(args.attempt_id, args.token, args.api_key)
    print("\n" + json.dumps(result, indent=2))
