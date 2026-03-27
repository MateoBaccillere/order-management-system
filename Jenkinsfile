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
                dir('product-service/product-service') {
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Package Product Service') {
            steps {
                dir('product-service/product-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }

        stage('Build and Test Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvnw.cmd clean test'
                }
            }
        }

        stage('Package Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }
    }
}