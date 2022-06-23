/********************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.action.impl;

import static org.eclipse.transformer.util.FileUtils.DEFAULT_CHARSET;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.unmodifiable.Maps;
import org.eclipse.transformer.action.SelectionRule;
import org.slf4j.Logger;

public class SelectionRuleImpl implements SelectionRule {
	private static final char RESOURCE_WILDCARD = '*';

	public SelectionRuleImpl(Logger logger, Map<String, String> includes, Map<String, String> excludes) {
		this.logger = logger;

		included = processSelections(includes);
		excluded = processSelections(excludes);
	}

	private final MatchSet	included;
	private final MatchSet	excluded;

	private MatchSet processSelections(Map<String, String> selections) {
		if (selections == null) {
			return null;
		}
		Map<String, Charset> exact = new HashMap<>();
		Map<String, Charset> head = new HashMap<>();
		Map<String, Charset> tail = new HashMap<>();
		Map<String, Charset> middle = new HashMap<>();
		Charset all = null;
		for (Map.Entry<String, String> selectionEntry : selections.entrySet()) {
			String selection = selectionEntry.getKey();
			int selectionLength = selection.length();
			if (selectionLength == 0) {
				continue;
			}

			String charsetName = selectionEntry.getValue();
			Charset charset = DEFAULT_CHARSET;
			if (!charsetName.isEmpty()) {
				try {
					charset = Charset.forName(charsetName);
				} catch (IllegalArgumentException e) {
					getLogger().warn("Invalid charset name for selection [ {} ]: \"{}\". Defaulting to {}}", selection, charsetName, DEFAULT_CHARSET, e);
				}
			}
			boolean matchHead = selection.charAt(0) == RESOURCE_WILDCARD;
			boolean matchTail = selection.charAt(selectionLength - 1) == RESOURCE_WILDCARD;
			if (matchHead) {
				if (selectionLength == 1) { // A single '*' matches everything
					all = charset;
				} else if (matchTail) {
					middle.put(selection.substring(1, selectionLength - 1), charset);
				} else {
					head.put(selection.substring(1), charset);
				}
			} else if (matchTail) {
				tail.put(selection.substring(0, selectionLength - 1), charset);
			} else {
				exact.put(selection, charset);
			}
		}
		if ((all == null) && exact.isEmpty() && head.isEmpty() && tail.isEmpty() && middle.isEmpty()) {
			return null;
		}
		return new MatchSet(exact, head, tail, middle, all);
	}

	//

	private final Logger logger;

	public Logger getLogger() {
		return logger;
	}

	//

	@Override
	public boolean select(String resourceName) {
		return selectIncluded(resourceName) && !rejectExcluded(resourceName);
	}

	@Override
	public boolean selectIncluded(String resourceName) {
		if (included == null) {
			getLogger().debug("Include [ {} ]: {}", resourceName, "*=UTF-8 (No includes)");
			return true;
		}
		Map.Entry<String, Charset> match = included.match(resourceName);
		if (match != null) {
			getLogger().debug("Include [ {} ]: {}", resourceName, match);
			return true;
		}
		getLogger().debug("Do not include [ {} ]", resourceName);
		return false;
	}

	@Override
	public boolean rejectExcluded(String resourceName) {
		if (excluded == null) {
			getLogger().debug("Do not exclude [ {} ]: {}", resourceName, "No excludes");
			return false;
		}
		Map.Entry<String, Charset> match = excluded.match(resourceName);
		if (match != null) {
			getLogger().debug("Exclude [ {} ]: {}", resourceName, match.getKey());
			return true;
		}
		getLogger().debug("Do not exclude [ {} ]", resourceName);
		return false;
	}

	@Override
	public Charset charset(String resourceName) {
		if (included != null) {
			Map.Entry<String, Charset> match = included.match(resourceName);
			if (match != null) {
				getLogger().trace("Charset [ {} ]: {}", resourceName, match);
				return match.getValue();
			}
		}
		getLogger().trace("Charset [ {} ]: <<default>> {}", resourceName, DEFAULT_CHARSET);
		return DEFAULT_CHARSET;
	}

	static class MatchSet {
		MatchSet(Map<String, Charset> exact, Map<String, Charset> head, Map<String, Charset> tail, Map<String, Charset> middle, Charset all) {
			this.exact = Maps.copyOf(exact);
			this.head = Maps.copyOf(head);
			this.tail = Maps.copyOf(tail);
			this.middle = Maps.copyOf(middle);
			this.all = all;
		}

		private final Map<String, Charset> exact;
		private final Map<String, Charset> head;
		private final Map<String, Charset> tail;
		private final Map<String, Charset> middle;
		private final Charset all;

		Map.Entry<String, Charset> match(String resourceName) {
			Charset charset = exact.get(resourceName);
			if (charset != null) {
				return Maps.entry(resourceName, charset);
			}
			for (Map.Entry<String, Charset> entry : head.entrySet()) {
				if (resourceName.endsWith(entry.getKey())) {
					return entry;
				}
			}
			for (Map.Entry<String, Charset> entry : tail.entrySet()) {
				if (resourceName.startsWith(entry.getKey())) {
					return entry;
				}
			}
			for (Map.Entry<String, Charset> entry : middle.entrySet()) {
				if (resourceName.contains(entry.getKey())) {
					return entry;
				}
			}
			if (all != null) {
				return Maps.entry("*", all);
			}
			return null;
		}
	}
}
