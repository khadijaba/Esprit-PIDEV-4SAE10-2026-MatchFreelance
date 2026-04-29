# Jenkins LTS + client Docker (le démon = celui de Docker Desktop via /var/run/docker.sock)
FROM docker:27-cli AS dockercli

FROM jenkins/jenkins:lts-jdk17

USER root
COPY --from=dockercli /usr/local/bin/docker /usr/local/bin/docker
RUN chmod +x /usr/local/bin/docker

USER jenkins
