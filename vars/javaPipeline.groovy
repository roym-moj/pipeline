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
            AWS_ACCOUNT_ID = credentials('accountid')
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

            
//            stage('Checkstyle') {
//                steps {
//                    sh './gradlew checkstyleMain'
//                }
//            }

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
                    sh './gradlew bootJar'
                    sh "docker build -f Dockerfile -t ${env.JOB_NAME} ."
                }
            }
            
            stage('Publish') {
                steps {
                    //  THIS SHOULD ALL BE IN THE CODE AS A LOGIN METHOD
                    sh 'loginvar=$(aws ecr get-login --no-include-email --region us-east-2) && eval "$loginvar"'
                    sh "docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-east-2.amazonaws.com/${env.JOB_NAME}"
                }
            }
            
//            stage('Create Enviroment -- beanstalk?') {
//                steps {
//                    DockerName.getNameAndTag(branchName, serviceName, subscription, version, isMasterRelease, this)

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
