pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test Product Service') {
            steps {
                dir('product-service') {
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Build and Test Product Service') {
            steps {
                dir('product-service') {
                    bat 'cd'
                    bat 'dir'
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Build and Test Order Service') {
            steps {
                dir('order-service') {
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Package Order Service') {
            steps {
                dir('order-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }
    }
}