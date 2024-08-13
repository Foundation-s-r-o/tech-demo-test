package sk.foundation.techdemo.infrastructure.api;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class PagedResultResponseDTO<T> {

	private final Long totalElements;

	private final List<T> elements;

}
