/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	<OtherHolder extends Holder, OtherHeld extends Held>
	    void record(BiDiMap<OtherHolder, OtherHeld> otherMap);
	
	<OtherHolder extends Holder, OtherHeld extends Held>
	    void record(BiDiMap<OtherHolder, OtherHeld> otherMap,
			        Set<? extends Holder> restrictedHolders);

	//

	<OtherHolder extends Holder, OtherHeld extends Held>
	    boolean sameAs(BiDiMap<OtherHolder, OtherHeld> otherMap);
}
