def call(Map userConfig = [:]) {

    // Load default config from resources
    def defaultConfig = loadConfigFromResources()
    def config = defaultConfig + userConfig   // user overrides defaults

    pipeline {
        agent any

        environment {
            SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
            ENV           = config.ENVIRONMENT
            CODE_PATH     = config.CODE_BASE_PATH
            MESSAGE       = config.ACTION_MESSAGE
        }

        stages {

            stage('Clone') {
                steps {
                    echo "Cloning code for ${ENV}"
                    git branch: 'main', url: 'https://github.com/your-org/kafka-ansible.git'
                }
            }

            stage('User Approval') {
                when {
                    expression { return config.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve deployment to ${ENV}?"
                }
            }

            stage('Playbook Execution') {
                steps {
                    echo "Running Ansible from ${CODE_PATH}"
                    sh """
                      cd ${CODE_PATH}
                      ansible-playbook kafka.yml
                    """
                }
            }

            stage('Notification') {
                steps {
                    echo "Sending Slack Notification"
                    slackSend(
                        channel: SLACK_CHANNEL,
                        message: MESSAGE + " for environment: " + ENV
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

