FROM mcr.microsoft.com/playwright/java:v1.48.0-jammy

WORKDIR /app

# Copy Maven project
COPY . /app

# Default environment (can be overridden with -e ENV=uat, etc.)
ENV ENV=test

# Run tests by default; ENV is expanded at runtime (e.g. -e ENV=uat).
CMD ["sh", "-c", "mvn clean test -P$ENV"]

