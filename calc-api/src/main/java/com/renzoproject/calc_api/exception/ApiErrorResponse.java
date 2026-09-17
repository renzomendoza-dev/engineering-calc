package com.renzoproject.calc_api.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The single error body every 400 from this API returns, whatever caused it:
 *
 * <pre>
 * {"message": "No duct size satisfies these inputs...", "fieldErrors": {}}
 * {"message": "Request validation failed", "fieldErrors": {"totalOperatingHours": "must be greater than 0"}}
 * </pre>
 *
 * @param message     always present; human-readable, safe to show directly to a user
 * @param fieldErrors request field name to message; always present (never {@code null}), empty
 *                    unless the failure is attributable to specific request fields, so clients
 *                    can branch on {@code fieldErrors.isEmpty()} rather than probing for keys
 */
public record ApiErrorResponse(String message, Map<String, String> fieldErrors) {

	public ApiErrorResponse {
		// Not Map.copyOf: that drops insertion order (fields should read in request order) and
		// throws on a null value, which would turn a 400 into a 500.
		fieldErrors = fieldErrors == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
	}

	public static ApiErrorResponse of(String message) {
		return new ApiErrorResponse(message, Map.of());
	}

}
