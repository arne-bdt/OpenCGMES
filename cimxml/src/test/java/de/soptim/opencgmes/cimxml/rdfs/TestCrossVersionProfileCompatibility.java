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
import static org.junit.Assume.assumeTrue;

import de.soptim.opencgmes.cimxml.graph.CimProfile;
import de.soptim.opencgmes.cimxml.parser.CimXmlParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests cross-version compatibility by loading CGMES 2.4 (CIM 16) and CGMES 3.0 (CIM 17/18)
 * profiles into the same CimProfileRegistry and verifying they coexist.
 *
 * <p>CGMES 2.4 profiles use {@code http://iec.ch/TC57/2013/CIM-schema-cim16#} as the CIM
 * namespace, while CGMES 3.0 profiles use {@code http://iec.ch/TC57/CIM100#}.</p>
 */
public class TestCrossVersionProfileCompatibility {

  private static final Path PROFILES_ROOT =
      Paths.get("testing", "application-profiles-library");

  private static final Path CGMES_30_RDFS =
      PROFILES_ROOT.resolve("CGMES/CurrentRelease/RDFS");

  private static final Path CGMES_24_RDFS =
      PROFILES_ROOT.resolve("CGMES/PastReleases/v2-4/Original/RDFS");

  private boolean submoduleAvailable;

  @Before
  public void checkSubmodule() {
    submoduleAvailable = Files.exists(PROFILES_ROOT) && Files.isDirectory(PROFILES_ROOT)
        && Files.exists(CGMES_30_RDFS) && Files.exists(CGMES_24_RDFS);
  }

  private static List<Path> listRdfFiles(Path directory) throws IOException {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".rdf"))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  @Test
  public void cgmes24And30CoexistInSameRegistry() throws IOException {
    assumeTrue("Submodule not available", submoduleAvailable);

    CimXmlParser parser = new CimXmlParser();

    // Load CGMES 3.0 profiles first (skip second header to avoid duplicate)
    for (Path rdfFile : listRdfFiles(CGMES_30_RDFS)) {
      if (rdfFile.getFileName().toString().contains("RDFS2019")) {
        continue; // Skip the older RDFS2019 header; RDFS2020 header will be loaded
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    int cgmes30Count = parser.getCimProfileRegistry().getRegisteredProfiles().size();
    assertTrue("Should have registered CGMES 3.0 profiles", cgmes30Count > 0);

    // Load CGMES 2.4 base profiles (skip augmented supersets to avoid duplicate IRIs)
    for (Path rdfFile : listRdfFiles(CGMES_24_RDFS)) {
      String name = rdfFile.getFileName().toString();
      if (name.contains("CoreOperation") || name.contains("CoreShortCircuit")) {
        continue; // Skip augmented profiles that overlap with base Equipment Core
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    Set<CimProfile> allProfiles = parser.getCimProfileRegistry().getRegisteredProfiles();
    int totalCount = allProfiles.size();
    assertTrue("Should have registered more profiles after adding CGMES 2.4",
        totalCount > cgmes30Count);

    // Verify both CIM namespaces are present
    Set<String> namespaces = new HashSet<>();
    for (CimProfile profile : allProfiles) {
      namespaces.add(profile.getCimNamespace());
    }
    assertTrue("Should contain CIM 16 namespace (CGMES 2.4)",
        namespaces.stream().anyMatch(ns -> ns.contains("cim16")));
    assertTrue("Should contain CIM 100 namespace (CGMES 3.0)",
        namespaces.stream().anyMatch(ns -> ns.contains("CIM100")));
  }

  @Test
  public void crossVersionProfilesHaveDistinctVersionIris() throws IOException {
    assumeTrue("Submodule not available", submoduleAvailable);

    CimXmlParser parser = new CimXmlParser();

    // Load CGMES 3.0 (skip duplicate header)
    for (Path rdfFile : listRdfFiles(CGMES_30_RDFS)) {
      if (rdfFile.getFileName().toString().contains("RDFS2019")) {
        continue;
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    // Load CGMES 2.4 base profiles
    for (Path rdfFile : listRdfFiles(CGMES_24_RDFS)) {
      String name = rdfFile.getFileName().toString();
      if (name.contains("CoreOperation") || name.contains("CoreShortCircuit")) {
        continue;
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    // Verify no version IRI overlap between CIM versions
    Set<String> cim16Iris = new HashSet<>();
    Set<String> cim100Iris = new HashSet<>();

    for (CimProfile profile : parser.getCimProfileRegistry().getRegisteredProfiles()) {
      if (profile.isHeaderProfile()) {
        continue;
      }
      String ns = profile.getCimNamespace();
      for (var iri : profile.getOwlVersionIris()) {
        if (ns.contains("cim16")) {
          cim16Iris.add(iri.getURI());
        } else {
          cim100Iris.add(iri.getURI());
        }
      }
    }

    assertFalse("CIM 16 should have version IRIs", cim16Iris.isEmpty());
    assertFalse("CIM 100 should have version IRIs", cim100Iris.isEmpty());

    // Version IRIs should be entirely disjoint between CIM versions
    Set<String> overlap = new HashSet<>(cim16Iris);
    overlap.retainAll(cim100Iris);
    assertTrue("Version IRIs should not overlap between CIM versions: " + overlap,
        overlap.isEmpty());
  }

  @Test
  public void crossVersionDatatypeResolution() throws IOException {
    assumeTrue("Submodule not available", submoduleAvailable);

    CimXmlParser parser = new CimXmlParser();

    // Load CGMES 3.0
    for (Path rdfFile : listRdfFiles(CGMES_30_RDFS)) {
      if (rdfFile.getFileName().toString().contains("RDFS2019")) {
        continue;
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    // Load CGMES 2.4 base profiles
    for (Path rdfFile : listRdfFiles(CGMES_24_RDFS)) {
      String name = rdfFile.getFileName().toString();
      if (name.contains("CoreOperation") || name.contains("CoreShortCircuit")) {
        continue;
      }
      parser.parseAndRegisterCimProfile(rdfFile);
    }

    CimProfileRegistry registry = parser.getCimProfileRegistry();

    // Verify property/datatype maps are resolvable for each non-header profile
    for (CimProfile profile : registry.getRegisteredProfiles()) {
      if (profile.isHeaderProfile()) {
        var headerProps = registry.getHeaderPropertiesAndDatatypes(
            profile.getCimNamespace());
        assertNotNull("Header props for " + profile.getCimNamespace(), headerProps);
        assertFalse("Header props should not be empty for " + profile.getCimNamespace(),
            headerProps.isEmpty());
      } else {
        var props = registry.getPropertiesAndDatatypes(profile.getOwlVersionIris());
        assertNotNull("Props for " + profile.getDcatKeyword(), props);
        assertFalse("Props should not be empty for " + profile.getDcatKeyword(),
            props.isEmpty());
      }
    }
  }
}
