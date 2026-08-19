package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.APRIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class AssetTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Asset.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var assetCategory = "VEHICLE";
		final var description = "Savings account";
		final var value = BigDecimal.valueOf(120000);
		final var propertyType = "HOUSE";
		final var purchaseYear = 2018;
		final var purchasePrice = BigDecimal.valueOf(2200000);
		final var companyName = "Andersson Bygg AB";
		final var companyAssetSum = BigDecimal.valueOf(350000);
		final var vehicleType = "CAR";
		final var registrationNumber = "ABC123";
		final var purchaseDate = LocalDate.of(2018, APRIL, 12);

		final var result = Asset.create()
			.withAssetCategory(assetCategory)
			.withDescription(description)
			.withValue(value)
			.withPropertyType(propertyType)
			.withPurchaseYear(purchaseYear)
			.withPurchasePrice(purchasePrice)
			.withCompanyName(companyName)
			.withCompanyAssetSum(companyAssetSum)
			.withVehicleType(vehicleType)
			.withRegistrationNumber(registrationNumber)
			.withPurchaseDate(purchaseDate);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getAssetCategory()).isEqualTo(assetCategory);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getPropertyType()).isEqualTo(propertyType);
		assertThat(result.getPurchaseYear()).isEqualTo(purchaseYear);
		assertThat(result.getPurchasePrice()).isEqualTo(purchasePrice);
		assertThat(result.getCompanyName()).isEqualTo(companyName);
		assertThat(result.getCompanyAssetSum()).isEqualTo(companyAssetSum);
		assertThat(result.getVehicleType()).isEqualTo(vehicleType);
		assertThat(result.getRegistrationNumber()).isEqualTo(registrationNumber);
		assertThat(result.getPurchaseDate()).isEqualTo(purchaseDate);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Asset.create()).hasAllNullFieldsOrProperties();
	}
}
