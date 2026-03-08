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

package de.soptim.opencgmes.cimxml.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import de.soptim.opencgmes.cimxml.sparql.core.CimDatasetGraph;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests that verify CIMXML grid data files from the ENTSO-E ReliCapGrid can be parsed
 * using CIM RDFS profiles from the application-profiles-library.
 *
 * <p>The ReliCapGrid is included as a Git submodule at
 * {@code cimxml/testing/relicapgrid} and is licensed under CC-BY-SA-4.0.
 * See <a href="https://github.com/entsoe/relicapgrid">
 * https://github.com/entsoe/relicapgrid</a></p>
 *
 * <p>Each test case corresponds to one directory containing {@code .xml} CIMXML files.
 * A {@link CimXmlParser} instance is created per directory with the appropriate RDFS profiles
 * loaded, and all {@code .xml} files in that directory are parsed as CIM models.</p>
 */
@RunWith(Parameterized.class)
public class TestReliCapGrid {

  private static final Logger LOG = LoggerFactory.getLogger(TestReliCapGrid.class);

  private static final Path RELICAPGRID_ROOT =
      Paths.get("testing", "relicapgrid");

  private static final Path GRID_ROOT =
      RELICAPGRID_ROOT.resolve("Instance/Grid");

  private static final Path PROFILES_ROOT =
      Paths.get("testing", "application-profiles-library");

  private static final Path CGMES_30_RDFS =
      PROFILES_ROOT.resolve("CGMES/CurrentRelease/RDFS");

  private static final Path CGMES_24_RDFS =
      PROFILES_ROOT.resolve("CGMES/PastReleases/v2-4/Original/RDFS");

  private static final Path CGMES_24_GRID_DIR =
      GRID_ROOT.resolve("CommonAndBoundaryData/CGMES_2-4");

  /**
   * CGMES 2.4 "Augmented" equipment profiles to skip when loading RDFS, matching the exclusions
   * in {@code TestCrossVersionProfileCompatibility#cgmes24And30CoexistInSameRegistry}.
   */
  private static final List<String> CGMES_24_SKIPPED_RDFS = List.of(
      "EquipmentProfileCoreOperationRDFSAugmented-v2_4_15-4Sep2020.rdf",
      "EquipmentProfileCoreRDFSAugmented-v2_4_15-4Sep2020.rdf",
      "EquipmentProfileCoreShortCircuitRDFSAugmented-v2_4_15-4Sep2020.rdf"
  );

  private final String testName;
  private final Path gridDirectory;
  private final boolean isCgmes24;

  public TestReliCapGrid(String testName, Path gridDirectory, boolean isCgmes24) {
    this.testName = testName;
    this.gridDirectory = gridDirectory;
    this.isCgmes24 = isCgmes24;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> discoverGridDirectories() throws IOException {
    List<Object[]> testCases = new ArrayList<>();

    boolean relicapAvailable = Files.exists(RELICAPGRID_ROOT)
        && Files.isDirectory(RELICAPGRID_ROOT)
        && Files.exists(GRID_ROOT);
    boolean profilesAvailable = Files.exists(PROFILES_ROOT)
        && Files.isDirectory(PROFILES_ROOT);

    if (!relicapAvailable || !profilesAvailable) {
      testCases.add(new Object[]{
          "submodules not available", null, false
      });
      return testCases;
    }

    // CGMES 2.4 test case
    if (Files.exists(CGMES_24_GRID_DIR) && Files.exists(CGMES_24_RDFS)) {
      testCases.add(new Object[]{
          "CGMES_2-4/CommonAndBoundaryData", CGMES_24_GRID_DIR, true
      });
    }

    // CGMES 3.0 test cases: all directories under Instance/Grid except CGMES_2-4
    try (Stream<Path> paths = Files.walk(GRID_ROOT)) {
      List<Path> xmlDirs = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".xml"))
          .map(Path::getParent)
          .distinct()
          .filter(dir -> !dir.startsWith(CGMES_24_GRID_DIR))
          .sorted()
          .collect(Collectors.toList());

      for (Path dir : xmlDirs) {
        String displayName = GRID_ROOT.relativize(dir).toString();
        testCases.add(new Object[]{displayName, dir, false});
      }
    }

    if (testCases.isEmpty()) {
      testCases.add(new Object[]{"No grid data found", null, false});
    }

    return testCases;
  }

  private CimXmlParser createParserWithProfiles() throws IOException {
    CimXmlParser parser = new CimXmlParser();

    if (isCgmes24) {
      loadRdfsProfiles(parser, CGMES_24_RDFS, CGMES_24_SKIPPED_RDFS);
    } else {
      loadRdfsProfiles(parser, CGMES_30_RDFS, List.of());
    }

    return parser;
  }

  private static void loadRdfsProfiles(CimXmlParser parser, Path rdfsDir,
                                       List<String> skippedFiles) throws IOException {
    try (Stream<Path> paths = Files.list(rdfsDir)) {
      List<Path> rdfFiles = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .filter(p -> !skippedFiles.contains(p.getFileName().toString()))
          .sorted()
          .collect(Collectors.toList());

      for (Path rdfFile : rdfFiles) {
        String name = rdfFile.getFileName().toString();
        // Skip older RDFS2019 header for CGMES 3.0 (same as cross-version test)
        if (name.contains("RDFS2019")) {
          continue;
        }
        try {
          parser.parseAndRegisterCimProfile(rdfFile);
        } catch (IllegalArgumentException e) {
          if (e.getMessage() != null && e.getMessage().contains("already registered")) {
            LOG.info("Skipping duplicate profile {}: {}", name, e.getMessage());
          } else {
            throw e;
          }
        }
      }
    }
  }

  private static List<Path> listXmlFiles(Path directory) throws IOException {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".xml"))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  @Test
  public void parseAllGridFiles() throws IOException {
    assumeTrue("Submodules not available, skipping: " + testName,
        gridDirectory != null);

    CimXmlParser parser = createParserWithProfiles();
    List<Path> xmlFiles = listXmlFiles(gridDirectory);
    assertFalse("No .xml files found in " + testName, xmlFiles.isEmpty());

    for (Path xmlFile : xmlFiles) {
      LOG.info("Parsing grid file: {}", xmlFile.getFileName());
      CimDatasetGraph dataset = parser.parseCimModel(xmlFile);

      assertNotNull("Parsed dataset should not be null for " + xmlFile.getFileName(),
          dataset);
      assertTrue("Dataset should be a FullModel: " + xmlFile.getFileName(),
          dataset.isFullModel());
      assertNotNull("Model header should not be null: " + xmlFile.getFileName(),
          dataset.getModelHeader());
      assertTrue("Model body should not be empty: " + xmlFile.getFileName(),
          dataset.getBody().size() > 0);
    }
  }
}
