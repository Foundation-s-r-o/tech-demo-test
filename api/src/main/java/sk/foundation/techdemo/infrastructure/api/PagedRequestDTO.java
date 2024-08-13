package sk.foundation.techdemo.infrastructure.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PagedRequestDTO {

	@Min(0)
	private int pageStart = 0;

	@Min(1)
	@Max(200)
	private Integer pageSize = 15;

}
