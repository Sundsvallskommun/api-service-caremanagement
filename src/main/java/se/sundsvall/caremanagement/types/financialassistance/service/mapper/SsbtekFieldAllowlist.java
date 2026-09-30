package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Keeps only the allowlisted fields of SSBTEK's per-agency answers and drops everything else, however deep.
 *
 * <p>
 * Why: the agency answers are income data for a named person and they carry personnummer, names and addresses beside
 * the amounts. Dataminimering means the identity fields never leave caremanagement; a caller gets the fields it has a
 * display need for and nothing more. Every field that passes is therefore a decision — add a path only when a screen
 * shows the field and its mapping is known (see {@link SsbtekPaymentViewFields}). A field nobody listed is gone, so a
 * new field in the SSBTEK contract stays out until somebody asks for it.
 * </p>
 *
 * <p>
 * A path is dotted, starts with the agency key and follows the answer's own keys, for example
 * {@code fk.formansinformation.utbetalningsuppgift.datum}. The rules, applied recursively from the agency down:
 * </p>
 * <ul>
 * <li>a key is kept only when a path passes through it; keys are matched exactly (the answers are converted from XML,
 * so their case follows the contract's element names)</li>
 * <li>a list is not a step of its own: each element is filtered with the same path, so a path reads the same whether
 * the agency answered one item or several, and a list keeps its elements and their order</li>
 * <li>an object is filtered by the paths that go through it and is kept even when nothing in it is allowed, so a list
 * keeps its element count</li>
 * <li>a plain value (text, number, boolean) is kept only where a path ends; an object or list found where a path
 * ends is dropped whole, so a path never lets a subtree through</li>
 * <li>a plain value where the paths continue is dropped, and to let a node that some answers give as plain text and
 * others as an object through in both forms, list both the node itself and the fields below it</li>
 * <li>a {@code null} on an allowed path stays a {@code null}; an agency without an answer is left out</li>
 * </ul>
 *
 * <p>
 * The input is never changed; the result is a new, mutable tree of the same kinds of maps and lists.
 * </p>
 */
public final class SsbtekFieldAllowlist {

	private static final String PATH_SEPARATOR = "\\.";
	private static final Object DROPPED = new Object();

	private final Node root;

	private SsbtekFieldAllowlist(final Node root) {
		this.root = root;
	}

	/**
	 * @param  paths                    the allowed paths, each an agency key followed by at least one field key
	 * @return                          the allowlist
	 * @throws IllegalArgumentException for a path that is blank, has an empty step, or names only an agency
	 */
	public static SsbtekFieldAllowlist of(final Collection<String> paths) {
		final var root = new Node();
		paths.forEach(path -> root.add(0, split(path)));
		return new SsbtekFieldAllowlist(root);
	}

	private static String[] split(final String path) {
		final var steps = ofNullable(path).orElse("").split(PATH_SEPARATOR, -1);
		if (steps.length < 2) {
			throw new IllegalArgumentException("An allowed path is an agency and at least one field, not '%s'".formatted(path));
		}
		for (final var step : steps) {
			if (step.isBlank()) {
				throw new IllegalArgumentException("An allowed path has no empty steps, not '%s'".formatted(path));
			}
		}
		return steps;
	}

	/**
	 * @param  agencies the answers per agency as SSBTEK gave them; may be {@code null}
	 * @return          the same answers reduced to the allowed fields, never {@code null}; an agency no path names, or
	 *                  that gave no answer, is not in it
	 */
	public Map<String, Map<String, Object>> retainIn(final Map<String, Map<String, Object>> agencies) {
		final var retained = new LinkedHashMap<String, Map<String, Object>>();
		ofNullable(agencies).orElseGet(Map::of).forEach((agency, answer) -> {
			final var node = root.children.get(agency);
			if (node != null && answer != null) {
				retained.put(agency, retainFields(answer, node));
			}
		});
		return retained;
	}

	private static Map<String, Object> retainFields(final Map<?, ?> fields, final Node node) {
		final var retained = new LinkedHashMap<String, Object>();
		fields.forEach((key, value) -> {
			final var name = String.valueOf(key);
			final var child = node.children.get(name);
			if (child != null) {
				final var kept = retain(value, child);
				if (kept != DROPPED) {
					retained.put(name, kept);
				}
			}
		});
		return retained;
	}

	private static Object retain(final Object value, final Node node) {
		if (value instanceof final Map<?, ?> fields) {
			return retainObject(fields, node);
		}
		if (value instanceof final Collection<?> items) {
			return retainItems(items, node);
		}
		return retainPlainValue(value, node);
	}

	/** An object where a path ends is dropped whole; anywhere else it is filtered by the paths that go through it. */
	private static Object retainObject(final Map<?, ?> fields, final Node node) {
		if (node.children.isEmpty()) {
			return DROPPED;
		}
		return retainFields(fields, node);
	}

	/** A {@code null} carries nothing and is kept; a plain value is kept where a path ends. */
	private static Object retainPlainValue(final Object value, final Node node) {
		if (value == null || (node.leaf && isPlainValue(value))) {
			return value;
		}
		return DROPPED;
	}

	private static List<Object> retainItems(final Collection<?> items, final Node node) {
		final var retained = new ArrayList<Object>();
		for (final var item : items) {
			final var kept = retain(item, node);
			if (kept != DROPPED) {
				retained.add(kept);
			}
		}
		return retained;
	}

	private static boolean isPlainValue(final Object value) {
		return value instanceof CharSequence || value instanceof Number || value instanceof Boolean;
	}

	/** One step of the allowed paths: whether a path ends here, and the steps that continue below it. */
	private static final class Node {

		private boolean leaf;
		private final Map<String, Node> children = new LinkedHashMap<>();

		private void add(final int index, final String[] steps) {
			if (index == steps.length) {
				leaf = true;
				return;
			}
			children.computeIfAbsent(steps[index], step -> new Node()).add(index + 1, steps);
		}
	}
}
