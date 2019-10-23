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
        stage('Run Tests') {
            steps {
                    sh "./gradlew test"
            }
        }
    }
}
