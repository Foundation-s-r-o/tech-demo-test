package sk.foundation.techdemo.persons.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonPdfServiceTest {

	@Mock
	private PersonApiService personApiService;

	@InjectMocks
	private PersonPdfService personPdfService;

	@Test
	void generatesNonEmptyPdfWithMagicHeader() {
		when(personApiService.getPerson(1L)).thenReturn(new PersonDetailResponseDTO(
				1L, "Jozef", "Antony", "jozef@example.com", "Bratislava", "SK", "0905548724"));

		byte[] pdf = personPdfService.generatePersonPdf(1L);

		assertThat(pdf).isNotEmpty();
		String header = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
		assertThat(header).isEqualTo("%PDF-");
	}
}
