FROM grafana/alloy:latest
USER root
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*
COPY config.alloy /etc/alloy/config.alloy
