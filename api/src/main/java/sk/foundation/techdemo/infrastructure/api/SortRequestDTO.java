package sk.foundation.techdemo.infrastructure.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortRequestDTO<O extends Enum<O>> {

	private O sortBy;

	private boolean sortDesc = false;

	public SortRequestDTO(O sortBy, boolean sortDesc) {
		this.sortBy = sortBy;
		this.sortDesc = sortDesc;
	}

}
