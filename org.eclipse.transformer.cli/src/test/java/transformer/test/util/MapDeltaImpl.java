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
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDeltaImpl<K, V> implements Delta {
	public static final String CLASS_NAME = MapDeltaImpl.class.getSimpleName();

	//

	private Map<K, V> createMap(boolean enabled, int expected) {
		if (!enabled) {
			return null;
		} else if (expected == ANY_NUMBER) {
			return new HashMap<>();
		} else if (expected == 0) {
			return null;
		} else {
			return new HashMap<>(expected);
		}
	}

	private Map<K, V[]> createValueMap(boolean enabled, int expected) {
		if (!enabled) {
			return null;
		} else if (expected == ANY_NUMBER) {
			return new HashMap<>();
		} else if (expected == 0) {
			return null;
		} else {
			return new HashMap<>(expected);
		}
	}

	//

	public MapDeltaImpl(Class<K> keyClass, Class<V> valueClass) {
		this(keyClass, valueClass, DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_RECORD_CHANGED, !DO_RECORD_STILL);
	}

	public MapDeltaImpl(Class<K> keyClass, Class<V> valueClass, int expectedAdded, int expectedRemoved) {

		this(keyClass, valueClass, DO_RECORD_ADDED, DO_RECORD_REMOVED, DO_RECORD_CHANGED, !DO_RECORD_STILL,
			expectedAdded, expectedRemoved, ZERO_CHANGED, ZERO_STILL);
	}

	public MapDeltaImpl(Class<K> keyClass, Class<V> valueClass, boolean recordAdded, boolean recordRemoved,
		boolean recordChanged, boolean recordStill) {

		this(keyClass, valueClass, recordAdded, recordRemoved, recordChanged, recordStill, ANY_NUMBER_OF_ADDED,
			ANY_NUMBER_OF_REMOVED, ANY_NUMBER_OF_CHANGED, ANY_NUMBER_OF_STILL);
	}

	public MapDeltaImpl(Class<K> keyClass, Class<V> valueClass, boolean recordAdded, boolean recordRemoved,
		boolean recordChanged, boolean recordStill, int expectedAdded, int expectedRemoved, int expectedChanged,
		int expectedStill) {

		this.keyClass = keyClass;
		this.valueClass = valueClass;

		this.hashText = getClass().getSimpleName() + "<" + keyClass.getSimpleName() + "," + valueClass.getSimpleName()
			+ ">" + "@" + Integer.toHexString(hashCode());

		this.addedMap = createMap(recordAdded, expectedAdded);
		this.removedMap = createMap(recordRemoved, expectedRemoved);
		this.changedMap = createValueMap(recordChanged, expectedChanged);
		this.stillMap = createMap(recordStill, expectedStill);
	}

	//

	protected final Class<K> keyClass;

	public Class<K> getKeyClass() {
		return keyClass;
	}

	protected final Class<V> valueClass;

	public Class<V> getValueClass() {
		return valueClass;
	}

	//

	protected final String hashText;

	@Override
	public String getHashText() {
		return hashText;
	}

	//

	protected final Map<K, V>	addedMap;
	protected final Map<K, V>	removedMap;
	protected final Map<K, V[]>	changedMap;
	protected final Map<K, V>	stillMap;

	public Map<K, V> getAddedMap() {
		return addedMap;
	}

	public void recordAdded(K addedKey_f, V addedValue_f) {
		if (addedMap != null) {
			addedMap.put(addedKey_f, addedValue_f);
		}
	}

	public void recordAdded(Map<K, V> added_f) {
		if (addedMap != null) {
			addedMap.putAll(added_f);
		}
	}

	public boolean isNullAdded() {
		return ((addedMap == null) || addedMap.isEmpty());
	}

	public Map<K, V> getRemovedMap() {
		return removedMap;
	}

	public void recordRemoved(K removedKey_i, V removedValue_i) {
		if (removedMap != null) {
			removedMap.put(removedKey_i, removedValue_i);
		}
	}

	public void recordRemoved(Map<K, V> removed_i) {
		if (removedMap != null) {
			removedMap.putAll(removed_i);
		}
	}

	public final boolean	AS_ADDED				= true;
	public final boolean	AS_REMOVED				= false;

	public final int		FINAL_VALUE_OFFSET		= 0;
	public final int		INITIAL_VALUE_OFFSET	= 0;

	public void recordTransfer(Map<K, V> transferMap, boolean asAdded) {
		if (asAdded) {
			recordAdded(transferMap);
		} else {
			recordRemoved(transferMap);
		}
	}

	public boolean isNullRemoved() {
		return ((removedMap == null) || removedMap.isEmpty());
	}

	public Map<K, V[]> getChangedMap() {
		return changedMap;
	}

	public void recordChanged(K changedKey_f, V value_f, V value_i) {
		if (changedMap != null) {
			@SuppressWarnings("unchecked")
			V[] valueChange = (V[]) Array.newInstance(valueClass, 2);
			valueChange[FINAL_VALUE_OFFSET] = value_f;
			valueChange[INITIAL_VALUE_OFFSET] = value_i;

			changedMap.put(changedKey_f, valueChange);
		}
	}

	public boolean isNullChanged() {
		return ((changedMap == null) || changedMap.isEmpty());
	}

	public Map<K, V> getStillMap() {
		return stillMap;
	}

	public void recordStill(K stillKey_f, V stillValue_f) {
		if (stillMap != null) {
			stillMap.put(stillKey_f, stillValue_f);
		}
	}

	public void recordStill(Map<K, V> still) {
		if (stillMap != null) {
			stillMap.putAll(still);
		}
	}

	public boolean isNullUnchanged() {
		return ((stillMap == null) || stillMap.isEmpty());
	}

	@Override
	public boolean isNull() {
		return (isNullAdded() && isNullRemoved() && isNullChanged());
	}

	//

	public void subtract(Map<K, V> finalMap, Map<K, V> initialMap) {
		if (((finalMap == null) || finalMap.isEmpty()) && ((initialMap == null) || initialMap.isEmpty())) {
			// Nothing to do: Both map are empty.

		} else if ((finalMap == null) || finalMap.isEmpty()) {
			// Everything in the initial map was removed.
			if (removedMap != null) {
				removedMap.putAll(initialMap);
			}

		} else if ((initialMap == null) || initialMap.isEmpty()) {
			// Everything in the final map was added.
			if (addedMap != null) {
				addedMap.putAll(finalMap);
			}

		} else {
			for (Map.Entry<K, V> entry_f : finalMap.entrySet()) {
				K key_f = entry_f.getKey();
				V value_f = entry_f.getValue();

				if (!initialMap.containsKey(key_f)) {
					addedMap.put(key_f, value_f);

				} else {
					V value_i = initialMap.get(key_f);

					if (((value_i == null) && (value_f != null)) || ((value_i != null) && (value_f == null))
						|| ((value_i != null) && (value_f != null) && !value_f.equals(value_i))) {
						if (changedMap != null) {
							recordChanged(key_f, value_f, value_i);
						}
					} else {
						if (stillMap != null) {
							recordStill(key_f, value_f);
						}
					}
				}
			}

			for (Map.Entry<K, V> entry_i : initialMap.entrySet()) {
				K key_i = entry_i.getKey();
				V value_i = entry_i.getValue();

				if (!finalMap.containsKey(key_i)) {
					addedMap.put(key_i, value_i);

				} else {
					// Changes should have already been recorded
					// when processing the final map.
					//
					// Note that 'recordChanged' in this case exposes
					// a weakness of how changes are recorded, in that
					// that record has a bias towards the final key.

					// V value_f = finalMap.get(key_f);

					// if ( ((value_f == null) && (value_i != null)) ||
					// ((value_f != null) && (value_i == null)) ||
					// ((value_f != null) && (value_i != null) &&
					// !value_i.equals(value_f)) ) {
					// if ( changedMap_f != null ) {
					// recordChanged(key_i, value_i, value_f);
					// }
					// } else {
					// if ( stillMap != null ) {
					// recordStill(initialKey, initialValue);
					// }
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
			writer.println(prefix + "** UNCHANGED **");
			return;
		}

		writer.println(prefix + "Added:");
		if (addedMap == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (addedMap.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			int addedNo = 0;
			for (Map.Entry<K, V> addedEntry : addedMap.entrySet()) {
				if (addedNo > 3) {
					writer.println(prefix + "  [ ... " + addedMap.entrySet()
						.size() + " ]");
					break;
				} else {
					writer.println(
						prefix + "  [ " + addedNo + " ] " + addedEntry.getKey() + ": " + addedEntry.getValue());
				}
				addedNo++;
			}
		}

		writer.println(prefix + "Removed:");
		if (removedMap == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (removedMap.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			int removedNo = 0;
			for (Map.Entry<K, V> removedEntry : removedMap.entrySet()) {
				if (removedNo > 3) {
					writer.println(prefix + "  [ ... " + removedMap.entrySet()
						.size() + " ]");
					break;
				} else {
					writer.println(
						prefix + "  [ " + removedNo + " ] " + removedEntry.getKey() + ": " + removedEntry.getValue());
				}
				removedNo++;
			}
		}

		writer.println(prefix + "Changed:");
		if (changedMap == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (changedMap.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			int changedNo = 0;
			for (Map.Entry<K, V[]> changedEntry : changedMap.entrySet()) {
				if (changedNo > 3) {
					writer.println(prefix + "  [ ... " + changedMap.entrySet()
						.size() + " ]");
					break;
				} else {
					V[] valueDelta = changedEntry.getValue();
					writer.println(prefix + "  " + changedEntry.getKey() + ": " + valueDelta[FINAL_VALUE_OFFSET]
						+ " :: ( " + valueDelta[INITIAL_VALUE_OFFSET] + " )");
				}
				changedNo++;
			}
		}

		writer.println(prefix + "Still:");
		if (stillMap == null) {
			writer.println(prefix + "  ** NOT RECORDED **");
		} else if (stillMap.isEmpty()) {
			writer.println(prefix + "  ** NONE **");
		} else {
			writer.println(prefix + "  [ " + stillMap.entrySet()
				.size() + " ]");
			// for ( Map.Entry<K, V> stillEntry : stillMap.entrySet() ){
			// writer.println(CLASS_NAME, methodName,
			// " {0}: {1}",
			// stillEntry.getKey(), stillEntry.getValue());
			// }
		}
	}

	public void describe(String prefix, List<String> nonNull) {
		if (!isNullAdded()) {
			nonNull.add(prefix + " Added [ " + getAddedMap().keySet()
				.size() + " ]");
		}
		if (!isNullRemoved()) {
			nonNull.add(prefix + " Removed [ " + getRemovedMap().keySet()
				.size() + " ]");
		}
	}
}
