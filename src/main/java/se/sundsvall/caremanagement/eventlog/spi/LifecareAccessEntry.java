package se.sundsvall.caremanagement.eventlog.spi;

/**
 * One access made in Lifecare.
 *
 * @param action      READ, CREATE, UPDATE or DELETE
 * @param target      what was accessed, a stable name rather than a Lifecare path
 * @param description a human-readable summary for the log, in Swedish
 * @param lifecareId  the Lifecare record the access was about, when there is one
 */
public record LifecareAccessEntry(String action, String target, String description, String lifecareId) {

	public static final String READ = "READ";
	public static final String CREATE = "CREATE";
	public static final String UPDATE = "UPDATE";
	public static final String DELETE = "DELETE";
}
