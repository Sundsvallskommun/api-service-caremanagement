package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.util.List;
import java.util.Map;

/**
 * How a ProfessionalWeb call reaches Lifecare.
 *
 * <p>
 * Chosen by {@code integration.lifecare-professionalweb.provider}: {@code direct} signs careM in and talks to Lifecare
 * itself, which only works from inside the municipal network; {@code integrator} hands the call to
 * api-service-lifecare-integrator, which does the same from inside it. Either way the answer is Lifecare's final one
 * after any session escalation, whatever its status; interpreting it is the client's job.
 * </p>
 */
public interface ProfessionalWebTransport {

	String PROVIDER_PROPERTY = "integration.lifecare-professionalweb.provider";

	/**
	 * Makes one call.
	 *
	 * @param  method the HTTP method
	 * @param  path   the path below the ProfessionalWeb module
	 * @param  params query parameters, in order
	 * @param  body   the JSON body, or null for none
	 * @return        Lifecare's answer
	 */
	ProfessionalWebResponse exchange(String method, String path, Map<String, String> params, byte[] body);

	/**
	 * Posts a form back the way a browser submits it: application/x-www-form-urlencoded, to the same address and query
	 * it came from, with a browser's navigation headers rather than api2's ajax ones.
	 *
	 * @param  path   the path below the ProfessionalWeb module
	 * @param  params query parameters, in order
	 * @param  fields the form's fields, in document order
	 * @return        Lifecare's answer
	 */
	ProfessionalWebResponse submitForm(String path, Map<String, String> params, List<ProfessionalWebFormField> fields);
}
