package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "The applicant's Lifecare records, split into the two groups the tab shows.")
public class LifecareRecords {

	@Schema(description = "The journalanteckningar, in Lifecare's order")
	private List<LifecareRecord> journalNotes;

	@Schema(description = "The documents, in Lifecare's order")
	private List<LifecareRecord> documents;

	public static LifecareRecords create() {
		return new LifecareRecords();
	}

	public List<LifecareRecord> getJournalNotes() {
		return journalNotes;
	}

	public void setJournalNotes(final List<LifecareRecord> journalNotes) {
		this.journalNotes = journalNotes;
	}

	public LifecareRecords withJournalNotes(final List<LifecareRecord> journalNotes) {
		this.journalNotes = journalNotes;
		return this;
	}

	public List<LifecareRecord> getDocuments() {
		return documents;
	}

	public void setDocuments(final List<LifecareRecord> documents) {
		this.documents = documents;
	}

	public LifecareRecords withDocuments(final List<LifecareRecord> documents) {
		this.documents = documents;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareRecords that = (LifecareRecords) o;
		return Objects.equals(journalNotes, that.journalNotes) && Objects.equals(documents, that.documents);
	}

	@Override
	public int hashCode() {
		return Objects.hash(journalNotes, documents);
	}

	@Override
	public String toString() {
		return "LifecareRecords{" + "journalNotes=" + journalNotes + ", documents=" + documents + '}';
	}
}
