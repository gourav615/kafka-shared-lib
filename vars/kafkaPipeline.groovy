def call(Map userConfig = [:]) {

    def defaultConfig = loadConfigFromResources()
    def config = defaultConfig + userConfig

    pipeline {
        agent any

        stages {

            stage('Init Config') {
                steps {
                    script {
                        env.SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
                        env.ENV           = config.ENVIRONMENT
                        env.CODE_PATH     = config.CODE_BASE_PATH
                        env.MESSAGE       = config.ACTION_MESSAGE
                    }
                }
            }

            stage('Clone') {
                steps {
                    echo "Cloning code for ${env.ENV}"
                    git branch: 'main', url: 'https://github.com/gourav615/kafka-ansible-project.git'
                }
            }

            stage('User Approval') {
                when {
                    expression { return config.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve deployment to ${env.ENV}?"
                }
            }

            stage('Playbook Execution') {
                steps {
                    echo "Running Ansible from ${env.CODE_PATH}"
                    sh """
                      cd ${env.CODE_PATH}
                      ansible-playbook kafka.yml
                    """
                }
            }

            stage('Notification') {
                steps {
                    echo "Sending Slack Notification"
                    slackSend(
                        channel: env.SLACK_CHANNEL,
                        message: "${env.MESSAGE} for environment: ${env.ENV}"
                    )
                }
            }
        }
    }
}

def loadConfigFromResources() {
    def text = libraryResource 'kafkaConfig.groovy'
    return evaluate(text)
}
