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

package transformer.test.data;

public class Sample_DirectStrings {
	public static final String DIRECT1   = "DIRECT_1";
	public static final String DIRECT12  = "DIRECT_1_2";
	public static final String DIRECT21  = "2_DIRECT_1";
	public static final String DIRECT212 = "2_DIRECT_1_2";
	
	static {
		System.out.println("Sample value 1");
		System.out.println("Name: Fred");
	}
}
