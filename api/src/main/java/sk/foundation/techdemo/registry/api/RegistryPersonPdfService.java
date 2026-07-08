package sk.foundation.techdemo.registry.api;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;

/**
 * Renders a civil-registry person extract (current identity + permanent address) as a PDF via
 * OpenPDF. Reads through {@link RegistryPersonApiService#getComposite}, so the read is access-logged.
 */
@Service
@RequiredArgsConstructor
public class RegistryPersonPdfService {

	private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
	private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);

	private final RegistryPersonApiService personApiService;

	public byte[] generatePersonExtract(String uid, String purpose, String requestId) {
		PersonCompositeResponseDTO person = personApiService.getComposite(uid, purpose, requestId);

		Document document = new Document();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, out);
		document.open();
		try {
			document.add(new Paragraph("Vypis z registra fyzickych osob", TITLE_FONT));
			document.add(new Paragraph(" "));
			document.add(line("Identifikator", person.uid()));
			DetailResponseDTO detail = person.detail();
			if (detail != null) {
				document.add(line("Meno", detail.givenNames()));
				document.add(line("Priezvisko", detail.familyName()));
				document.add(line("Pohlavie", detail.sex()));
				document.add(line("Datum narodenia",
						detail.dateOfBirth() == null ? "" : detail.dateOfBirth().toString()));
				document.add(line("Statna prislusnost", detail.citizenship()));
			}
			document.add(line("Zomrely", person.deceased() ? "ano" : "nie"));
			if (!person.addresses().isEmpty()) {
				AddressResponseDTO address = person.addresses().get(0);
				document.add(line("Adresa", address.street() + " " + nullToEmpty(address.houseNumber())));
			}
		} finally {
			document.close();
		}
		return out.toByteArray();
	}

	private static Paragraph line(String label, String value) {
		return new Paragraph(label + ": " + nullToEmpty(value), LABEL_FONT);
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
