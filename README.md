# Keycloak Federated Identities Mapper

A custom Keycloak protocol mapper extension that adds a user's federated identities to OpenID Connect tokens (ID token, access token, and UserInfo) as a JSON array. This mapper allows filtering of identity providers and is compatible with Keycloak 26.2.5.

## Features
- Maps all federated identities of a user to a configurable claim (default: `federated_identities`).
- Supports filtering by specific identity provider aliases via a configurable list.
- Includes options to add the claim to ID token, access token, and UserInfo.
- Configurable as a multi-valued claim (JSON array) or single object.

## Installation

1. **Build the Project**:
   - Ensure you have Maven 3.9+ and Java 17 installed.
   - Clone this repository:
     ```bash
     git clone https://github.com/LovingFox/keycloak-federated-identities-mapper.git
     cd keycloak-federated-identities-mapper
     ```
   - Build the JAR file:
     ```bash
     mvn clean package -U -T 4 -DskipTests
     ```
   - This generates `target/keycloak-federated-identities-mapper-0.1.0.jar`.

2. **Deploy to Keycloak**:
   - Copy the JAR to your Keycloak instance's providers directory:
     ```bash
     docker cp target/keycloak-federated-identities-mapper-0.1.0.jar <keycloak-container>:/opt/keycloak/providers/
     ```
   - Build and restart Keycloak:
     ```bash
     docker exec <keycloak-container> /opt/keycloak/bin/kc.sh build
     docker restart <keycloak-container>
     ```

3. **Configure the Mapper**:
   - Log in to the Keycloak Admin Console.
   - Navigate to **Client Scopes** > `groups-scope-keycloak` (or create a new scope).
   - Click **Mappers** > **Create**.
   - Select **Federated Identities Mapper** from the dropdown.
   - Configure the following:
     - **Name**: `Federated Identities`
     - **Token Claim Name**: `federated_identities` (or your preferred name)
     - **Add to ID token**: Check if needed
     - **Add to access token**: Check if needed
     - **Add to UserInfo**: Check if needed
     - **Multi-valued**: Check to map as a JSON array (default: true)
     - **Identity Providers to Map**: Leave empty to map all, or enter a comma-separated list (e.g., `vkid,yandex`) to filter
   - Save the configuration.

## Usage
- After configuration, authenticate a user with federated identities linked (e.g., via vkid, yandex, AuthDevTestAppBot, google).
- Decode the token (e.g., using jwt.io) to verify the `federated_identities` claim contains the expected data, such as:
  ```json
  "federated_identities": [
    {
      "identityProvider": "vkid",
      "userId": "368081472",
      "userName": "vk.368081472"
    },
    {
      "identityProvider": "yandex",
      "userId": "1130447525600000",
      "userName": "user@yandex.ru"
    },
    {
      "identityProvider": "AuthDevTestAppBot",
      "userId": "186184620",
      "userName": "user123"
    },
    {
      "identityProvider": "google",
      "userId": "104038758650866318337",
      "userName": "user@gmail.com"
    }
  ]
  ```
- Check Keycloak logs (`/opt/keycloak/data/log/*`) for debug output if needed.

## Requirements
- Keycloak 26.2.5
- Java 17
- Maven 3.9+

## License
This project is licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

## Contributing
Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request. Ensure you follow the Apache-2.0 license terms.

## Acknowledgments
- Built with assistance from the xAI Grok community.
- Inspired by the Keycloak custom protocol mapper example by mschwartau (https://github.com/mschwartau/keycloak-custom-protocol-mapper-example).

## Contact
For issues or questions, open an issue on this repository or contact the maintainers via GitHub.
