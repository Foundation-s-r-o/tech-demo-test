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
public class MessagesResponse {

	private String object;
	private List<MessageItem> data;

}
