pipeline {
    agent any
    options {
        ansiColor('xterm')
        timestamps()
        disableConcurrentBuilds()
    }

    post {
        always {
            step([$class: 'WsCleanup'])
        }
      }

    stages {
        stage('Checkout Branch'){
          steps {
            library "pipeline@$BRANCH_NAME"
          }
        }
        stage('Run Tests') {
            steps {
                    sh "./gradlew test"
            }
        }
    }
}
