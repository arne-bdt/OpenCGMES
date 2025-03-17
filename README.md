# Project moved to https://github.com/SOPTIM/OpenCGMES


# OpenCGMES (Draft)&#x20;

OpenCGMES is an open-source Java toolkit designed for working with Common Grid Model Exchange Standard (CGMES) files, specifically those based on the IEC 61970 CIM RDF format. It uses Apache Jena under the hood to handle RDF data. This project is currently in draft form and may change over time. CGMES is a widely used standard in the energy industry to facilitate the exchange of grid model information, enabling better interoperability between different systems and tools. OpenCGMES aims to simplify and enhance the process of working with these complex file formats, empowering researchers, engineers, and developers in the power systems community.

## Motivation

The energy sector is undergoing a significant transformation driven by the need for cleaner, more efficient power systems. As part of this transition, the European Network of Transmission System Operators for Electricity (ENTSO-E) and other stakeholders are adopting the Common Grid Model Exchange Standard (CGMES) to ensure seamless data exchange and interoperability between different systems. However, working with CGMES files can be challenging because CGMES is not fully RDF/XML compliant, which makes it difficult to use existing RDF tools effectively. This incompatibility often leads to issues when parsing, validating, or processing CGMES data.

OpenCGMES was created to address these challenges by providing an open-source, user-friendly toolkit that makes it easier to work with CGMES data. By simplifying data handling, validation, and transformation, OpenCGMES aims to support grid operators, researchers, and developers in their efforts to model and analyze power systems more effectively, ultimately contributing to a more resilient and sustainable energy future.

## Features

- **SPARQL Queries**: Support executing SPARQL queries to interact with and extract insights from CGMES data, enhancing analytical capabilities.
- **SHACL Validation**: Validate CGMES files by supporting SHACL files published by ENTSO-E, ensuring data integrity and interoperability without having to implement the entire IEC standard. This helps users identify and resolve inconsistencies in their models more effectively.
- **Comprehensive CGMES File Handling**: Easily parse and load CGMES (IEC 61970) RDF files, making it more accessible to work with power grid data models.
- **Support for CGMES Difference Models**: Read-only support for CGMES difference models, allowing users to work with incremental changes in grid models effectively.
- **Data Type Derivation**: Due to missing data types in CIM/XML compared to RDF/XML, the data types are derived from corresponding RDF schemas to ensure compatibility and consistency.
- **Model Conversion & Transformation**: Support converting CIM/XML into RDF/XML or JSON-LD, making integration with other tools easier.
- **Extensibility**: Built with flexibility in mind, the toolkit is easily extendable, allowing users to add custom functionalities or adapt it to specific use cases.
- **Java API & Optional APIs**: Provides a Java API for programmatic access, along with optional REST and gRPC APIs, giving users flexibility in how they interact with the toolkit.
- **CAS/NC Specifications Support**: Includes support for the Common Grid Model Exchange Standard (CGMES) library, including the Capacity Allocation & Congestion Management (CAS/NC) specifications as defined by ENTSO-E. This ensures compliance with the latest industry standards and compatibility with ENTSO-E guidelines.

## Design Goals

- **Speed**: The tools are designed for speed, addressing the inefficiencies found in many existing RDF tools, which are often too slow for time-critical processes where grid data is required.
- **In-Memory Processing**: Currently, there is no persistence layer like a triple store or database. To maximize performance, all data is kept in memory, which can consume significant RAM. However, similar in-memory approaches have been successful in handling CGMES data for 35 TSOs for 24-hour periods.

## Why OpenCGMES?

- **Interoperability**: Facilitates seamless exchange of information between different grid analysis tools.
- **Community Driven**: OpenCGMES is open-source and driven by the community, making it continuously adaptable to the evolving needs of the energy industry.
- **Ease of Use**: Designed to simplify working with CGMES files, allowing users to focus on analysis rather than the intricacies of file handling.

## Future Ideas

- **Data Manipulation via SPARQL Updates**: Add support for data manipulation through SPARQL Updates, enabling modifications of in-memory data efficiently.

- **gRPC Client Implementations**: Develop gRPC client implementations (at least for SHACL validation and SPARQL queries) for different programming environments to enhance cross-platform compatibility:

  - **.NET**: Support integration with dotNetRDF.
  - **Rust**: Support integration with Oxigraph.
  - **Python**: Support integration with rdflib.
  - **Julia**: Support integration with ???.

## Current Hurdles

- **Conformity Assessment Test Files Licensing**: The conformity assessment test files for CGMES are not available under a license that permits their use in unit tests. This limitation makes it challenging to ensure comprehensive test coverage and validation for the toolkit.

* **Missing Sample Data for CAS/NC**: There is currently a lack of publicly available sample data for the CAS/NC specifications, making it difficult to validate implementations and perform thorough testing.

- **Missing Examples for JSON-LD Format**: There are currently only few available examples for the upcoming JSON-LD format, making it difficult to test and validate JSON-LD conversion. (the current progress of the standardization of JSON-LD for CIM/CGMES is documented here: https://github.com/Sveino/Inst4CIM-KG/tree/develop/rdf-improved#json-ld-serialization)



Contributions are welcome! If you find a bug, have a feature request, or want to improve the existing code, please feel free to open an issue or submit a pull request.

## License

OpenCGMES is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for more details.
