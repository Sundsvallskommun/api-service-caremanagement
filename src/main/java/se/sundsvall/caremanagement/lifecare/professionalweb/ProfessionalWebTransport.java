package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.util.List;
import java.util.Map;

/**
 * How a ProfessionalWeb call reaches Lifecare.
 *
 * <p>
 * The call is handed to api-service-lifecare-integrator, which holds the integration account's session inside the
 * municipal network. The answer is Lifecare's final one after any session escalation, whatever its status;
 * interpreting it is the client's job.
 * </p>
 */
public interface ProfessionalWebTransport {

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
