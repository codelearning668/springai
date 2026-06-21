package sk.mkrajcovic.springai.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Incoming request payload for the flights/chat endpoint.
 *
 * <p>
 * Contains a single user message that will be sent to the LLM for processing.
 */
@Getter
public class FlightRequest {

	@NotBlank
	@Size(max = 500)
	private String userFlightQuestion;

}
