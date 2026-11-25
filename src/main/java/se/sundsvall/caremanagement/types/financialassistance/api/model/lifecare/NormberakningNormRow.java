package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A normintervall of the norm, as Normintervall/Belopp offers it.
 */
@Schema(description = "A normintervall of the norm.")
public class NormberakningNormRow {

	@Schema(description = "The Lifecare norm row id", examples = "2")
	private Integer id;

	@Schema(description = "The name and monthly amount", examples = "Ensamstående 3940.00")
	private String name;

	public static NormberakningNormRow create() {
		return new NormberakningNormRow();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public NormberakningNormRow withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormberakningNormRow withName(final String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningNormRow that = (NormberakningNormRow) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Override
	public String toString() {
		return "NormberakningNormRow{" +
			"id=" + id +
			", name='" + name + '\'' +
			'}';
	}
}
