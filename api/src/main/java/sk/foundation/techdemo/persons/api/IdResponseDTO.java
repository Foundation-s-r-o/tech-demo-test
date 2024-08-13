package sk.foundation.techdemo.persons.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IdResponseDTO<T> {

	private final T id;

}
