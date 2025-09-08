# AGENTS.md

## Build/Lint/Test Commands
- Build: ./gradlew build
- Lint: ./gradlew check
- Test: ./gradlew test
- Test single class: ./gradlew test --tests *.ClassName

## Code Style Guidelines
- Language: Java 11
- Indentation: 4 spaces
- Line wrapping: 120 characters
- Imports: Grouped and ordered alphabetically, no wildcards
- Naming conventions:
  - Variables/methods: camelCase
  - Classes: UpperCamelCase
  - Constants: UPPERCASE_WITH_UNDERSCORES

## Error Handling
- Use Java's built-in exception handling
- Always use try-with-resources for resource management
- Log exceptions using SLF4J

## Additional Rules
- Follow Java naming conventions strictly
- Use standard Java libraries when possible
- Keep imports organized and remove unused ones