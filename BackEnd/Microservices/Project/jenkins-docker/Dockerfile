# Jenkins LTS + client Docker (le démon = celui de Docker Desktop via /var/run/docker.sock)
FROM docker:27-cli AS dockercli

FROM jenkins/jenkins:lts-jdk17

USER root
COPY --from=dockercli /usr/local/bin/docker /usr/local/bin/docker
RUN chmod +x /usr/local/bin/docker

# Root: accès au socket Docker monté (/var/run/docker.sock). Avec USER jenkins,
# Docker renvoie souvent "permission denied" sauf si group_add du GID docker hôte.
# Pour la prod, préférer: usermod -aG <gid-docker-hôte> jenkins + USER jenkins.
USER root
