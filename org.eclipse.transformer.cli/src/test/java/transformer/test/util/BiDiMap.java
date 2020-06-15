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
import java.util.Set;

public interface BiDiMap<Holder, Held> {
	String getHashText();

	void log(PrintWriter writer);

	//

	Class<Holder> getHolderClass();

	String getHolderTag();

	Class<Held> getHeldClass();

	String getHeldTag();

	//

	boolean isEmpty();

	boolean holds(Holder holder, Held held);

	boolean isHolder(Holder hold);

	Set<Holder> getHolders();

	Set<Held> getHeld(Holder holder);

	boolean isHeld(Held held);

	Set<Held> getHeld();

	Set<Holder> getHolders(Held held);

	//

	boolean record(Holder holder, Held held);

	<OtherHolder extends Holder, OtherHeld extends Held> void record(BiDiMap<OtherHolder, OtherHeld> otherMap);

	<OtherHolder extends Holder, OtherHeld extends Held> void record(BiDiMap<OtherHolder, OtherHeld> otherMap,
		Set<? extends Holder> restrictedHolders);

	//

	<OtherHolder extends Holder, OtherHeld extends Held> boolean sameAs(BiDiMap<OtherHolder, OtherHeld> otherMap);
}
