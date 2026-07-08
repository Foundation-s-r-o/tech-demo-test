package sk.foundation.techdemo.registry.api;

import java.util.List;

/** Current composite view of a person: current identity detail + current addresses. */
public record PersonCompositeResponseDTO(
		String uid,
		boolean deceased,
		DetailResponseDTO detail,
		List<AddressResponseDTO> addresses) {
}
