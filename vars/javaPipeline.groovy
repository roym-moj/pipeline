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

            stage('Checkstyle') {
                steps {
                    //OPTIONAL - FAILURE
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        sh './gradlew checkstyleMain'
                    }
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
                    sh './gradlew bootJar'
                    sh "docker build -f Dockerfile -t ${env.JOB_NAME} ."
                    sh "docker tag ${env.JOB_NAME}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
                }
            }
            
            stage('Publish') {
                steps {
                    
                    // AwsActions.logIn()
                    
                    sh 'loginvar=$(aws ecr get-login --no-include-email --region us-west-2) && eval "$loginvar"'
                    sh "aws ecr batch-delete-image --repository-name demo --image-ids imageTag=${env.JOB_NAME} --region us-west-2"
                    
                    sh "docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
                }
            }
            
            stage('Create/Update Enviroment') {
                steps {
                    
                      //EnviromentActions.createEmptyEnviroment(env.JOB_NAME, this)
                    //OR                    
                      //EnviromentActions.createEmptyEnviroment(env.JOB_NAME, this)
                    
                    sh "/usr/local/bin/ecs-cli configure --region us-west-2 --cluster ${env.JOB_NAME} --default-launch-type FARGATE --config-name ${env.JOB_NAME}"
                    sh "/usr/local/bin/ecs-cli up --capability-iam --size 2 --instance-type t3.micro --launch-type EC2 --cluster-config ${env.JOB_NAME} --force --region us-west-2"
                    sleep(60)
                }
            }
            
            stage('Deploy/Update services') {
                steps {
                    
                    //Should use a manifest and below methods
                    //EnviromentActions.populateEnviroment(env.JOB_NAME, this)
                    //EnviromentActions.deploySpecificService(env.JOB_NAME, this)
                    
                    sh 'aws ecs register-task-definition --network-mode host --family demoapp1 --region us-west-2 --container-definitions "[{\\"name\\":\\"demoapp1\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp1\\",\\"cpu\\":120,\\"memory\\":120,\\"essential\\":true}]"'
                    sh "aws ecs run-task --cluster ${env.JOB_NAME} --task-definition demoapp1 --count 1  --region us-west-2"
                    sh 'aws ecs register-task-definition --network-mode host --family demoapp2 --region us-west-2 --container-definitions "[{\\"name\\":\\"demoapp2\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp1\\",\\"cpu\\":120,\\"memory\\":120,\\"essential\\":true}]"'
                    sh "aws ecs run-task --cluster ${env.JOB_NAME} --task-definition demoapp2 --count 1  --region us-west-2"
                }
            }        
            
            stage('Run Global Tests for probation') {
                steps {
                    sleep(30)
                    echo "Running Tests"
                }
            }    
            
            stage('Return/Destroy Enviroment') {
                steps {
                    echo "Destroy Manually!!! for now"
                }
            }
        }
    }
}