package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "An asset owned by the applicant or co-applicant.")
public class Asset {

	@Schema(description = "The category of asset", examples = "VEHICLE", allowableValues = {
		"BANK_SAVINGS", "REAL_ESTATE", "COMPANY", "VEHICLE"
	})
	@OneOf(value = {
		"BANK_SAVINGS", "REAL_ESTATE", "COMPANY", "VEHICLE"
	}, nullable = true)
	private String assetCategory;

	@Schema(description = "Free text description of the asset", examples = "Sparkonto hos Swedbank")
	private String description;

	@Schema(description = "Estimated value of the asset", examples = "120000.00")
	private BigDecimal value;

	@Schema(description = "Type of real estate property", examples = "VILLA", allowableValues = {
		"BOSTADSRATT", "VILLA", "FASTIGHET", "FRITIDSHUS"
	})
	@OneOf(value = {
		"BOSTADSRATT", "VILLA", "FASTIGHET", "FRITIDSHUS"
	}, nullable = true)
	private String propertyType;

	@Schema(description = "Year the asset was purchased", examples = "2018")
	private Integer purchaseYear;

	@Schema(description = "Price paid when the asset was purchased", examples = "2200000.00")
	private BigDecimal purchasePrice;

	@Schema(description = "Name of the company asset", examples = "Andersson Bygg AB")
	private String companyName;

	@Schema(description = "Total sum of the company's assets", examples = "350000.00")
	private BigDecimal companyAssetSum;

	@Schema(description = "Type of vehicle", examples = "BIL", allowableValues = {
		"BIL", "BAT", "MC", "HUSVAGN", "MOPED", "SNOSKOTER", "ANNAT"
	})
	@OneOf(value = {
		"BIL", "BAT", "MC", "HUSVAGN", "MOPED", "SNOSKOTER", "ANNAT"
	}, nullable = true)
	private String vehicleType;

	@Schema(description = "Vehicle registration number", examples = "ABC123")
	private String registrationNumber;

	@Schema(description = "The date the asset was purchased", examples = "2018-04-12")
	private LocalDate purchaseDate;

	public static Asset create() {
		return new Asset();
	}

	public String getAssetCategory() {
		return assetCategory;
	}

	public void setAssetCategory(final String assetCategory) {
		this.assetCategory = assetCategory;
	}

	public Asset withAssetCategory(final String assetCategory) {
		this.assetCategory = assetCategory;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Asset withDescription(final String description) {
		this.description = description;
		return this;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(final BigDecimal value) {
		this.value = value;
	}

	public Asset withValue(final BigDecimal value) {
		this.value = value;
		return this;
	}

	public String getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(final String propertyType) {
		this.propertyType = propertyType;
	}

	public Asset withPropertyType(final String propertyType) {
		this.propertyType = propertyType;
		return this;
	}

	public Integer getPurchaseYear() {
		return purchaseYear;
	}

	public void setPurchaseYear(final Integer purchaseYear) {
		this.purchaseYear = purchaseYear;
	}

	public Asset withPurchaseYear(final Integer purchaseYear) {
		this.purchaseYear = purchaseYear;
		return this;
	}

	public BigDecimal getPurchasePrice() {
		return purchasePrice;
	}

	public void setPurchasePrice(final BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
	}

	public Asset withPurchasePrice(final BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
		return this;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(final String companyName) {
		this.companyName = companyName;
	}

	public Asset withCompanyName(final String companyName) {
		this.companyName = companyName;
		return this;
	}

	public BigDecimal getCompanyAssetSum() {
		return companyAssetSum;
	}

	public void setCompanyAssetSum(final BigDecimal companyAssetSum) {
		this.companyAssetSum = companyAssetSum;
	}

	public Asset withCompanyAssetSum(final BigDecimal companyAssetSum) {
		this.companyAssetSum = companyAssetSum;
		return this;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(final String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public Asset withVehicleType(final String vehicleType) {
		this.vehicleType = vehicleType;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public Asset withRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(final LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public Asset withPurchaseDate(final LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Asset that = (Asset) o;
		return Objects.equals(assetCategory, that.assetCategory) && Objects.equals(description, that.description)
			&& Objects.equals(value, that.value) && Objects.equals(propertyType, that.propertyType)
			&& Objects.equals(purchaseYear, that.purchaseYear) && Objects.equals(purchasePrice, that.purchasePrice)
			&& Objects.equals(companyName, that.companyName) && Objects.equals(companyAssetSum, that.companyAssetSum)
			&& Objects.equals(vehicleType, that.vehicleType) && Objects.equals(registrationNumber, that.registrationNumber)
			&& Objects.equals(purchaseDate, that.purchaseDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(assetCategory, description, value, propertyType, purchaseYear, purchasePrice, companyName,
			companyAssetSum, vehicleType, registrationNumber, purchaseDate);
	}

	@Override
	public String toString() {
		return "Asset{assetCategory='" + assetCategory + "', description='" + description + "', value=" + value
			+ ", propertyType='" + propertyType + "', purchaseYear=" + purchaseYear + ", purchasePrice=" + purchasePrice
			+ ", companyName='" + companyName + "', companyAssetSum=" + companyAssetSum + ", vehicleType='" + vehicleType
			+ "', registrationNumber='" + registrationNumber + "', purchaseDate=" + purchaseDate + '}';
	}
}
