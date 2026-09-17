package com.renzoproject.calc_api.exception;

import com.renzoproject.calc.core.exception.CalculationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates every client-caused failure into one {@link ApiErrorResponse} shape, so the frontend
 * parses a single contract instead of guessing from which keys happen to be present.
 *
 * <ul>
 *   <li>{@link CalculationException} -- a domain rule in calc-core rejected the input. Message
 *       only; calc-core's messages are already written for the user.</li>
 *   <li>{@link MethodArgumentNotValidException} -- Bean Validation on the request DTO failed.
 *       Per-field messages in {@code fieldErrors}.</li>
 *   <li>{@link HttpMessageNotReadableException} -- the body isn't valid JSON, or a value can't be
 *       converted to its field's type (e.g. an unknown value for an enum-typed field). Without
 *       this handler Spring falls back to its default error body, a third, unrelated shape.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	static final String VALIDATION_FAILED_MESSAGE = "Request validation failed";
	static final String UNREADABLE_BODY_MESSAGE =
			"Request body is malformed or contains a value of the wrong type -- check the JSON syntax and field values.";

	@ExceptionHandler(CalculationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleCalculationException(CalculationException ex) {
		return ApiErrorResponse.of(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			String fieldMessage = fieldError.getDefaultMessage();
			fieldErrors.put(fieldError.getField(), fieldMessage != null ? fieldMessage : "is invalid");
		}

		// Class-level constraints aren't tied to one field, so they can't go in fieldErrors. The
		// previous handler dropped them silently, which produced an empty 400 body.
		List<String> globalMessages = ex.getBindingResult().getGlobalErrors().stream()
				.map(ObjectError::getDefaultMessage)
				.toList();
		String message = globalMessages.isEmpty() ? VALIDATION_FAILED_MESSAGE : String.join(" ", globalMessages);

		return new ApiErrorResponse(message, fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex) {
		// Deliberately not echoing ex.getMessage(): Jackson's messages expose internal class and
		// package names and aren't meaningful to an end user.
		return ApiErrorResponse.of(UNREADABLE_BODY_MESSAGE);
	}

}
