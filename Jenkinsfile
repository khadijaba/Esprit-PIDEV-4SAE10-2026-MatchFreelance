pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
        git 'Default'
    }

    options {
        skipDefaultCheckout(true)
        timeout(time: 90, unit: 'MINUTES')
    }

    environment {
        MODULE_DIR = 'BackEnd/Microservices/Formation'
        SONAR_PROJECT_KEY = 'backend'
        SONAR_HOST = 'http://host.docker.internal:9000'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: 'master'
                    def exts = []
                    if (scm.extensions != null && !scm.extensions.isEmpty()) {
                        exts.addAll(scm.extensions)
                    }
                    exts.add([
                        $class       : 'CloneOption',
                        shallow      : true,
                        depth        : 1,
                        timeout      : 120,
                        honorRefspec : true,
                        noTags       : true,
                        reference    : ''
                    ])
                    def remotes = scm.userRemoteConfigs.collect { cfg ->
                        def m = [url: cfg.url]
                        if (cfg.credentialsId != null && cfg.credentialsId.toString().trim()) {
                            m.credentialsId = cfg.credentialsId
                        }
                        m.refspec = "+refs/heads/${branch}:refs/remotes/origin/${branch}"
                        m
                    }
                    checkout([
                        $class            : 'GitSCM',
                        branches          : [[name: "*/${branch}"]],
                        extensions        : exts,
                        userRemoteConfigs : remotes
                    ])
                }
            }
        }

        stage('Build & Sonar') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token-backend', variable: 'SONAR_TOKEN')]) {
                    dir("${env.MODULE_DIR}") {
                        sh """
                            mvn -B clean verify sonar:sonar \\
                              -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} \\
                              -Dsonar.host.url=${env.SONAR_HOST} \\
                              -Dsonar.token=\${SONAR_TOKEN}
                        """
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: "${env.MODULE_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
    }
}
