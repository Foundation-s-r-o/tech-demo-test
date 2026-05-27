package sk.foundation.techdemo.persons.api;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;

/**
 * Renders a person detail as a simple PDF using OpenPDF. Kept deliberately minimal
 * (one document type, programmatic layout) per KISS/YAGNI.
 */
@Service
@RequiredArgsConstructor
public class PersonPdfService {

	private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
	private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);

	private final PersonApiService personApiService;

	public byte[] generatePersonPdf(Long id) {
		PersonDetailResponseDTO person = personApiService.getPerson(id);

		Document document = new Document();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, out);
		document.open();
		try {
			document.add(new Paragraph("Person detail", TITLE_FONT));
			document.add(new Paragraph(" "));
			document.add(line("First name", person.getFirstName()));
			document.add(line("Last name", person.getLastName()));
			document.add(line("Email", person.getEmail()));
			document.add(line("Address", person.getAddress()));
			document.add(line("State", person.getState()));
			document.add(line("Phone number", person.getPhoneNumber()));
		} finally {
			document.close();
		}
		return out.toByteArray();
	}

	private Paragraph line(String label, String value) {
		return new Paragraph(label + ": " + (value == null ? "" : value), LABEL_FONT);
	}
}
