package transformer.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.eclipse.transformer.Transformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestVersion {
	public static void main(String[] args) {
		Date date = new Date();
		DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

		System.out.println( "BUILD_DATE=" + format.format(date) );
	}

	@Test
	public void verifyBuildProperties() throws Exception {
		System.out.println("Verifying build properties [ " + Transformer.TRANSFORMER_BUILD_PROPERTIES + " ]");

		Properties buildProperties = Transformer.loadProperties(Transformer.TRANSFORMER_BUILD_PROPERTIES);

		String copyright = (String) buildProperties.get(Transformer.COPYRIGHT_PROPERTY_NAME);
		String shortVersion = (String) buildProperties.get(Transformer.SHORT_VERSION_PROPERTY_NAME);
		String longVersion = (String) buildProperties.get(Transformer.LONG_VERSION_PROPERTY_NAME);
		String buildDate = (String) buildProperties.get(Transformer.BUILD_DATE_PROPERTY_NAME);

		System.out.println("  Copyright     [ " + copyright + " ] [ " + Transformer.COPYRIGHT_PROPERTY_NAME + " ]");
		System.out.println("  Short version [ " + shortVersion + " ] [ " + Transformer.SHORT_VERSION_PROPERTY_NAME + " ]");
		System.out.println("  Long version  [ " + longVersion + " ] [ " + Transformer.LONG_VERSION_PROPERTY_NAME + " ]");
		System.out.println("  Build date    [ " + buildDate + " ] [ " + Transformer.BUILD_DATE_PROPERTY_NAME + " ]");

		Assertions.assertNotNull(copyright, "Copyright is absent");
		Assertions.assertNotNull(shortVersion, "Short version is absent");
		Assertions.assertNotNull(longVersion, "Long version is absent");
		Assertions.assertNotNull(buildDate, "Build date is absent");
	}
}
