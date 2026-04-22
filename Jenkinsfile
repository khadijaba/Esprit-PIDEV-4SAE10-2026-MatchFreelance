pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        MODULE_DIR = 'backend/microservices/Formation'
        IMAGE_NAME = 'khadijabenayed/formation'
        IMAGE_TAG = "1.0.${BUILD_NUMBER}"
        SONAR_HOST = 'http://localhost:9000'
        SONAR_CREDENTIALS_ID = 'sonar-token'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir("${MODULE_DIR}") {
                    sh 'mvn clean verify'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir("${MODULE_DIR}") {
                    withCredentials([string(credentialsId: "${SONAR_CREDENTIALS_ID}", variable: 'SONAR_TOKEN')]) {
                        sh '''
                            mvn sonar:sonar \
                              -Dsonar.projectKey=formation \
                              -Dsonar.host.url=${SONAR_HOST} \
                              -Dsonar.login=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir("${MODULE_DIR}") {
                    sh '''
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    '''
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
