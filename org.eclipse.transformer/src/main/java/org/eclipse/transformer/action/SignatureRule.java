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

package org.eclipse.transformer.action;

import java.util.Map;

import aQute.bnd.signatures.ArrayTypeSignature;
import aQute.bnd.signatures.ClassSignature;
import aQute.bnd.signatures.ClassTypeSignature;
import aQute.bnd.signatures.FieldSignature;
import aQute.bnd.signatures.JavaTypeSignature;
import aQute.bnd.signatures.MethodSignature;
import aQute.bnd.signatures.ReferenceTypeSignature;
import aQute.bnd.signatures.Result;
import aQute.bnd.signatures.SimpleClassTypeSignature;
import aQute.bnd.signatures.ThrowsSignature;
import aQute.bnd.signatures.TypeArgument;
import aQute.bnd.signatures.TypeParameter;

public interface SignatureRule {

	BundleData getBundleUpdate(String symbolicName);

	//

	Map<String, String> getPackageRenames();

	Map<String, String> getPackageVersions();

	//

	public enum SignatureType {
		CLASS,
		FIELD,
		METHOD
	}

	boolean	ALLOW_SIMPLE_SUBSTITUTION	= true;
	boolean	NO_SIMPLE_SUBSTITUTION		= false;

	/**
	 * Replace a single package according to the package rename rules. Package
	 * names must match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 * @return The replacement for the initial package name. Null if no
	 *         replacement is available.
	 */
	String replacePackage(String initialName);

	/**
	 * Replace a single package according to the package rename rules. The
	 * package name has '/' separators, not '.' separators. Package names must
	 * match exactly.
	 *
	 * @param initialName The package name which is to be replaced.
	 * @return The replacement for the initial package name. Null if no
	 *         replacement is available.
	 */
	String replaceBinaryPackage(String initialName);

	/**
	 * Replace all embedded packages of specified text with replacement
	 * packages.
	 *
	 * @param text String embedding zero, one, or more package names.
	 * @param packageRenames map of names and replacement values
	 * @return The text with all embedded package names replaced. Null if no
	 *         replacements were performed.
	 */
	String replacePackages(String text, Map<String, String> packageRenames);

	String replacePackages(String text);

	String transformConstantAsBinaryType(String inputConstant);

	String transformConstantAsBinaryType(String inputConstant, boolean allowSimpleSubstitution);

	/**
	 * Modify a fully qualified type name according to the package rename table.
	 * Answer either the transformed type name, or, if the type name was not
	 * changed, a wrapped null.
	 *
	 * @param inputName A fully qualified type name which is to be transformed.
	 * @return The transformed type name, or a wrapped null if no changed was
	 *         made.
	 */
	String transformBinaryType(String inputName);

	String transformConstantAsDescriptor(String inputConstant);

	String transformConstantAsDescriptor(String inputConstant, boolean allowSimpleSubstitution);

	String transformDescriptor(String inputDescriptor);

	String transformDescriptor(String inputDescriptor, boolean allowSimpleSubstitution);

	/**
	 * Transform a class, field, or method signature. Answer a wrapped null if
	 * the signature is not changed by the transformation rules.
	 *
	 * @param input The signature value which is to be transformed.
	 * @param signatureType The type of the signature value.
	 * @return The transformed signature value. A wrapped null if no change was
	 *         made to the value.
	 */
	String transform(String input, SignatureType signatureType);

	ClassSignature transform(ClassSignature classSignature);

	FieldSignature transform(FieldSignature fieldSignature);

	MethodSignature transform(MethodSignature methodSignature);

	Result transform(Result type);

	ThrowsSignature transform(ThrowsSignature type);

	ArrayTypeSignature transform(ArrayTypeSignature inputType);

	TypeParameter transform(TypeParameter inputTypeParameter);

	ClassTypeSignature transform(ClassTypeSignature inputType);

	SimpleClassTypeSignature transform(SimpleClassTypeSignature inputSignature);

	TypeArgument transform(TypeArgument inputArgument);

	JavaTypeSignature transform(JavaTypeSignature type);

	ReferenceTypeSignature transform(ReferenceTypeSignature type);

	//

	String getDirectString(String initialValue);

	String getConstantString(String initialValue, String clazz);

}
