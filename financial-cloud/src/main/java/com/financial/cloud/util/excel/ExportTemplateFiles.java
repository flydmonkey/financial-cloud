package com.financial.cloud.util.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.ClassPathResource;

/**
 * Copy classpath Excel templates to a real temp file.
 * <p>
 * {@code ResourceUtils.getURL(...).getPath()} is not portable inside Spring Boot jars
 * (and the old leading-{@code /} strip breaks absolute Linux paths).
 */
public final class ExportTemplateFiles {

	private ExportTemplateFiles() {
	}

	public static File copyToTemp(String classpathLocation, String prefix) throws IOException {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		if (!resource.exists()) {
			throw new IOException("Missing classpath template: " + classpathLocation);
		}
		File tempFile = Files.createTempFile(prefix, ".xlsx").toFile();
		try (InputStream in = resource.getInputStream()) {
			Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		tempFile.deleteOnExit();
		return tempFile;
	}
}
