package se.sundsvall.caremanagement.lifecare.professionalweb;

/**
 * One field of an HTML form as a browser posts it back. A name may repeat, as a checkbox's does beside its hidden
 * false, so a form is a list of these rather than a map.
 *
 * @param name  the field's name
 * @param value the field's value
 */
public record ProfessionalWebFormField(String name, String value) {
}
