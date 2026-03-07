# OpenCGMES
Suite of tools for CGMES / CIM (IEC 61970) RDF - RDFS, SHACL and CIMXML

## CimXml

A Java library for parsing CIMXML files into RDF graphs using Apache Jena. 
It supports both full models and difference models as defined in IEC 61970-552.
see [CimXmlParser](cimxml/README.md)

## Coming soon: QueryAndValidationUI

A web application for uploading RDF Schema, SHACL shapes, and CIM XML files,
querying the data using SPARQL, and validating the data against SHACL shapes.
see [QueryAndValidationUI](./QueryAndValidationUI/README.md)

## Third-Party Test Data

This project includes the [ENTSO-E Application Profiles Library](https://github.com/entsoe/application-profiles-library)
(v1.1.1) as a Git submodule under `cimxml/testing/application-profiles-library/` for testing purposes only.
The Application Profiles Library is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Commercial Support and Services

For organizations requiring commercial support, professional maintenance, integration services,
or custom extensions for this project, these services are available from **SOPTIM AG**.

Please feel free to contact us via [opencgmes@soptim.de](mailto:opencgmes@soptim.de).

## Contributing

We welcome contributions to improve this project.
Please see our [Contributing Guide](CONTRIBUTING.md) for details on how to submit pull requests, report issues, and suggest improvements.

## Code of Conduct

This project adheres to a code of conduct adapted from the [Apache Foundation's Code of Conduct](https://www.apache.org/foundation/policies/conduct).
We expect all contributors and users to follow these guidelines to ensure a welcoming and inclusive community.