pipeline {
    agent any

    // 每个阶段运行结束时的操作
    options {
        /*
            LogRotator构造参数分别为：
            daysToKeep: 构建记录将保存的天数
            numToKeep: 最多此数目的构建记录将被保存
            artifactDaysToKeep: 比此早的发布包将被删除，但构建的日志、操作历史、报告等将被保留
            artifactNumToKeep: 最多此数目的构建将保留他们的发布包
         */
        buildDiscarder(logRotator(numToKeepStr: '15', artifactDaysToKeepStr: '15', daysToKeepStr: '90'))
        // 构建超时时间
        timeout(time: 5, unit: 'MINUTES')
        // 不允许同时执行流水线。 可被用来防止同时访问共享资源等
        disableConcurrentBuilds()
    }

    environment {
        repoUrl = "http://43.139.83.195/root/jenkins-test.git"
    }

    parameters {
        string(name:'repoBranch', defaultValue: '', description: '分支')
    }

    triggers {
        GenericTrigger(
            genericVariables: [
              [key: 'branch', value: '$.ref']
            ],
            token: 'jsj' ,
            causeString: ' Triggered on $branch' ,
            printContributedVariables: true,
            printPostContent: true
        )
    }

    stages {
        stage('获取分支') {
            steps {
                script{
                  try{
                    if("${branch}" != ""){
                      println "----------webhook式触发-----------"
                      branchName = branch - "refs/heads"
                      branchName = sh(returnStdout: true,script: "echo ${branchName}|awk -F '/' '{print \$NF}'").trim()
                      println "webhook触发的分支是: " + "${branchName}"
                    }
                  } catch(exc) { }
                    if("${params.repoBranch}" != ""){
                      println "-----------手动方式触发------------"
                      branchName = "${params.repoBranch}"
                      println "手动触发的分支是: " + "${branchName}"
                    }
                }
            }
        }

        stage('获取代码') {
          steps{
            checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]], extensions: [], userRemoteConfigs: [[url: "${repoUrl}"]]])
          }
        }

        stage('代码打包') {
            steps {
                // 编译，构建本地镜像
                sh "mvn clean package"
                echo '构建完成'
            }
        }

         stage('镜像打包并运行') {
            steps {
                // 打包镜像
                sh "mvn dockerfile:build"

                // 停止并删除旧容器
                sh "docker stop jenkins-test && docker rm jenkins-test"

                // 运行镜像
                sh "docker run -d --name=jenkins-test -p 7999:7999 jenkins-test:latest"
            }
        }

    }
}
