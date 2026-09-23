package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Who a payment goes to and how. Free-form on purpose: the payment methods and account formats are Lifecare's, and
 * the robot types them into the Lifecare payment form as given.
 */
@Schema(description = "The recipient of a payment and the payment method.")
public class Payee {

	@Schema(description = "The id of the payee row this came from, as GET .../payees returns it — send it whenever the caseworker "
		+ "picked an entry from that list. It is what lets the REGISTER_PAYMENT robot be handed the payee's Lifecare id "
		+ "instead of matching on name and account number. Omit it for a payee that has no row: one derived from the "
		+ "Lifecare payment history carries a null id in the list.", examples = "a1b2c3d4-0000-0000-0000-000000000001")
	@ValidUuid(nullable = true)
	private String id;

	@Schema(description = "Name of the payee as registered in Lifecare", examples = "Anna Andersson", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String name;

	@Schema(description = "The Lifecare payment method, e.g. bank account, bankgiro, plusgiro or utbetalningskort", examples = "BANKKONTO", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 64)
	private String paymentMethod;

	@Schema(description = "Clearing number, when the payment method needs one", examples = "6000")
	@Size(max = 16)
	private String clearing;

	@Schema(description = "Account, bankgiro or plusgiro number, when the payment method needs one", examples = "123456789")
	@Size(max = 64)
	private String accountNumber;

	@Schema(description = """
		The payee's id in Lifecare. Send it when the caseworker picked a payee Lifecare already has — the list read \
		from Lifecare, or one just created there — so the payment can be registered against that payee by id instead \
		of matching on name and account number.""", examples = "1234567")
	@Size(max = 64)
	private String lifecarePayeeId;

	@Schema(description = "The payee's street address", examples = "Storgatan 1")
	@Size(max = 255)
	private String address;

	@Schema(description = "The payee's c/o line", examples = "c/o Bertil Bertilsson")
	@Size(max = 255)
	private String careOf;

	@Schema(description = "The payee's zip code", examples = "85230")
	@Size(max = 16)
	private String zipCode;

	@Schema(description = "The payee's city", examples = "Sundsvall")
	@Size(max = 255)
	private String city;

	public static Payee create() {
		return new Payee();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Payee withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Payee withName(final String name) {
		this.name = name;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public Payee withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public Payee withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Payee withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public Payee withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public Payee withAddress(final String address) {
		this.address = address;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(final String careOf) {
		this.careOf = careOf;
	}

	public Payee withCareOf(final String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(final String zipCode) {
		this.zipCode = zipCode;
	}

	public Payee withZipCode(final String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(final String city) {
		this.city = city;
	}

	public Payee withCity(final String city) {
		this.city = city;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Payee that = (Payee) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(paymentMethod, that.paymentMethod)
			&& Objects.equals(clearing, that.clearing) && Objects.equals(accountNumber, that.accountNumber) && Objects.equals(lifecarePayeeId, that.lifecarePayeeId)
			&& Objects.equals(address, that.address) && Objects.equals(careOf, that.careOf) && Objects.equals(zipCode, that.zipCode) && Objects.equals(city, that.city);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, paymentMethod, clearing, accountNumber, lifecarePayeeId, address, careOf, zipCode, city);
	}

	@Override
	public String toString() {
		return "Payee{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", clearing='" + clearing + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			", lifecarePayeeId='" + lifecarePayeeId + '\'' +
			", address='" + address + '\'' +
			", careOf='" + careOf + '\'' +
			", zipCode='" + zipCode + '\'' +
			", city='" + city + '\'' +
			'}';
	}
}
