# CBOMkit - the essentials for CBOMs

[![License](https://img.shields.io/github/license/PQCA/cbomkit.svg)](https://opensource.org/licenses/Apache-2.0) <!--- long-description-skip-begin -->
[![Current Release](https://img.shields.io/github/release/PQCA/cbomkit.svg)](https://github.com/PQCA/cbomkit/releases)

CBOMkit is a toolset for dealing with Cryptography Bill of Materials (CBOM). CBOMkit includes a
- **CBOM Generation** ([CBOMkit-hyperion](https://github.com/PQCA/sonar-cryptography), [CBOMkit-theia](https://github.com/PQCA/cbomkit-theia)): Generate CBOMs from source code by scanning private and public git repositories to find the used cryptography.
- **CBOM Viewer ([CBOMkit-coeus](https://github.com/PQCA/cbomkit?tab=readme-ov-file#cbomkit-coeus))**: Visualize a generated or uploaded CBOM and access comprehensive statistics.
- **CBOM Compliance Check**: Evaluate CBOMs created or uploaded against specified compliance policies and receive detailed compliance status reports.
- **CBOM Database**: Collect and store CBOMs into the database and expose this data through a RESTful API.

![CBOMkit Demo](.github/img/cbomkit.gif)

> [!WARNING]
> The CBOMkit service does not build any repository prior to scanning. For Java repositories in particular, this means that we cannot rely on any build results (class files, jars) that could improve the scanning result. This potentially reduces completeness and accuracy of the findings since some Java symbols may not be resolved. For better results, use the [sonar-cryptography-plugin](https://github.com/PQCA/sonar-cryptography) together with SonarQube or [CBOMkit-action](https://github.com/PQCA/cbomkit-action) embedded in a pipeline definition that builds the code before scanning.

## Quickstart

Starting the CBOMkit using `docker-compose`.
```shell
# clone the repository 
git clone https://github.com/PQCA/cbomkit
# run the make command to start the docker compose 
make production
```

Alternatively, if you wish to use podman instead of docker, run the following:
```shell
# run the make command to start the docker compose using podman
make production ENGINE=podman
```

(This requires podman-compose to have been installed via `pip3 install podman-compose`).

Next steps:
- Enter a git url like [https://github.com/keycloak/keycloak](https://github.com/keycloak/keycloak) or a package url (PURL) like `pkg:maven/io.quarkus/quarkus-core@3.18.1` to generate a CBOM
- View your generated CBOM by selecting your previously scanned CBOM
- Drag and drop CBOM from the [examples](example) into the dropbox to view it

> [!NOTE]
> By default, the service can be accessed at http://localhost:8001

Deploy using the helm chart to a kubernetes environment. Pass the domain suffix and the cbomkit database creds via helm parameters.
```shell
# clone the repository 
git clone https://github.com/PQCA/cbomkit
# deploy using helm
helm install cbomkit \
  --set common.clusterDomain={CLUSTER_DOMAIN} \
  --set postgresql.auth.username={POSTGRES_USER} \
  --set postgresql.auth.password={POSTGRES_PASSWORD} \
  --set backend.tag=$(curl -s https://api.github.com/repos/PQCA/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') \
  --set frontend.tag=$(curl -s https://api.github.com/repos/PQCA/cbomkit/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/') \
  ./chart
```

## Architecture

The CBOMkit consists of three integral components: a web frontend, an API server, and a database.

### Frontend and CBOMkit-coeus

The web frontend serves as an intuitive user interface for interacting with the API server. It offers a range of functionalities, including:
 - Browsing the inventory of existing Cryptographic Bills of Materials (CBOMs)
 - Initiating new scans to generate CBOMs 
 - Uploading existing CBOMs for visualization and analysis

#### CBOMkit-coeus

For enhanced flexibility, the frontend component can be deployed as a standalone version, known as the CBOMkit-coeus. 
This option allows for streamlined visualization and compliance analysis independent of the full CBOMkit suite.

```shell
# use this command if you want to run only the CBOMkit-coeus
make coeus
```

### API Server

The API server functions as the central component of the CBOMkit, offering a comprehensive RESTful API 
(see [OpenAPI specification](openapi.yaml)) with the following key features:

#### Features
- Retrieve the most recent generated CBOMs
- Access stored CBOMs from the database
- Perform compliance checks for user-provided CBOMs against specified policies 
- Conduct compliance assessments for stored or generated CBOMs against defined policies

*Sample Query to Retrieve CBOM project identifier*
```shell
curl --request GET \
  --url 'http://localhost:8081/api/v1/cbom/github.com%2Fkeycloak%2Fkeycloak'
```

In addition to the RESTful API, the server incorporates WebSocket integration, enabling:
 - Initiation of CBOM generation through Git repository scanning 
 - Real-time progress updates during the scanning process, transmitted via WebSocket connection

### Compliance

A critical component of the CBOMkit is its compliance checking mechanism for Cryptography Bills of Materials (CBOMs). 
The CBOM structure represents a hierarchical tree of cryptographic assets detected and used by an application. 
This standardized format facilitates the development and implementation of generalized policies 
to identify and flag violations in cryptographic usage.

The CBOMkit currently features a foundational `quantum-safe` compliance check. 
This initial implementation serves as a proof of concept and demonstrates the system's capability to evaluate
cryptographic components against defined policies.

The compliance framework is designed with extensibility in mind, providing a solid platform for:
 - Implementing additional compliance checks 
 - Enhancing existing verification processes 
 - Integrating custom compliance checks (external)

#### Configuration

Different deployment configurations utilize distinct sources for compliance verification.

| Deployment       | How is the compliance check performed?                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `coeus`          | A `quantum-safe` algorithm compliance check is natively implemented within the frontend. This integration allows for immediate, client-side assessment of basic quantum resistance criteria.                                                                                                                                                                                                                                         |
| `production`     | In the standard deployment, a core compliance service is integrated into the backend service. This implementation enables the execution of compliance checks via the RESTful API, providing a scalable and centralized approach to cryptographic policy verification.                                                                                                                                                                |
| `ext-compliance` | In advanced deployment scenarios, compliance evaluation is delegated to a dedicated external service. This service can invoked by the API server as needed. This configuration maintains the standard user experience for both the frontend and API of the CBOMkit, mirroring the functionality of the `production` configuration while allowing for more sophisticated or specialized compliance checks to be performed externally. |

### Handling of Credentials

When a new scan of a GitHub repository is started, CBOMkit generates a temporary local clone
of the repository. The frontend enables users to provide GitHub credentials 
(either a username and password or a personal access token). These credentials are not
logged or stored; instead, they are directly forwarded 
to [JGit](https://github.com/eclipse-jgit/jgit) to facilitate the cloning process. 
After the scan completes - regardless of whether it succeeds or fails - the temporary 
local clone is deleted.

### Scanning and CBOM Generation

The CBOMkit leverages advanced scanning technology to identify cryptographic usage within source code and generate 
Cryptography Bills of Materials (CBOMs). This scanning capability is provided by the 
[CBOMkit-hyperion (Sonar Cryptography Plugin)](https://github.com/PQCA/sonar-cryptography), an open-source tool developed by IBM.

#### Supported languages and libraries

The current scanning capabilities of the CBOMkit are defined by the Sonar Cryptography Plugin's supported languages 
and cryptographic libraries:

| Language | Cryptographic Library                                                                         | Coverage | 
|----------|-----------------------------------------------------------------------------------------------|----------|
| Java     | [JCA](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html) | 100%     |
|          | [BouncyCastle](https://github.com/bcgit/bc-java) (*light-weight API*)                         | 100%[^1] |
| Python   | [pyca/cryptography](https://cryptography.io/en/latest/)                                       | 100%     |

[^1]: We only cover the BouncyCastle *light-weight API* according to [this specification](https://javadoc.io/static/org.bouncycastle/bctls-jdk14/1.80/specifications.html)

While the CBOMkit's scanning capabilities are currently bound to the Sonar Cryptography Plugin, the modular 
design of this plugin allows for potential expansion to support additional languages and cryptographic libraries in 
future updates.

## Contribution Guidelines

If you'd like to contribute to CBOMkit, please take a look at our
[contribution guidelines](CONTRIBUTING.md). By participating, you are expected to uphold our [code of conduct](CODE_OF_CONDUCT.md).

We use [GitHub issues](https://github.com/PQCA/cbomkit/issues) for tracking requests and bugs. For questions
start a discussion using [GitHub Discussions](https://github.com/PQCA/cbomkit/discussions).

## License

[Apache License 2.0](LICENSE.txt)
