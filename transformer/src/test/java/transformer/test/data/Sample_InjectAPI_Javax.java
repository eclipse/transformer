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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

public class Sample_InjectAPI_Javax {
	public static void method2(int intParm) {
		// Empty
	}

	public void method1(int intParm) {
		// Empty
	}

	// Use of @Inject and Provider

	@Inject
	public Sample_InjectAPI_Javax(Provider<SampleValue> sampleValueProvider) {
		this.sampleValue = sampleValueProvider.get();
	}

	public static class SampleValue {
		public int value; 
	}

	protected SampleValue sampleValue;

	public SampleValue getSampleValue() {
		return sampleValue;
	}

	// Basic use of @Inject

	@Inject
	protected static long injectedLong;

	@Inject
	protected int injectedInt;

	// @Named use of @Inject

	@Inject
	@Named("sample1")
	protected Sample_InjectAPI_Javax injectedSample1;

	@Inject
	@Named("sample2")
	protected Sample_InjectAPI_Javax injectedSample2;

	// Use of @Qualifier

	@Qualifier
	public @interface Color {
		Value value() default Value.RED;
		public enum Value { RED, BLUE, YELLOW }
	}

	@Inject
	@Color(Color.Value.BLUE)
	protected String injectedString2;

	// Use of @Scope

	@Scope
	public @interface Lifetime {
		Value value() default Value.INSTANCE;
		public enum Value { GLOBAL, INSTANCE }
	}	

	@Inject
	@Lifetime(Lifetime.Value.GLOBAL)
	protected String injectedString1;
}
