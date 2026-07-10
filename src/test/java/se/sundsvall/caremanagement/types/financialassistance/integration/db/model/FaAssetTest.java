package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaAssetTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaAsset.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("description"),
			hasValidBeanEqualsExcluding("description")));
	}

	@Test
	void testBuilderMethods() {
		final var assetCategory = "PROPERTY";
		final var description = "summer house";
		final var value = BigDecimal.valueOf(250000);
		final var propertyType = "HOUSE";
		final var purchaseYear = 2020;
		final var purchasePrice = BigDecimal.valueOf(200000);
		final var companyName = "Sundsvall AB";
		final var companyAssetSum = BigDecimal.valueOf(50000);
		final var vehicleType = "CAR";
		final var registrationNumber = "ABC123";
		final var purchaseDate = LocalDate.of(2026, 5, 25);

		final var result = FaAsset.create()
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
		assertThat(FaAsset.create()).hasAllNullFieldsOrProperties();
	}
}
