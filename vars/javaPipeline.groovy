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
                    sh './gradlew bootJar'
                    sh "docker build -f Dockerfile -t ${env.JOB_NAME} ."
                    sh "docker tag ${env.JOB_NAME}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
                }
            }
            
            stage('Publish') {
                steps {
                    //  THIS SHOULD ALL BE IN THE CODE AS A LOGIN METHOD
                    sh 'loginvar=$(aws ecr get-login --no-include-email --region us-west-2) && eval "$loginvar"'
                    //This should not be done, instead seperate repos
                    sh "aws ecr batch-delete-image --repository-name demo --image-ids imageTag=${env.JOB_NAME} --region us-west-2"
                    
                    sh "docker push ${AWS_ACCOUNT_ID}.dkr.ecr.us-west-2.amazonaws.com/demo:${env.JOB_NAME}"
                }
            }
            
            stage('Create Cluster') {
                steps {
                    //config
                    sh "/usr/local/bin/ecs-cli configure --region us-west-2 --cluster ${env.JOB_NAME} --default-launch-type FARGATE --config-name ${env.JOB_NAME}"
                    //create cluster
                    sh "/usr/local/bin/ecs-cli up --capability-iam --size 1 --instance-type t2.small --launch-type EC2 --cluster-config ${env.JOB_NAME} --force --region us-west-2"
                }
            }
            
            stage('Deploy services') {
                steps {
                    //deploy docker images -- MANIFEST. (TODO in proper code)
                    sh 'aws ecs register-task-definition --network-mode host --family demoapp1 --region us-west-2 --container-definitions "[{\\"name\\":\\"demoapp1\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp1\\",\\"cpu\\":256,\\"memory\\":512,\\"essential\\":true}]"'
                    sh 'aws ecs run-task --cluster demoapp1 --task-definition demoapp1 --count 1  --region us-west-2'
                    sh 'aws ecs register-task-definition --network-mode host --family demoapp2 --region us-west-2 --container-definitions "[{\\"name\\":\\"demoapp2\\",\\"image\\":\\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demoapp2\\",\\"cpu\\":256,\"memory\":512,\\"essential\\":true}]"'
                    sh 'aws ecs run-task --cluster demoapp2 --task-definition demoapp2 --count 1  --region us-west-2'
                }
            }
//config
//ecs-cli configure --region us-west-2 --cluster appnamehere --default-launch-type FARGATE --config-name appnamehere --force
//create cluster
//ecs-cli up --capability-iam --size 1 --instance-type t2.small --launch-type EC2 --cluster-config appnamehere --force --region us-west-2
//deploy docker images -- loop. (TODO in proper code)
//aws ecs register-task-definition --network-mode host --family appnamehere --region us-west-2 --container-definitions "[{\"name\":\"appnamehere\",\"image\":\"651524873607.dkr.ecr.us-west-2.amazonaws.com/demo:demo-app-1\",\"cpu\":256,\"memory\":512,\"essential\":true}]"
//run it -- MEM LIMIT
//aws ecs run-task --cluster appnamehere --task-definition appnamehere --count 1  --region us-west-2

//Manual destruction --REMEMBER

//Jenkins dropdown job

//deploy all services -- should be done with manifest
//move code to common so can demo drop down
//   DockerName.getNameAndTag(branchName, serviceName, subscription, version, isMasterRelease, this)

            
        }
    }
}
