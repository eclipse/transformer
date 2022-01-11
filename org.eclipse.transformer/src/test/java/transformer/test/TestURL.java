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

package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.eclipse.transformer.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.lib.io.ByteBufferInputStream;

public class TestURL {
	private Properties prior;

	@BeforeEach
	public void setUp() {
		prior = new Properties();
		prior.putAll(System.getProperties());
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(prior);
	}

	public static String classToResource(String className) {
		return className.replace('.', '/') + ".class";
	}

	public static int firstDifference(byte[] data1, byte[] data2, int length) {
		for (int offset = 0; offset < length; offset++) {
			if (data1[offset] != data2[offset]) {
				return offset;
			}
		}
		return -1;
	}

	public static class TransformURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformURLConnection(baseURL); // throws IOException
		}
	}

	public static class TransformURLConnection extends URLConnection {
		public TransformURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL()
				.openConnection(); // 'openConnection' throws IOException
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect(); // 'connect' throws IOException
		}

		@Override
		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL()
				.toString();
			InputStream baseStream = useBaseConnection.getInputStream(); // throws
																			// IOException

			ByteBuffer inputData = FileUtils.read(baseName, baseStream);

			return new ByteBufferInputStream(inputData);
		}
	}

	@Test
	public void testClassLoaderAPI() {
		String urlTestClassName = TestURL.class.getName();

		ClassLoader urlTestLoader = TestURL.class.getClassLoader();

		String urlTestResourceName = classToResource(urlTestClassName);
		URL urlTestURL = urlTestLoader.getResource(urlTestResourceName);
		String urlTestURLName = urlTestURL.toString();

		System.out.println("Class [ " + urlTestClassName + " ] as resource [ " + urlTestResourceName + " ]");
		System.out.println("In [ " + urlTestLoader + " ] as URL [ " + urlTestURLName + " ]");

		URL xformURL;
		try {
			xformURL = new URL(null, urlTestURLName, new TransformURLStreamHandler(urlTestURL));
		} catch (MalformedURLException e) {
			Assertions.fail("Failed to wrap test URL: " + e);
			return;
		}

		System.out.println("As xForm URL [ " + xformURL + " ]");

		ByteBuffer directData;
		try {
			directData = FileUtils.read(urlTestURLName, urlTestURL.openStream());
		} catch (IOException e) {
			Assertions.fail("Failed to read direct URL [ " + urlTestURLName + " ]: " + e);
			return;
		}

		ByteBuffer indirectData;
		try {
			indirectData = FileUtils.read(urlTestURLName, xformURL.openStream());
		} catch (IOException e) {
			Assertions.fail("Failed to read indirect URL [ " + urlTestURLName + " ]: " + e);
			return;
		}

		if ((directData != null) && (indirectData != null)) {
			if (directData.limit() != indirectData.limit()) {
				Assertions.fail("Length change [ " + urlTestURLName + " ] Direct [ " + directData.limit()
					+ " ] Indirect [ " + indirectData.limit() + " ]");
			} else {
				int firstChange = firstDifference(directData.array(), indirectData.array(), directData.limit());
				if (firstChange != -1) {
					Assertions.fail("Data change [ " + urlTestURLName + " ] at [ " + firstChange + " ]");
				} else {
					System.out.println(
						"Direct matches indirect [ " + urlTestURLName + " ] to length [ " + directData.limit() + " ]");
				}
			}
		}
	}
}
