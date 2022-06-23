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

package org.eclipse.transformer.action.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.util.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import aQute.lib.io.ByteBufferOutputStream;

/**
 * Prototype action for transforming XML resources.
 * <p>
 * Currently unused: XML updates are currently made as text updates.
 * <p>
 * XML updates could be made using an XML parser, with updates driven through
 * values as they appear within the XML text. Both header values (namespaces),
 * element names, text attribute values, and text element values would be
 * updated.
 * <p>
 * An update which uses a parser suffers from a (current) inability to preserve
 * whitespace and comments within the parsed XML text. Also, a parser based
 * action is much more expensive than a string based update. Finally, a better
 * implementation possibly uses XSL transformations.
 */
public class XmlActionImpl extends ElementActionImpl {

	public XmlActionImpl(ActionContext context) {
		super(context);
	}

	//

	@Override
	public String getName() {
		return "XML Action";
	}

	@Override
	public ActionType getActionType() {
		return ActionType.XML;
	}

	// TODO: This check against text substitutions
	// is no longer sufficient. If the XML action is
	// put back in use, the test will need to be updated
	// to include package rename and direct update cases.
	//
	// See issue #308.

	@Override
	public boolean acceptResource(String resourceName, File resourceFile) {
		if (super.acceptResource(resourceName, resourceFile)) {
			if (getSignatureRule().getTextSubstitutions(resourceName) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getAcceptExtension() {
		return ".xml";
	}

	//

	static final boolean XML_AS_PLAIN_TEXT;
	static {
		String value = System.getProperty("XML_AS_PLAIN_TEXT", "true");
		XML_AS_PLAIN_TEXT = Boolean.parseBoolean(value);
	}

	@Override
	public ByteData apply(ByteData inputData) throws TransformException {
		String inputName = inputData.name();

		startRecording(inputData);

		try {
			if (XML_AS_PLAIN_TEXT) {
				return applyAsPlainText(inputData);
			}

			setResourceNames(inputName, inputName);

			Charset charset = inputData.charset();
			ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());

			transformUsingSaxParser(inputName, inputData.stream(), charset, outputStream);

			if (!isChanged()) {
				return inputData;
			} else {
				return new ByteDataImpl(inputName, outputStream.toByteBuffer(), charset);
			}

		} finally {
			stopRecording(inputData);
		}
	}

	public ByteData applyAsPlainText(ByteData inputData) throws TransformException {
		String inputName = inputData.name();

		setResourceNames(inputName, inputName);

		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputData.length());

		Charset charset = inputData.charset();
		try (BufferedReader reader = inputData.reader(); BufferedWriter writer = FileUtils.writer(outputStream, charset)) {
			transformAsPlainText(inputName, reader, writer);
		} catch (IOException e) {
			throw new TransformException("Failed to transform [ " + inputName + " ]", e);
		}

		if (!isChanged()) {
			return inputData;
		} else {
			return new ByteDataImpl(inputName, outputStream.toByteBuffer(), charset);
		}
	}

	//

	/**
	 * Create and return a SAXParserFactory instance.
	 * <p>
	 * The returned SAXParserFactory is configured to avoid XML External Entity
	 * (XXE) attacks.
	 *
	 * @return A properly configured SAXParserFactory instance.
	 */
	public SAXParserFactory newSAXParserFactory() {
		SAXParserFactory instance = SAXParserFactory.newInstance();
		instance.setXIncludeAware(false);
		try {
			instance.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl", true);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				getLogger().info(
					"Unable to set feature to disallow DTD (doctypes): XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-general-entities",
					false);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				getLogger().info(
					"Unable to set feature to disallow external-general-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		try {
			instance.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
			try { // Xerces 2 only fallback
				instance.setFeature("http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities",
					false);
			} catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e2) {
				getLogger().info(
					"Unable to set feature to disallow external-parameter-entities: XML External Entity (XXE) attack risk",
					e);
			}
		}
		return instance;
	}

	public void transform(String inputName, InputStream input, Charset charset, OutputStream output) throws TransformException {
		InputSource inputSource = new InputSource(input);
		inputSource.setEncoding(charset.name());

		XMLContentHandler handler = new XMLContentHandler(inputName, inputSource, output);

		SAXParser parser;
		try {
			parser = newSAXParserFactory().newSAXParser();
		} catch (Exception e) {
			throw new TransformException("Failed to obtain parser for [ " + inputName + " ]", e);
		}

		try {
			parser.parse(input, handler);
		} catch (Exception e) {
			throw new TransformException("Failed to parse [ " + inputName + " ]", e);
		}
	}

	public void transformUsingSaxParser(String inputName, InputStream input, Charset charset, OutputStream output)
		throws TransformException {
		InputSource inputSource = new InputSource(input);
		inputSource.setEncoding(charset.name());

		XMLContentHandler handler = new XMLContentHandler(inputName, inputSource, output);

		SAXParser parser;
		try {
			parser = newSAXParserFactory().newSAXParser();
		} catch (Exception e) {
			throw new TransformException("Failed to obtain parser for [ " + inputName + " ]", e);
		}

		try {
			parser.parse(inputSource, handler);
		} catch (Exception e) {
			throw new TransformException("Failed to parse [ " + inputName + " ]", e);
		}
	}

	protected void transformAsPlainText(String inputName, BufferedReader reader, BufferedWriter writer)
		throws IOException {

		String inputLine;
		while ((inputLine = reader.readLine()) != null) {
			String outputLine = replaceText(inputName, inputLine);
			if (outputLine == null) {
				outputLine = inputLine;
			} else {
				addReplacement();
			}
			writer.write(outputLine);
			writer.write('\n');
		}
	}

	//

	public class XMLContentHandler extends DefaultHandler {
		public XMLContentHandler(String inputName, InputSource inputSource, OutputStream outputStream) {
			this.inputName = inputName;
			this.charset = Charset.forName(inputSource.getEncoding());
			this.publicId = inputSource.getPublicId();
			this.systemId = inputSource.getSystemId();

			this.outputStream = outputStream;

			this.lineBuilder = new StringBuilder();
		}

		//

		private final String		inputName;

		private final String		publicId;
		private final String		systemId;
		private Charset				charset;

		private final OutputStream	outputStream;

		public String getInputName() {
			return inputName;
		}

		public Charset getCharset() {
			return charset;
		}

		public String getPublicId() {
			return publicId;
		}

		public String getSystemId() {
			return systemId;
		}

		//

		public OutputStream getOutputStream() {
			return outputStream;
		}

		public void write(String text) throws SAXException {
			write(text, getCharset());
		}

		public void writeUTF8(String text) throws SAXException {
			write(text, UTF_8);
		}

		public void write(String text, Charset useCharset) throws SAXException {
			try {
				outputStream.write(text.getBytes(useCharset));
			} catch (IOException e) {
				throw new SAXException("Failed to write [ " + text + " ]", e);
			}
		}

		//

		private final StringBuilder lineBuilder;

		protected void appendLine() {
			lineBuilder.append('\n');
		}

		protected void append(char c) {
			lineBuilder.append(c);
		}

		protected void append(char[] buffer, int start, int length) {
			for (int trav = start; trav < start + length; trav++) {
				lineBuilder.append(buffer[trav]);
			}
		}

		protected void appendLine(char c) {
			lineBuilder.append(c)
				.append('\n');
		}

		protected void append(String text) {
			getLogger().trace("append [{}]", text);
			lineBuilder.append(text);
		}

		protected void appendLine(String text) {
			getLogger().trace("appendline [{}]", text);
			lineBuilder.append(text)
				.append('\n');
		}

		protected void emit() throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			write(nextLine);
		}

		@SuppressWarnings("unused")
		protected void emitLineUTF8(String text) throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			writeUTF8(nextLine);
		}

		//

		@Override
		public void startDocument() throws SAXException {
			String charsetName = getCharset().name();
			emitLineUTF8("<?xml version = \"1.0\" encoding = \"" + charsetName + "\"?>\n");
		}

		// @Override
		// public void endDocument() throws SAXException {
		// super.endDocument();
		// }
		//
		// @Override
		// public void setDocumentLocator(Locator locator) {
		// super.setDocumentLocator(locator);
		// }

		@SuppressWarnings("unused")
		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			append("<?");
			append(target);
			if ((data != null) && data.length() > 0) {
				getLogger().trace("processingInstruction [ {} ]", data);
				append(' ');
				append(data);
			}
			append("?>");
		}

		//

		// @Override
		// public void startPrefixMapping(String prefix, String uri) throws
		// SAXException {
		// super.startPrefixMapping(prefix, uri);
		// }
		//
		// @Override
		// public void endPrefixMapping(String prefix) throws SAXException {
		// super.endPrefixMapping(prefix);
		// }

		//

		@SuppressWarnings("unused")
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
			getLogger().trace("startElement: uri [ {} ] localName [ {} ] qName [ {} ] attributes [ {} ]", uri,
				localName, qName, attributes);
			append('<' + localName);
			append(uri);

			if (attributes != null) {
				int numberAttributes = attributes.getLength();
				for (int i = 0; i < numberAttributes; i++) {
					String attrQName = attributes.getQName(i);
					String attrValue = attributes.getValue(i);

					getLogger().trace("startElement getQName({}) [ {} ]", i, attrQName);
					getLogger().trace("startElement getValue({}) [ {} ]", i, attrValue);
					append(' ');
					append(attrQName);
					append("=\"");
					append(attrValue);
					append('"');
				}
			}

			appendLine('>');

			emit();
		}

		@SuppressWarnings("unused")
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			getLogger().trace("endElement: uri [ {} ] localName [ {} ] qName [ {} ]", uri, localName, qName);
			append("</");
			append(localName + '>');
		}

		@SuppressWarnings("unused")
		@Override
		public void characters(char[] chars, int start, int length) throws SAXException {
			String initialText = new String(chars, start, length);
			String finalText = replaceText(inputName, initialText);
			if (finalText == null) {
				finalText = initialText;
				getLogger().trace("characters [ {} ] (unchanged)", initialText);
			} else {
				getLogger().debug("characters [ {} ] -> [ {} ]", initialText, finalText);
				addReplacement();
			}
			append(finalText);
		}

		@SuppressWarnings("unused")
		@Override
		public void ignorableWhitespace(char[] whitespace, int start, int length) throws SAXException {
			append(whitespace, start, length);
		}

		// @Override
		// public void skippedEntity(String name) throws SAXException {
		// super.skippedEntity(name);
		// }
	}
}
