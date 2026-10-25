FROM openjdk:17-slim

# Install Python 3
RUN apt-get update && apt-get install -y --no-install-recommends python3 && rm -rf /var/lib/apt/lists/*

# Create non-root runner user with UID 10001
RUN groupadd -g 10001 runner && useradd -u 10001 -g runner -m runner

COPY runner.sh /usr/local/bin/runner.sh
RUN chmod +x /usr/local/bin/runner.sh

USER 10001:10001
WORKDIR /tmp

ENTRYPOINT ["/usr/local/bin/runner.sh"]
