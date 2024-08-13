package sk.foundation.techdemo.ai.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class ChatRequest {

	private String model;
	private List<Message> messages;
	private int n;
	private double temperature;

	public ChatRequest(String model, String prompt) {
		this.model = model;

		this.messages = new ArrayList<>();
		this.messages.add(new Message("user", prompt));
		this.n = 1;
	}

}
