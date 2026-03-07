/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.soptim.opencgmes.cimxml.rdfs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import de.soptim.opencgmes.cimxml.parser.CimXmlParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests that verify all RDF Schema files from the ENTSO-E application-profiles-library can be
 * loaded and registered by the CimXmlParser.
 *
 * <p>The application-profiles-library is included as a Git submodule at
 * {@code cimxml/testing/application-profiles-library} and is licensed under Apache License 2.0.
 * See <a href="https://github.com/entsoe/application-profiles-library">
 * https://github.com/entsoe/application-profiles-library</a></p>
 *
 * <p>Each test case corresponds to one directory containing {@code .rdf} files. A fresh
 * {@link CimXmlParser} instance is created per directory, and all {@code .rdf} files in that
 * directory are loaded and registered as CIM profiles.</p>
 */
@RunWith(Parameterized.class)
public class TestApplicationProfilesLibrary {

  private static final Path PROFILES_ROOT =
      Paths.get("testing", "application-profiles-library");

  private static final List<String> EXCLUDED_PATHS = List.of(
      "NCP/PastReleases",
      "CGMES/CurrentRelease/RDFS/Beta_501_Ed2_CD"
  );

  private final String directoryName;
  private final Path directoryPath;

  public TestApplicationProfilesLibrary(String directoryName, Path directoryPath) {
    this.directoryName = directoryName;
    this.directoryPath = directoryPath;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> discoverProfileDirectories() throws IOException {
    List<Object[]> testCases = new ArrayList<>();

    if (!Files.exists(PROFILES_ROOT) || !Files.isDirectory(PROFILES_ROOT)) {
      testCases.add(new Object[]{
          "application-profiles-library submodule not available", null
      });
      return testCases;
    }

    try (Stream<Path> paths = Files.walk(PROFILES_ROOT)) {
      List<Path> rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .filter(p -> !isExcluded(p))
          .collect(Collectors.toList());

      rdfFiles.stream()
          .map(Path::getParent)
          .distinct()
          .sorted()
          .forEach(dir -> {
            String displayName = PROFILES_ROOT.relativize(dir).toString();
            testCases.add(new Object[]{displayName, dir});
          });
    }

    if (testCases.isEmpty()) {
      testCases.add(new Object[]{"No .rdf files found", null});
    }

    return testCases;
  }

  private static boolean isExcluded(Path path) {
    String normalized = PROFILES_ROOT.relativize(path).toString().replace('\\', '/');
    return EXCLUDED_PATHS.stream().anyMatch(normalized::startsWith);
  }

  @Test
  public void loadAllRdfSchemaFilesInDirectory() throws IOException {
    assumeTrue("Submodule not available, skipping test: " + directoryName,
        directoryPath != null);

    CimXmlParser parser = new CimXmlParser();
    List<Path> rdfFiles;
    try (Stream<Path> paths = Files.list(directoryPath)) {
      rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .sorted()
          .collect(Collectors.toList());
    }

    assertFalse("No .rdf files found in " + directoryName, rdfFiles.isEmpty());

    for (Path rdfFile : rdfFiles) {
      try {
        parser.parseAndRegisterCimProfile(rdfFile);
      } catch (Exception e) {
        fail("Failed to load " + rdfFile.getFileName() + " in " + directoryName
            + ": " + e.getMessage());
      }
    }

    assertFalse("No profiles were registered from " + directoryName,
        parser.getCimProfileRegistry().getRegisteredProfiles().isEmpty());
  }

  @Test
  public void allProfilesHaveNonEmptyGraph() throws IOException {
    assumeTrue("Submodule not available, skipping test: " + directoryName,
        directoryPath != null);

    CimXmlParser parser = new CimXmlParser();
    List<Path> rdfFiles;
    try (Stream<Path> paths = Files.list(directoryPath)) {
      rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .sorted()
          .collect(Collectors.toList());
    }

    for (Path rdfFile : rdfFiles) {
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    for (var profile : parser.getCimProfileRegistry().getRegisteredProfiles()) {
      assertTrue("Profile graph should not be empty: " + profile.getDcatKeyword(),
          profile.size() > 0);
      assertNotNull("Profile should have a CIM namespace",
          profile.getCimNamespace());
    }
  }

  @Test
  public void allNonHeaderProfilesHaveVersionIris() throws IOException {
    assumeTrue("Submodule not available, skipping test: " + directoryName,
        directoryPath != null);

    CimXmlParser parser = new CimXmlParser();
    List<Path> rdfFiles;
    try (Stream<Path> paths = Files.list(directoryPath)) {
      rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .sorted()
          .collect(Collectors.toList());
    }

    for (Path rdfFile : rdfFiles) {
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    for (var profile : parser.getCimProfileRegistry().getRegisteredProfiles()) {
      if (!profile.isHeaderProfile()) {
        assertNotNull("Non-header profile should have version IRIs: "
            + profile.getDcatKeyword(), profile.getOwlVersionIris());
        assertFalse("Non-header profile should have at least one version IRI: "
            + profile.getDcatKeyword(), profile.getOwlVersionIris().isEmpty());
      }
    }
  }

  @Test
  public void allNonHeaderProfilesHaveKeyword() throws IOException {
    assumeTrue("Submodule not available, skipping test: " + directoryName,
        directoryPath != null);

    CimXmlParser parser = new CimXmlParser();
    List<Path> rdfFiles;
    try (Stream<Path> paths = Files.list(directoryPath)) {
      rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .sorted()
          .collect(Collectors.toList());
    }

    for (Path rdfFile : rdfFiles) {
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    for (var profile : parser.getCimProfileRegistry().getRegisteredProfiles()) {
      if (!profile.isHeaderProfile()) {
        assertNotNull("Non-header profile should have a dcat:keyword: "
                + profile.getOwlVersionIris(),
            profile.getDcatKeyword());
        assertFalse("Non-header profile keyword should not be empty: "
                + profile.getOwlVersionIris(),
            profile.getDcatKeyword().isEmpty());
      }
    }
  }
}
