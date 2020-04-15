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

public interface Delta {

	public static boolean strCmp(String str1, String str2) {
		if ( (str1 == null) && (str2 == null) ) {
			return true;
		} else if ( (str1 == null) || (str2 == null) ) {
			return false;
		} else {
			return str1.equals(str2);
		}
	}

	//

    boolean DO_RECORD_ADDED = true;
    boolean DO_RECORD_REMOVED = true;
    boolean DO_RECORD_CHANGED = true;
    boolean DO_RECORD_STILL = true;

    int ANY_NUMBER = -1;

    int ANY_NUMBER_OF_ADDED = ANY_NUMBER;
    int ANY_NUMBER_OF_REMOVED = ANY_NUMBER;
    int ANY_NUMBER_OF_CHANGED = ANY_NUMBER;
    int ANY_NUMBER_OF_STILL = ANY_NUMBER;

    int ZERO_ADDED = 0;
    int ZERO_REMOVED = 0;
    int ZERO_CHANGED = 0;
    int ZERO_STILL = 0;

    //

    String getHashText();

    boolean isNull();

    void log(PrintWriter writer);
}
