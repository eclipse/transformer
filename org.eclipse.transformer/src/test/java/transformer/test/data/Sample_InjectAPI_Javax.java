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

package transformer.test.data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

public class Sample_InjectAPI_Javax {
	public static void method2(@SuppressWarnings("unused") int intParm) {
		// Empty
	}

	public void method1(@SuppressWarnings("unused") int intParm) {
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
	protected static long				injectedLong;

	@Inject
	protected int						injectedInt;

	// @Named use of @Inject

	@Inject
	@Named("sample1")
	protected Sample_InjectAPI_Javax	injectedSample1;

	@Inject
	@Named("sample2")
	protected Sample_InjectAPI_Javax	injectedSample2;

	// Use of @Qualifier

	@Qualifier
	public @interface Color {
		Value value() default Value.RED;

		public enum Value {
			RED,
			BLUE,
			YELLOW
		}
	}

	@Inject
	@Color(Color.Value.BLUE)
	protected String injectedString2;

	// Use of @Scope

	@Scope
	public @interface Lifetime {
		Value value() default Value.INSTANCE;

		public enum Value {
			GLOBAL,
			INSTANCE
		}
	}

	@Inject
	@Lifetime(Lifetime.Value.GLOBAL)
	protected String injectedString1;
}
