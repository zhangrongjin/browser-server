pipeline {
    agent {
        docker { image 'gradle' }
    }
    stages {
        stage('build') {
            steps {
                sh 'gradle -x test build'
            }
        }
    }
}
