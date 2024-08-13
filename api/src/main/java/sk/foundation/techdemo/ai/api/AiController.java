package sk.foundation.techdemo.ai.api;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

	private final Logger log;
	private final RestTemplate openaiRestTemplate;

	@Value("${openai.model}")
	private String model;

	@Value("${openai.api.chat.url}")
	private String apiChatUrl;

	@Value("${openai.api.assistant.url}")
	private String apiAssistantUrl;

	@Value("${openai.api.assistant.id}")
	private String apiAssistantId;

	private String assistantThread = null;

	@GetMapping(value = "/chat")
	public String chat(@RequestParam String message) {
		// create a request
		ChatRequest request = new ChatRequest(model, message);
		log.info("Sending message: {}", message.replaceAll("[\n\r]", "_"));

		// call the API
		ChatResponse response = openaiRestTemplate.postForObject(apiChatUrl, request, ChatResponse.class);

		if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
			return "No response";
		}

		// return the first response
		String answer = response.getChoices().get(0).getMessage().getContent();
		log.info("Received answer: {}", answer);
		return answer;
	}

	@GetMapping(value = "/assistant")
	public String assistant(@RequestParam String message) throws InterruptedException {
		String messageId = null;
		String runId;
		String answer = null;

		// create common headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("OpenAI-Beta", "assistants=v2");

		// create a thread
		HttpEntity<String> threadEntity = new HttpEntity<>("{}", headers);
		if (assistantThread == null) {
			log.info("Creating new assistant thread");
			ResponseEntity<ThreadResponse> threadResponse = openaiRestTemplate
					.exchange(apiAssistantUrl, HttpMethod.POST, threadEntity, ThreadResponse.class);
			if (threadResponse.getBody() != null) {
				assistantThread = threadResponse.getBody().getId();
				log.info("Assistant thread created: {}", assistantThread);
			}
		}

		// send a message
		String messageUrl = apiAssistantUrl + "/" + assistantThread + "/messages";
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("role", "user");
		bodyMap.put("content", message);
		log.info("Sending message: {}", message.replaceAll("[\n\r]", "_"));
		HttpEntity<Map<String, Object>> messageEntity = new HttpEntity<>(bodyMap, headers);
		ResponseEntity<MessageResponse> messageResponse = openaiRestTemplate
				.exchange(messageUrl, HttpMethod.POST, messageEntity, MessageResponse.class);
		if (messageResponse.getBody() != null) {
			messageId = messageResponse.getBody().getId();
			log.info("Message sent: {}", messageId);
		}

		// run the assistant
		String runUrl = apiAssistantUrl + "/" + assistantThread + "/runs";
		bodyMap = new HashMap<>();
		bodyMap.put("assistant_id", apiAssistantId);
		bodyMap.put("instructions", "Please respond to the user message");
		log.info("Running assistant: {}", apiAssistantId);
		HttpEntity<Map<String, Object>> runEntity = new HttpEntity<>(bodyMap, headers);
		ResponseEntity<RunResponse> runResponse = openaiRestTemplate
				.exchange(runUrl, HttpMethod.POST, runEntity, RunResponse.class);
		if (runResponse.getBody() != null) {
			runId = runResponse.getBody().getId();
			log.info("Thread run: {}, status: {}", runId, runResponse.getBody().getStatus());
		}

		// get the answer
		String messagesUrl = apiAssistantUrl + "/" + assistantThread + "/messages";
		log.info("Getting answer");
		HttpEntity<String> messagesEntity = new HttpEntity<>(null, headers);
		for (int tries = 0; tries < 10; tries++) {
			answer = getAnswer(messageId, messagesUrl, messagesEntity);
			if (answer != null) {
				break;
			}
		}
		return answer;
	}

	private String getAnswer(String messageId, String messagesUrl, HttpEntity<String> messagesEntity)
			throws InterruptedException {
		String answer = null;
		ResponseEntity<MessagesResponse> messagesResponse = openaiRestTemplate
				.exchange(messagesUrl, HttpMethod.GET, messagesEntity, MessagesResponse.class);
		if (messagesResponse.getBody() != null) {
			List<MessageItem> messages = messagesResponse.getBody().getData();
			log.info("Conversation length: {}", messages.size());
			if (!messages.isEmpty()) {
				if (messageId != null && messageId.equals(messages.get(0).getId())) {
					log.info("Waiting for the answer");
					TimeUnit.SECONDS.sleep(10);
				} else {
					List<Content> content = messages.get(0).getContent();
					if (!content.isEmpty()) {
						answer = content.get(0).getText().getValue();
						log.info("Answer: {}", answer);
					}
				}
			}
		}
		return answer;
	}

}
