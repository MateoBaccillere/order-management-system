pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvnw.cmd clean compile'
                }
            }
        }

        stage('Test Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvnw.cmd test'
                }
            }
        }

        stage('Package Order Service') {
            steps {
                dir('order-service/order-service') {
                    bat 'mvnw.cmd clean package -DskipTests'
                }
            }
        }
    }
}