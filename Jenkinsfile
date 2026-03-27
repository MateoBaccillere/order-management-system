pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'feature/compose-jenkins',
                    credentialsId: 'github-pat',
                    url: 'https://github.com/MateoBaccillere/order-management-system'
            }
        }

        stage('Build and Test Product Service') {
            steps {
                dir('product-service') {
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Package Product Service') {
            steps {
                dir('product-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }

        stage('Build and Test Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvn clean test'
                }
            }
        }

        stage('Package Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvn package -DskipTests'
                }
            }
        }
    }
}