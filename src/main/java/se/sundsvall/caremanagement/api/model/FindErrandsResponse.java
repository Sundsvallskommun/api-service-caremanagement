package se.sundsvall.caremanagement.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Paged errand response")
public class FindErrandsResponse {

	@ArraySchema(schema = @Schema(implementation = Errand.class, accessMode = READ_ONLY))
	private List<Errand> errands;

	@JsonProperty("_meta")
	@Schema(implementation = PagingAndSortingMetaData.class, accessMode = READ_ONLY)
	private PagingAndSortingMetaData metaData;

	public static FindErrandsResponse create() {
		return new FindErrandsResponse();
	}

	public List<Errand> getErrands() {
		return errands;
	}

	public void setErrands(final List<Errand> errands) {
		this.errands = errands;
	}

	public FindErrandsResponse withErrands(final List<Errand> errands) {
		this.errands = errands;
		return this;
	}

	public PagingAndSortingMetaData getMetaData() {
		return metaData;
	}

	public void setMetaData(final PagingAndSortingMetaData metaData) {
		this.metaData = metaData;
	}

	public FindErrandsResponse withMetaData(final PagingAndSortingMetaData metaData) {
		this.metaData = metaData;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FindErrandsResponse that = (FindErrandsResponse) o;
		return Objects.equals(errands, that.errands) && Objects.equals(metaData, that.metaData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errands, metaData);
	}

	@Override
	public String toString() {
		return "FindErrandsResponse{" +
			"errands=" + errands +
			", metaData=" + metaData +
			'}';
	}
}
