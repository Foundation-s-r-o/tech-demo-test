package sk.foundation.techdemo.ai.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatResponse {

	private List<Choice> choices;

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static class Choice {

		private int index;
		private Message message;

	}

}