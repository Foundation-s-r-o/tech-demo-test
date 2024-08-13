package sk.foundation.techdemo.ai.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageItem {

	private String id;
	private String object;
	private long createdAt;
	private String role;
	private List<Content> content;
	private String assistantId;
	private String threadId;
	private String runId;

}
