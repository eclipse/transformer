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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ActionType;
import org.eclipse.transformer.util.ByteData;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

public class XmlActionImpl extends ActionImpl {

	public XmlActionImpl(Logger logger, boolean isTerse, boolean isVerbose, InputBufferImpl buffer,
		SelectionRuleImpl selectionRule, SignatureRuleImpl signatureRule) {

		super(logger, isTerse, isVerbose, buffer, selectionRule, signatureRule);
	}

	//

	@Override
	public String getName() {
		return "XML Action";
	}

	@Override
	public ActionType getActionType() {
		// return ActionType.XML;
		return null; // THis action is disabled.
	}

	@Override
	public String getAcceptExtension() {
		return ".xml";
	}

	@Override
	public boolean accept(String resourceName, File resourceFile) {
		if (resourceName.toLowerCase()
			.endsWith(getAcceptExtension())) {
			if (signatureRule.getTextSubstitutions(resourceName) != null) {
				return true;
			}
		}
		return false;
	}

	//

	static final boolean XML_AS_PLAIN_TEXT;
	static {
		String value = System.getProperty("XML_AS_PLAIN_TEXT", "true");
		XML_AS_PLAIN_TEXT = Boolean.valueOf(value);
	}

	@Override
	public ByteData apply(String inputName, byte[] inputBytes, int inputCount) throws TransformException {
		if (XML_AS_PLAIN_TEXT) {
			return applyAsPlainText(inputName, inputBytes, inputCount);
		}

		setResourceNames(inputName, inputName);

		InputStream inputStream = new ByteBufferInputStream(inputBytes, 0, inputCount);
		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputCount);

		transformUsingSaxParser(inputName, inputStream, outputStream);

		if (!hasNonResourceNameChanges()) {
			return null;

		} else {
			byte[] outputBytes = outputStream.toByteArray();
			return new ByteData(inputName, outputBytes, 0, outputBytes.length);
		}
	}

	@SuppressWarnings("unused")
	public ByteData applyAsPlainText(String inputName, byte[] inputBytes, int inputLength) throws TransformException {

		String outputName = inputName;

		setResourceNames(inputName, outputName);

		InputStream inputStream = new ByteBufferInputStream(inputBytes, 0, inputLength);
		InputStreamReader inputReader = new InputStreamReader(inputStream, UTF_8);

		BufferedReader reader = new BufferedReader(inputReader);

		ByteBufferOutputStream outputStream = new ByteBufferOutputStream(inputBytes.length);
		OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, UTF_8);

		BufferedWriter writer = new BufferedWriter(outputWriter);

		try {
			transformAsPlainText(inputName, reader, writer); // throws
																// IOException
		} catch (IOException e) {
			error("Failed to transform [ {} ]", e, inputName);
			return null;
		}

		try {
			writer.flush(); // throws
		} catch (IOException e) {
			error("Failed to flush [ {} ]", e, inputName);
			return null;
		}

		if (!hasNonResourceNameChanges()) {
			return null;
		}

		byte[] outputBytes = outputStream.toByteArray();
		return new ByteData(inputName, outputBytes, 0, outputBytes.length);
	}

	//

	private static final SAXParserFactory parserFactory;

	static {
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
	}

	public static SAXParserFactory getParserFactory() {
		return parserFactory;
	}

	public void transform(String inputName, InputStream input, OutputStream output) throws TransformException {
		InputSource inputSource = new InputSource(input);
		inputSource.setEncoding(UTF_8.name());

		XMLContentHandler handler = new XMLContentHandler(inputName, inputSource, output);

		SAXParser parser;
		try {
			parser = getParserFactory().newSAXParser();
			// 'newSAXParser' throws ParserConfigurationException, SAXException
		} catch (Exception e) {
			throw new TransformException("Failed to obtain parser for [ " + inputName + " ]", e);
		}

		try {
			parser.parse(input, handler); // throws SAXException, IOException
		} catch (Exception e) {
			throw new TransformException("Failed to parse [ " + inputName + " ]", e);
		}
	}

	public void transformUsingSaxParser(String inputName, InputStream input, OutputStream output)
		throws TransformException {
		InputSource inputSource = new InputSource(input);
		inputSource.setEncoding(UTF_8.name());

		XMLContentHandler handler = new XMLContentHandler(inputName, inputSource, output);

		SAXParser parser;
		try {
			parser = getParserFactory().newSAXParser();
			// 'newSAXParser' throws ParserConfigurationException, SAXException
		} catch (Exception e) {
			throw new TransformException("Failed to obtain parser for [ " + inputName + " ]", e);
		}

		try {
			parser.parse(input, handler); // throws SAXException, IOException
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
			lineBuilder.append(c);
			lineBuilder.append('\n');
		}

		protected void append(String text) {
			debug("appending [" + text + "]");
			lineBuilder.append(text);
		}

		protected void appendLine(String text) {
			debug("appendline[" + text + "]");
			lineBuilder.append(text);
			lineBuilder.append('\n');
		}

		protected void emit() throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			write(nextLine); // throws SAXException
		}

		@SuppressWarnings("unused")
		protected void emitLineUTF8(String text) throws SAXException {
			String nextLine = lineBuilder.toString();
			lineBuilder.setLength(0);

			writeUTF8(nextLine); // throws SAXException
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
				debug("processingInstruction: data[" + data + "]");
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
			debug("startElement: uri[" + uri + "] localName[" + localName + "] qName[" + qName + "] attributes["
				+ attributes + "]");
			append('<' + localName);
			append(uri);

			if (attributes != null) {
				int numberAttributes = attributes.getLength();
				for (int i = 0; i < numberAttributes; i++) {
					append(' ');
					append(attributes.getQName(i));
					debug("startElement: attributes.getQName(" + i + ")[" + attributes.getQName(i) + "]");
					append("=\"");
					append(attributes.getValue(i));
					debug("startElement: attributes.getValue(" + i + ")[" + attributes.getValue(i) + "]");
					append('"');
				}
			}

			appendLine('>');

			emit();
		}

		@SuppressWarnings("unused")
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			debug("endElement: uri[" + uri + "] localName[" + localName + "] qName[" + qName + "]");
			append("</");
			append(localName + '>');
		}

		@SuppressWarnings("unused")
		@Override
		public void characters(char[] chars, int start, int length) throws SAXException {
			String initialText = new String(chars, start, length);
			debug("characters: initialText[" + initialText + "]");

			String finalText = XmlActionImpl.this.replaceText(inputName, initialText);
			if (finalText == null) {
				finalText = initialText;
				XmlActionImpl.this.addReplacement();
			}

			debug("characters:  finalText[" + finalText + "]");
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

	// protected void debug(String s) {
	// System.out.println(s);
	// }
}
