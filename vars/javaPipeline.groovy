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

//            stage('Package') {
//                steps {
//                    sh './gradlew bootJar'
//                    sh "docker build -f Dockerfile -t ${env.JOB_NAME} ."
//                    sh "docker tag ${env.JOB_NAME}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
//                }
//            }
//            
//            stage('Publish') {
//                steps {
//                    
//                    // AwsActions.logIn()
//                    
//                    sh 'loginvar=$(aws ecr get-login --no-include-email --region us-west-2) && eval "$loginvar"'
//                    sh "aws ecr batch-delete-image --repository-name demo --image-ids imageTag=${env.JOB_NAME} --region us-west-2"
//                    
//                    sh "docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
//                }
//            }
//            
//            stage('Create/Update Enviroment') {
//                when {
//                    expression {
//                        return env.JOB_NAME == "demoapp1"
//                    }
//                }
//                steps {
//                    //EnviromentActions.createEmptyEnviroment(env.JOB_NAME, this)
//                    //OR                    
//                    //EnviromentActions.createEmptyEnviroment(env.JOB_NAME, this)
//                    sh "/usr/local/bin/ecs-cli up --capability-iam --size 1 --instance-type t3.medium --launch-type EC2 --cluster-config probationbuilds --region us-west-2 --force"
//                }
//            }
//            
//            stage('Deploy/Update services') {
//                steps {
//                    sleep(60)
//                    //Should use a manifest (embedded json file) and below methods
//                    //EnviromentActions.populateEnviroment(env.JOB_NAME, this)
//                    //EnviromentActions.deploySpecificService(env.JOB_NAME, this)
//                    sh "aws ecs register-task-definition --network-mode none --family ${env.JOB_NAME}" + ' --region us-west-2 --container-definitions "[{\\"name\\":\\"demoapp1\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp1\\",\\"cpu\\":512,\\"memory\\":512,\\"essential\\":true}, {\\"name\\":\\"demoapp2\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp2\\",\\"cpu\\":512,\\"memory\\":512,\\"essential\\":true}]"'
//                    sh "aws ecs run-task --cluster probationbuilds --task-definition ${env.JOB_NAME} --count 1  --region us-west-2"
//                }
//            }        
//            
//            stage('Run Global Tests for probation') {
//                steps {
//                    sleep(300)
//                    echo "Running Tests"
//                }
//            }    
//            
//            stage('Return/Destroy Enviroment') {
//                steps {
//                    echo "Destroy Manually!!! for now"
//                }
//            }
        }
    }
}