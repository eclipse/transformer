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

package transformer.test.data;

import transformer.test.data.javax.repeat.Sample_Repeated;
import transformer.test.data.javax.repeat.Sample_Repeats;

@Sample_Repeated(value = 1, name = "one")
@Sample_Repeated(value = 2, name = "two")
public class Sample_Repeat_Target {
	@Sample_Repeats(value = {
		@Sample_Repeated(value = 3, name = "three"), @Sample_Repeated(value = 4, name = "four")
	})
	public int testMethod() {
		return 1;
	}
}
