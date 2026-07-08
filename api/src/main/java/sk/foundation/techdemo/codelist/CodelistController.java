package sk.foundation.techdemo.codelist;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Reference codelists (low sensitivity) — readable by any authenticated user. */
@RestController
@RequestMapping("/api/codelists")
@RequiredArgsConstructor
public class CodelistController {

	private final CodelistService codelistService;

	@GetMapping
	public List<String> list() {
		return codelistService.available();
	}

	@GetMapping("/{codelist}/items")
	public List<CodelistItemDTO> items(@PathVariable("codelist") String codelist) {
		return codelistService.items(codelist);
	}
}
