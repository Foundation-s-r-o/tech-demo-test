package sk.foundation.techdemo.gdpr.api;

import java.util.List;

import sk.foundation.techdemo.auditlog.api.AccessLogEntryDTO;
import sk.foundation.techdemo.records.api.BirthResponseDTO;
import sk.foundation.techdemo.records.api.DeathResponseDTO;
import sk.foundation.techdemo.records.api.MarriageResponseDTO;
import sk.foundation.techdemo.registry.api.AddressResponseDTO;
import sk.foundation.techdemo.registry.api.DetailResponseDTO;
import sk.foundation.techdemo.registry.api.PersonCompositeResponseDTO;
import sk.foundation.techdemo.registry.api.ReligionResponseDTO;

/**
 * Full GDPR Art. 15 data export for a person: the current composite plus the complete bitemporal
 * history of identity details, addresses and religion, every vital record where the person is a
 * subject (births as child, marriages as spouse, deaths), consents, disclosures, and the person's
 * own read-access log ("who saw my data"). Producing the export is itself recorded as a disclosure.
 */
public record ExportResponseDTO(
		String uid,
		PersonCompositeResponseDTO person,
		List<DetailResponseDTO> detailHistory,
		List<AddressResponseDTO> addressHistory,
		List<ReligionResponseDTO> religionHistory,
		List<BirthResponseDTO> births,
		List<MarriageResponseDTO> marriages,
		List<DeathResponseDTO> deaths,
		List<ConsentResponseDTO> consents,
		List<DisclosureResponseDTO> disclosures,
		List<AccessLogEntryDTO> accessLog) {
}
