package sk.mkrajcovic.springai.controller;

import static lombok.AccessLevel.PACKAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.mkrajcovic.springai.controller.dto.FlightRequest;
import sk.mkrajcovic.springai.controller.dto.FlightResponse;
import sk.mkrajcovic.springai.service.FlightChatService;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor(access = PACKAGE)
public class FlightChatController {

	private final FlightChatService chatService;

	@PostMapping(path = "/chat", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	FlightResponse chat(@RequestBody FlightRequest flightRequest) {
		return new FlightResponse(chatService.answerQuestion(flightRequest.getUserFlightQuestion()));
	}

}
