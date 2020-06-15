/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package transformer.test.util;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetDeltaImpl<E> implements Delta {
	public static final String CLASS_NAME = SetDeltaImpl.class.getSimpleName();

	//

	public static SetDeltaImpl<String> stringDelta(String elementTag) {
		return new SetDeltaImpl<>(String.class, elementTag, DO_RECORD_ADDED, DO_RECORD_REMOVED, !DO_RECORD_STILL);
	}

	public SetDeltaImpl(Class<E> elementClass, String elementTag, boolean recordAdded, boolean recordRemoved,
		boolean recordStill) {

		this.hashText = getClass().getSimpleName() + "<" + elementClass.getSimpleName() + ">" + "@"
			+ Integer.toHexString(hashCode()) + "(" + elementTag + ")";

		this.elementClass = elementClass;
		this.elementTag = elementTag;

		this.added_f = (recordAdded ? new HashSet<>() : null);
		this.removed_i = (recordRemoved ? new HashSet<>() : null);
		this.still_f = (recordStill ? new HashSet<>() : null);
	}

	//

	protected final Class<E> elementClass;

	public Class<E> getElementClass() {
		return elementClass;
	}

	protected final String elementTag;

	public String getElementTag() {
		return elementTag;
	}

	//

	protected final String hashText;

	@Override
	public String getHashText() {
		return hashText;
	}

	// All elements of 'added' are from the domain of the final set.
	//
	// All elements of 'removed' are from the domain of the initial set.
	//
	// Currently, all elements of 'still' are from the final set, but,
	// there *could* be two still sets, one for each of the sets.
	// That would be potentially useful for keeping values from
	// the initial and final domains apart.

	protected final Set<E>	added_f;
	protected final Set<E>	removed_i;
	protected final Set<E>	still_f;
	// protected final Set<E> still_i;

	public Set<E> getAdded() {
		return ((added_f == null) ? null : added_f);
	}

	public boolean isNullAdded() {
		return ((added_f == null) || added_f.isEmpty());
	}

	public Set<E> getRemoved() {
		return ((removed_i == null) ? null : removed_i);
	}

	public boolean isNullRemoved() {
		return ((removed_i == null) || removed_i.isEmpty());
	}

	public Set<E> getStill() {
		return ((still_f == null) ? null : still_f);
	}

	public boolean isNullStill() {
		return ((still_f == null) || still_f.isEmpty());
	}

	@Override
	public boolean isNull() {
		return (isNullAdded() && isNullRemoved());
	}

	public boolean isNull(boolean ignoreRemoved) {
		return (isNullAdded() && (ignoreRemoved || isNullRemoved()));
	}

	//

	public void describe(String prefix, List<String> nonNull) {
		if (!isNullAdded()) {
			nonNull.add(prefix + " Added [ " + getAdded().size() + " ]");
		}
		if (!isNullRemoved()) {
			nonNull.add(prefix + " Removed [ " + getAdded().size() + " ]");
		}
	}

	//

	public void subtract(E[] array_f, E[] array_i) {
		if (((array_f == null) || (array_f.length == 0)) && ((array_i == null) || (array_i.length == 0))) {
			// Nothing to do: Both lists are empty.

		} else if ((array_f == null) || (array_f.length == 0)) {
			// Everything in the initial list was removed.
			if (removed_i != null) {
				for (E e_i : array_i) {
					removed_i.add(e_i);
				}
			}

		} else if ((array_i == null) || (array_i.length == 0)) {
			// Everything in the final list was added.
			if (added_f != null) {
				for (E e_f : array_f) {
					added_f.add(e_f);
				}
			}

		} else {
			// Two non-empty arrays: Have to do the work of comparing them.

			Set<E> set_i = new HashSet<>(array_i.length);
			for (E e_i : array_i) {
				set_i.add(e_i);
			}
			Set<E> set_f = new HashSet<>(array_f.length);
			for (E e_f : array_f) {
				set_f.add(e_f);
			}

			for (E element_f : set_f) {
				if (!set_i.contains(element_f)) {
					if (added_f != null) {
						added_f.add(element_f);
					}
				} else {
					if (still_f != null) {
						still_f.add(element_f);
					}
				}
			}

			for (E element_i : set_i) {
				if (!set_f.contains(element_i)) {
					if (removed_i != null) {
						removed_i.add(element_i);
					}
				} else {
					// if ( still_i != null ) {
					// still_i.add(element_i);
					// }
				}
			}
		}
	}

	public void subtract(List<E> list_f, List<E> list_i) {
		if (((list_f == null) || list_f.isEmpty()) && ((list_i == null) || list_i.isEmpty())) {
			// Nothing to do: Both lists are empty.

		} else if ((list_f == null) || list_f.isEmpty()) {
			// Everything in the initial list was removed.
			if (removed_i != null) {
				removed_i.addAll(list_i);
			}

		} else if ((list_i == null) || list_i.isEmpty()) {
			// Everything in the final list was added.
			if (added_f != null) {
				added_f.addAll(list_f);
			}

		} else {
			// Two non-empty lists: Have to do the work of comparing them.

			Set<E> set_i = new HashSet<>(list_i);
			Set<E> set_f = new HashSet<>(list_f);

			for (E element_f : set_f) {
				if (!set_i.contains(element_f)) {
					if (added_f != null) {
						added_f.add(element_f);
					}
				} else {
					if (still_f != null) {
						still_f.add(element_f);
					}
				}
			}

			for (E element_i : set_i) {
				if (!set_f.contains(element_i)) {
					if (removed_i != null) {
						removed_i.add(element_i);
					}
				} else {
					// if ( still_i != null ) {
					// still_i.add(element_i);
					// }
				}
			}
		}
	}

	public void subtract(Set<E> set_f, Set<E> set_i) {
		if (((set_f == null) || set_f.isEmpty()) && ((set_i == null) || set_i.isEmpty())) {
			// Nothing to do: Both sets are empty.

		} else if ((set_f == null) || set_f.isEmpty()) {
			// Everything in the initial set was removed.
			if (removed_i != null) {
				removed_i.addAll(set_i);
			}

		} else if ((set_i == null) || set_i.isEmpty()) {
			// Everything in the final set was added.
			if (added_f != null) {
				added_f.addAll(set_f);
			}

		} else {
			// Two non-empty sets: Have to do the work of comparing them.

			for (E element_f : set_f) {
				if (!set_i.contains(element_f)) {
					if (added_f != null) {
						added_f.add(element_f);
					}
				} else {
					if (still_f != null) {
						still_f.add(element_f);
					}
				}
			}

			for (E element_i : set_i) {
				if (!set_f.contains(element_i)) {
					if (removed_i != null) {
						removed_i.add(element_i);
					}
				} else {
					// if ( still_i != null ) {
					// still_i.add(element_i);
					// }
				}
			}
		}
	}

	//

	@Override
	public void log(PrintWriter writer) {
		String methodName = "log";

		String prefix = CLASS_NAME + ": " + methodName + ": ";

		if (isNull()) {
			if (still_f == null) {
				writer.println(prefix + "** UNCHANGED **");
			} else {
				writer.println(prefix + "** UNCHANGED [ " + still_f.size() + " ] **");
			}
			return;
		}

		writer.println(prefix + "Added:");
		if (added_f == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (added_f.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			int addedNo = 0;
			for (E addedElement : added_f) {
				if (addedNo > 3) {
					writer.println(prefix + "  [ ... " + added_f.size() + " ]");
					break;
				} else {
					writer.println(prefix + "  [ " + addedNo + " ]  " + addedElement);
				}
				addedNo++;
			}
		}

		writer.println(prefix + "Removed:");
		if (removed_i == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (removed_i.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			int removedNo = 0;
			for (E removedElement : removed_i) {
				if (removedNo > 3) {
					writer.println(prefix + "  [ ... " + removed_i.size() + " ]");
					break;
				} else {
					writer.println(prefix + "  [ " + removedNo + " ]  " + removedElement);
				}
				removedNo++;
			}
		}

		writer.println(prefix + "Still:");
		if (still_f == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (still_f.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			writer.println(prefix + "  [ " + still_f.size() + " ]");
			// for ( E stillElement : still ){
			// useLogger.println(prefix + " " + stillElement);
			// }
		}
	}
}
