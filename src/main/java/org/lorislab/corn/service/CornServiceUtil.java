/*
 * Copyright 2018 lorislab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lorislab.corn.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The corn service utility class.
 *
 * @author andrej
 */
public class CornServiceUtil {

    /**
     * The logger.
     */
    private final static Logger LOG = Logger.getLogger(CornServiceUtil.class.getName());

    /**
     * Creates the ZIP data.
     *
     * @param dirName the directory.
     * @return the corresponding data.
     */
    public static byte[] createZipData(String dirName) {
        try {
            Path path = createZip(dirName);
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Creates the ZIP data.
     *
     * @param dirName the directory.
     * @return the corresponding path.
     */
    public static Path createZip(String dirName) {
        Path directory = Paths.get(dirName);

        Path result = null;

        try {
            result = Files.createTempFile("corn", ".zip");
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(result.toFile()))) {
            DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory);
            dirStream.forEach(path -> addToZipFile(path, zipStream));
            LOG.log(Level.FINE, "Zip file created in {0}", directory.toFile().getPath());
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "Error while zipping.", e);
        }

        return result;
    }

    /**
     * Adds the file to the zip package.
     *
     * @param file the file.
     * @param zipStream the zip stream.
     */
    private static void addToZipFile(Path file, ZipOutputStream zipStream) {
        String inputFileName = file.toFile().getPath();
        try (FileInputStream inputStream = new FileInputStream(inputFileName)) {

            ZipEntry entry = new ZipEntry(file.toFile().getName());
            entry.setCreationTime(FileTime.fromMillis(file.toFile().lastModified()));
            entry.setComment("Created by TheCodersCorner");
            zipStream.putNextEntry(entry);

            LOG.log(Level.FINE, "Generated new entry for: {0}", inputFileName);

            byte[] readBuffer = new byte[2048];
            int amountRead;
            int written = 0;
            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
                written += amountRead;
            }
            LOG.log(Level.FINE, "Stored {0} bytes to {1}", new Object[]{written, inputFileName});

        } catch (IOException e) {
            throw new RuntimeException("Unable to process " + inputFileName, e);
        }
    }

    /**
     * Deletes the non empty directory.
     *
     * @param directory the directory.
     * @throws Exception if the method fails.
     */
    public static void deleteDirectory(Path directory) throws Exception {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }
}
