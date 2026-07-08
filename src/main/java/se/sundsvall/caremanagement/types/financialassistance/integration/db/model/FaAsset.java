package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.hibernate.Length.LONG32;

@Embeddable
public class FaAsset {

	@Column(name = "asset_category")
	private String assetCategory;

	@Column(name = "description", length = LONG32)
	private String description;

	@Column(name = "value", precision = 12, scale = 2)
	private BigDecimal value;

	@Column(name = "property_type")
	private String propertyType;

	@Column(name = "purchase_year")
	private Integer purchaseYear;

	@Column(name = "purchase_price", precision = 12, scale = 2)
	private BigDecimal purchasePrice;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "company_asset_sum", precision = 12, scale = 2)
	private BigDecimal companyAssetSum;

	@Column(name = "vehicle_type")
	private String vehicleType;

	@Column(name = "registration_number")
	private String registrationNumber;

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	public static FaAsset create() {
		return new FaAsset();
	}

	public String getAssetCategory() {
		return assetCategory;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getValue() {
		return value;
	}

	public String getPropertyType() {
		return propertyType;
	}

	public Integer getPurchaseYear() {
		return purchaseYear;
	}

	public BigDecimal getPurchasePrice() {
		return purchasePrice;
	}

	public String getCompanyName() {
		return companyName;
	}

	public BigDecimal getCompanyAssetSum() {
		return companyAssetSum;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setAssetCategory(final String assetCategory) {
		this.assetCategory = assetCategory;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setValue(final BigDecimal value) {
		this.value = value;
	}

	public void setPropertyType(final String propertyType) {
		this.propertyType = propertyType;
	}

	public void setPurchaseYear(final Integer purchaseYear) {
		this.purchaseYear = purchaseYear;
	}

	public void setPurchasePrice(final BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
	}

	public void setCompanyName(final String companyName) {
		this.companyName = companyName;
	}

	public void setCompanyAssetSum(final BigDecimal companyAssetSum) {
		this.companyAssetSum = companyAssetSum;
	}

	public void setVehicleType(final String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public void setRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public void setPurchaseDate(final LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public FaAsset withAssetCategory(final String assetCategory) {
		this.assetCategory = assetCategory;
		return this;
	}

	public FaAsset withDescription(final String description) {
		this.description = description;
		return this;
	}

	public FaAsset withValue(final BigDecimal value) {
		this.value = value;
		return this;
	}

	public FaAsset withPropertyType(final String propertyType) {
		this.propertyType = propertyType;
		return this;
	}

	public FaAsset withPurchaseYear(final Integer purchaseYear) {
		this.purchaseYear = purchaseYear;
		return this;
	}

	public FaAsset withPurchasePrice(final BigDecimal purchasePrice) {
		this.purchasePrice = purchasePrice;
		return this;
	}

	public FaAsset withCompanyName(final String companyName) {
		this.companyName = companyName;
		return this;
	}

	public FaAsset withCompanyAssetSum(final BigDecimal companyAssetSum) {
		this.companyAssetSum = companyAssetSum;
		return this;
	}

	public FaAsset withVehicleType(final String vehicleType) {
		this.vehicleType = vehicleType;
		return this;
	}

	public FaAsset withRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public FaAsset withPurchaseDate(final LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
		return this;
	}

	// 'description' (a LONG32 column) is deliberately excluded from equals/hashCode/toString — it can be large and is not
	// part of the entity's identity.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaAsset that = (FaAsset) o;
		return Objects.equals(assetCategory, that.assetCategory)
			&& Objects.equals(value, that.value) && Objects.equals(propertyType, that.propertyType)
			&& Objects.equals(purchaseYear, that.purchaseYear) && Objects.equals(purchasePrice, that.purchasePrice)
			&& Objects.equals(companyName, that.companyName) && Objects.equals(companyAssetSum, that.companyAssetSum)
			&& Objects.equals(vehicleType, that.vehicleType) && Objects.equals(registrationNumber, that.registrationNumber)
			&& Objects.equals(purchaseDate, that.purchaseDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(assetCategory, value, propertyType, purchaseYear, purchasePrice, companyName,
			companyAssetSum, vehicleType, registrationNumber, purchaseDate);
	}

	@Override
	public String toString() {
		return "FaAsset{assetCategory='" + assetCategory + "', value=" + value
			+ ", propertyType='" + propertyType + "', purchaseYear=" + purchaseYear + ", purchasePrice=" + purchasePrice
			+ ", companyName='" + companyName + "', companyAssetSum=" + companyAssetSum + ", vehicleType='" + vehicleType
			+ "', registrationNumber='" + registrationNumber + "', purchaseDate=" + purchaseDate + '}';
	}
}
