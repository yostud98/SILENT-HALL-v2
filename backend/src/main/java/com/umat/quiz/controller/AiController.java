package com.umat.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Silent Hall — AI Explanation Controller
 * ----------------------------------------
 * Provides on-demand AI explanations for quiz questions.
 * Called when a student clicks "Explain with AI" on the result detail page.
 *
 * Endpoint: POST /api/ai/explain
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-20250514";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * POST /api/ai/explain
     *
     * Request body:
     * {
     *   "question":      "Question text",
     *   "options":       {"A":"...", "B":"...", "C":"...", "D":"..."},
     *   "correctAnswer": "B",
     *   "studentAnswer": "A",   // null if not answered
     *   "course":        "Computer Science"
     * }
     *
     * Response:
     * {
     *   "correctExplanation": "...",
     *   "wrongExplanation":   "..."   // empty string if student was correct
     * }
     */
    @PostMapping("/explain")
    public ResponseEntity<?> explain(@RequestBody Map<String, Object> body) {

        String question     = getString(body, "question");
        String correctAnswer = getString(body, "correctAnswer");
        String studentAnswer = getString(body, "studentAnswer"); // may be null
        String course       = getString(body, "course");

        @SuppressWarnings("unchecked")
        Map<String, String> options = body.containsKey("options")
                ? (Map<String, String>) body.get("options")
                : Map.of("A","Option A","B","Option B","C","Option C","D","Option D");

        String correctText = options.getOrDefault(correctAnswer, correctAnswer);
        String studentText = studentAnswer != null ? options.getOrDefault(studentAnswer, studentAnswer) : null;
        boolean isCorrect  = correctAnswer.equalsIgnoreCase(studentAnswer);

        // Build prompt
        String prompt = buildPrompt(question, options, correctAnswer, correctText,
                studentAnswer, studentText, isCorrect, course);

        // Try Anthropic API; fall back to a built-in response if key is missing/invalid
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return ResponseEntity.ok(buildFallback(correctAnswer, correctText,
                    studentAnswer, studentText, isCorrect));
        }

        try {
            Map<String, Object> result = callAnthropicApi(prompt, isCorrect);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Return graceful fallback — never crash the student UX
            return ResponseEntity.ok(buildFallback(correctAnswer, correctText,
                    studentAnswer, studentText, isCorrect));
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private String buildPrompt(String question, Map<String, String> options,
                                String correctAnswer, String correctText,
                                String studentAnswer, String studentText,
                                boolean isCorrect, String course) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an academic tutor at Silent Hall, the examination system for UMAT ");
        sb.append("(University of Mines and Technology, Ghana).\n");
        if (course != null && !course.isBlank()) {
            sb.append("Course: ").append(course).append("\n");
        }
        sb.append("\nQuestion: \"").append(question).append("\"\n");
        sb.append("Options:\n");
        for (String letter : List.of("A", "B", "C", "D")) {
            sb.append("  ").append(letter).append(") ").append(options.getOrDefault(letter, "")).append("\n");
        }
        sb.append("\nCorrect Answer: ").append(correctAnswer).append(") ").append(correctText).append("\n");
        sb.append("Student's Answer: ");
        if (studentAnswer != null) {
            sb.append(studentAnswer).append(") ").append(studentText);
        } else {
            sb.append("Not answered");
        }
        sb.append("\nResult: ").append(isCorrect ? "CORRECT ✓" : "INCORRECT ✗").append("\n\n");

        sb.append("Generate a JSON response with exactly two keys:\n");
        sb.append("1. \"correctExplanation\": 1–2 clear sentences explaining WHY option ")
          .append(correctAnswer).append(" (").append(correctText).append(") is correct. ");
        sb.append("Be specific, educational and reference the course subject matter.\n");

        if (!isCorrect && studentAnswer != null) {
            sb.append("2. \"wrongExplanation\": 1–2 sentences explaining WHY option ")
              .append(studentAnswer).append(" (").append(studentText)
              .append(") is incorrect. Address the specific misconception.\n");
        } else {
            sb.append("2. \"wrongExplanation\": \"\" (empty string — student was correct or did not answer)\n");
        }

        sb.append("\nRespond with ONLY valid JSON, no markdown fences, no extra text:\n");
        sb.append("{\"correctExplanation\":\"...\",\"wrongExplanation\":\"...\"}");
        return sb.toString();
    }

    private Map<String, Object> callAnthropicApi(String prompt, boolean isCorrect) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", 400,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        String jsonBody = mapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Empty content from Anthropic API");
        }

        String text = (String) content.get(0).get("text");
        // Strip markdown fences if present
        text = text.replaceAll("```json", "").replaceAll("```", "").trim();

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(text, Map.class);
        return parsed;
    }

    private Map<String, Object> buildFallback(String correctAnswer, String correctText,
                                               String studentAnswer, String studentText,
                                               boolean isCorrect) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correctExplanation",
                "Option " + correctAnswer + " (" + correctText + ") is the correct answer to this question. " +
                "Review your course material for a detailed explanation.");

        if (!isCorrect && studentAnswer != null) {
            result.put("wrongExplanation",
                    "Option " + studentAnswer + " (" + studentText + ") is not correct. " +
                    "The correct answer is " + correctAnswer + " (" + correctText + ").");
        } else {
            result.put("wrongExplanation", "");
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
