// Skill — même logique que le pipeline Formation (node + sparse checkout + SSH vers la VM kubeadm),
// avec les mêmes infos que Jenkinsfile.cd Skill : IMAGE_NAME emna450/backend-skill, IMAGE_TAG, K8S_NAMESPACE (défaut skill).
// Coller dans un job Jenkins « Pipeline » (scripted) ou adapter repoUrl / branch / IP / credentials.

node {
    def repoUrl = 'https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git'
    def branch = 'projectskills'
    def moduleDir = 'BackEnd/Microservices/Skill'
    def k8sSshCredentials = 'vm-k8s-ssh'
    def nsMysql = 'mysql'
    def IMAGE_NAME = 'emna450/backend-skill'

    def jdkHome = tool(name: 'JDK17', type: 'hudson.model.JDK')
    if (!jdkHome?.trim()) {
        jdkHome = (env.JAVA_HOME ?: '').trim()
    }
    if (!jdkHome?.trim()) {
        error 'JDK17 (Global Tool Configuration) a un chemin vide : sous JDK17, clique « Ajouter un installateur », choisis par ex. Adoptium Temurin 17, enregistre. Sans installateur, Jenkins ne telecharge pas le JDK.'
    }

    withEnv([
        "JAVA_HOME=${jdkHome}",
        "PATH+JDK=${jdkHome}/bin"
    ]) {
        properties([
            parameters([
                string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag to deploy'),
                string(name: 'K8S_NAMESPACE', defaultValue: 'skill', description: 'Kubernetes namespace (microservice Skill)'),
                string(name: 'K8S_VM_HOST', defaultValue: '192.168.17.131', description: 'IP ou hostname de la VM (sshd + kubectl ; ex. ip a sur ens33)')
            ]),
            disableConcurrentBuilds(),
            buildDiscarder(logRotator(numToKeepStr: '20'))
        ])

        // Après le 1er lancement, paramètres du job.
        def deployTag = (params?.IMAGE_TAG?.toString()?.trim()) ?: 'latest'
        def nsApp = (params?.K8S_NAMESPACE?.toString()?.trim()) ?: 'skill'
        def k8sVmHost = (params?.K8S_VM_HOST?.toString()?.trim()) ?: '192.168.17.131'
        def k8sSshRemote = "ubuntu@${k8sVmHost}"
        def skillImage = "${IMAGE_NAME}:${deployTag}"

        timeout(time: 90, unit: 'MINUTES') {
            stage('Checkout') {
                retry(3) {
                    sh """
                        set -eux
                        rm -rf .git
                        git init .
                        git remote add origin '${repoUrl}' || git remote set-url origin '${repoUrl}'
                        git config core.sparseCheckout true
                        git config remote.origin.promisor true
                        git config remote.origin.partialclonefilter blob:none
                        mkdir -p .git/info
                        printf '%s\\n' '${moduleDir}' 'BackEnd/infra/k8s' > .git/info/sparse-checkout
                        git -c protocol.version=2 fetch --no-tags --depth=1 origin '${branch}'
                        git checkout -f FETCH_HEAD
                    """
                }
            }

            stage('Deploy Kubernetes') {
                dir(moduleDir) {
                    sshagent([k8sSshCredentials]) {
                        sh """
                            set -eux
                            SSH='ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=30'
                            SCP='scp -o StrictHostKeyChecking=accept-new -o ConnectTimeout=30'
                            \${SCP} ../../infra/k8s/mysql/mysql.yaml ${k8sSshRemote}:/tmp/mysql.yaml
                            \${SCP} ../../infra/k8s/namespaces-labels.yaml ${k8sSshRemote}:/tmp/namespaces-labels.yaml
                            \${SSH} ${k8sSshRemote} 'mkdir -p /tmp/jenkins-skill-k8s'
                            mkdir -p /tmp/jenkins-skill-k8s-local
                            sed "s|__NAMESPACE__|${nsApp}|g" k8s/skill/secret.yaml > /tmp/jenkins-skill-k8s-local/secret.yaml
                            sed "s|__NAMESPACE__|${nsApp}|g" k8s/skill/deployment.yaml | sed "s|__IMAGE__|${skillImage}|g" > /tmp/jenkins-skill-k8s-local/deployment.yaml
                            sed "s|__NAMESPACE__|${nsApp}|g" k8s/skill/service.yaml > /tmp/jenkins-skill-k8s-local/service.yaml
                            \${SCP} /tmp/jenkins-skill-k8s-local/secret.yaml /tmp/jenkins-skill-k8s-local/deployment.yaml /tmp/jenkins-skill-k8s-local/service.yaml ${k8sSshRemote}:/tmp/jenkins-skill-k8s/
                            \${SSH} ${k8sSshRemote} 'kubectl apply --validate=false -f /tmp/namespaces-labels.yaml && kubectl apply --validate=false -f /tmp/mysql.yaml && kubectl rollout status deployment/mysql -n ${nsMysql} --timeout=180s && kubectl apply --validate=false -f /tmp/jenkins-skill-k8s/secret.yaml && kubectl apply --validate=false -f /tmp/jenkins-skill-k8s/deployment.yaml && kubectl apply --validate=false -f /tmp/jenkins-skill-k8s/service.yaml'
                        """
                    }
                }
            }

            stage('Monitoring') {
                sshagent([k8sSshCredentials]) {
                    sh """
                        set -eux
                        ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=30 ${k8sSshRemote} 'kubectl rollout status deployment/skill-ms -n ${nsApp} --timeout=120s && kubectl get pods -n ${nsApp} -l app=skill-ms -o wide && kubectl get svc skill-ms -n ${nsApp}'
                    """
                }
            }
        }
    }
}
