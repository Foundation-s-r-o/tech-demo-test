package sk.foundation.techdemo.codelist;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to reference codelists for UI dropdowns. Rather than mapping 14 read-only entities,
 * a whitelisted map of {@code name -> (table, code column)} drives a parameterless native query;
 * the whitelist makes the concatenated identifiers safe from injection.
 */
@Service
public class CodelistService {

	private static final Map<String, String[]> CODELISTS = Map.ofEntries(
			Map.entry("sex", new String[] { "CODELIST.SEX", "SEX_CODE" }),
			Map.entry("marital_status", new String[] { "CODELIST.MARITAL_STATUS", "MARITAL_STATUS_CODE" }),
			Map.entry("existence_status", new String[] { "CODELIST.EXISTENCE_STATUS", "EXISTENCE_STATUS_CODE" }),
			Map.entry("address_type", new String[] { "CODELIST.ADDRESS_TYPE", "ADDRESS_TYPE_CODE" }),
			Map.entry("religion", new String[] { "CODELIST.RELIGION", "RELIGION_CODE" }),
			Map.entry("country", new String[] { "CODELIST.COUNTRY", "COUNTRY_CODE" }),
			Map.entry("court", new String[] { "CODELIST.COURT", "COURT_CODE" }),
			Map.entry("registry_office", new String[] { "CODELIST.REGISTRY_OFFICE", "OFFICE_CODE" }),
			Map.entry("region", new String[] { "CODELIST.REGION", "REGION_CODE" }),
			Map.entry("district", new String[] { "CODELIST.DISTRICT", "DISTRICT_CODE" }),
			Map.entry("municipality", new String[] { "CODELIST.MUNICIPALITY", "MUNICIPALITY_CODE" }),
			Map.entry("municipality_part", new String[] { "CODELIST.MUNICIPALITY_PART", "PART_CODE" }),
			// ACADEMIC_TITLE carries a before/after position discriminator; distinct collapses the
			// composite (code, position) key to one row per title code for the typeahead.
			Map.entry("academic_title", new String[] { "CODELIST.ACADEMIC_TITLE", "TITLE_CODE" }));

	@PersistenceContext
	private EntityManager entityManager;

	public List<String> available() {
		return new TreeSet<>(CODELISTS.keySet()).stream().toList();
	}

	@Transactional(readOnly = true)
	public List<CodelistItemDTO> items(String name) {
		String[] target = CODELISTS.get(name);
		if (target == null) {
			throw new EntityNotFoundException("Unknown codelist: " + name);
		}
		String sql = "select distinct " + target[1] + " as code, name from " + target[0] + " order by name";
		List<?> rows = entityManager.createNativeQuery(sql).getResultList();
		return rows.stream()
				.map(row -> (Object[]) row)
				.map(cols -> new CodelistItemDTO((String) cols[0], (String) cols[1]))
				.toList();
	}
}
