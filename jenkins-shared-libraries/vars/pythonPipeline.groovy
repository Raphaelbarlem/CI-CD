def call (body) {

  def settings = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = settings
  body()

  pipeline {
    agent {
      kubernetes {
        yamlFile 'jenkinsPod.yaml'
      }
    }
    environment {
      DISCORD_WEBHOOK = credentials('discord-webhook')
    }
    stages {
      stage('Unit tests') { 
        steps {
          pythonUnitTest {}
        }
        when {
          anyOf {
            branch pattern: "feature-*"
            branch pattern: "develop"
            branch pattern: "hotfix-*"
            branch pattern: "release-*"
            branch pattern: "v*"
          }
        }
      }
      stage('Sonarqube Scan') {
        environment {
          SONAR_HOST_URL = "http://sonarqube.localhost.com"
          SONAR_LOGIN    = credentials('sonar-scanner-cli')
        }
        steps {
          sonarqubeScan {}
        }
        when {
          anyOf {
            branch pattern: "feature-*"
            branch pattern: "develop"
            branch pattern: "hotfix-*"
            branch pattern: "release-*"
            branch pattern: "v*"
          }
        }
      }
      stage('Build and Push') {
        steps {
          kanikoBuildPush {}
        }
        when {
          anyOf {
            branch pattern: "develop"
            branch pattern: "hotfix-*"
          }
        }
      }
      stage('Harbor Security Scan') {
        environment {
          HARBOR_CREDENTIALS = credentials('harbor-credentials')
        }
        steps {
          harborSecurityScan {}
        }
        when {
          anyOf {
            branch pattern: "develop"
            branch pattern: "hotfix-*"
          }
        }
      }
      stage('Artifact Promotion') {
        steps {
          artifactPromotionCrane {}
        }
        when {
          anyOf {
            branch pattern: "release-*"
            branch pattern: "v*"
          }
        }
      }
      stage('Infrastructure Tests on K8s') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
        }
        steps {
          infraTestK8s {}
        }
        when {
          anyOf {
            branch pattern: "develop"
            branch pattern: "hotfix-*"
          }
        }
        post {
          always {
            container('helm') {
              sh '''
                helm delete -n citest flask-ci
              '''
            }
          }
        }
      }
      stage('Deploy to Development') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
        }
        steps {
          deployDev {}
        }
        when {
          anyOf {
            branch pattern: "develop"
          }
        }
      }
      stage('Deploy to Staging') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
        }
        steps {
          deployStg {}
        }
        when {
          anyOf {
            branch pattern: "release-*"
            branch pattern: "hotfix-*"
          }
        }
      }
      stage('Create Tag?') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
        }
        steps {
          input message: "Would you like to promote to production?"
          createTag {}
        }
        when {
          anyOf {
            branch pattern: "release-*"
            branch pattern: "hotfix-*"
          }
        }
      }
      stage('Deploy to Production') {
        environment {
          JENKINS_SSH_PRIVATE_KEY = credentials('jenkins-gitea')
        }
        steps {
          input message: "Deploy to Production?"
          deployPro {}
        }
        when {
          anyOf {
            branch pattern: "v*"
          }
        }
      }
    }
    post {
      always {
        discordSend description: "Jenkins Pipeline Build",
                footer: "${JOB_BASE_NAME} (build #${BUILD_NUMBER})",
                link: "${BUILD_URL}",
                result: currentBuild.currentResult,
                title: "${JOB_NAME}",
                webhookURL: "${DISCORD_WEBHOOK}",
                thumbnail: "https://www.errietta.me/blog/wp-content/uploads/2019/08/256.png"
      }
    }
  }
}