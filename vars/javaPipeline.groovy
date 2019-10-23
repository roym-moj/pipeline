#!/usr/bin/env groovy

def call(body) {
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def resourceGroupName = null
    def serviceVersion = null
    def seleniumBranchName = null
    pipeline {
        agent any
        environment {
            SERVICE_NAME = "$pipelineParams.SERVICE_NAME"
            SONARQUBE_PROJECT_NAME = "$pipelineParams.SONARQUBE_PROJECT_NAME"
        }
        options {
            ansiColor('xterm')
            timestamps()
            disableConcurrentBuilds()
        }
        parameters {
            booleanParam(name: 'RESERVE_ENVIRONMENT', defaultValue: false, description: 'Do you want to reserve the environment?')
        }
        post {
            always {
                step([$class: 'WsCleanup'])
            }
        }

        stages {
            stage('Compile') {
                steps {
                    sh './gradlew compileJava'
                }
            }

            
            stage('Checkstyle') {
                steps {
                    sh './gradlew checkstyleMain'
                }
            }

            stage('Unit Test') {
                steps {
                    sh './gradlew test'
                    }
                    post {
                        success {
                            junit 'build/test-results/test/TEST-hello.HelloControllerIT.xml'
                        }
                }
            }
            

            stage('Package') {
                steps {
                    sh './gradlew jar'
                    }
            }
            
            
//            stage('Create Enviroment -- beanstalk?') {
//                steps {
//                }
//            }
//
//            stage('System Tests') {
//                steps {
//                }
//            }
//
//            stage('Destroy Enviroment -- beanstalk?') {
//                steps {
//                }
//            }

            
        }
    }
}
