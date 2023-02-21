package com.whitegive.devops.kubernetes

/**
 * 代碼打包
 * @param buildEnv 構建環境
 */
def buildCode(buildEnv) {
    sh "mvn clean package -P ${buildEnv} -U -Dmaven.test.skip=true"
}

/**
 * 获取模块名
 * @param serverName 服务名称
 */
static def getModuleName(serverName) {
    if (serverName.contains("sgp-")) {
        return serverName.replace("sgp-", "")
    }
    return serverName
}

/**
 * 获取环境目录
 * @param buildEnv 构建环境
 */
static def getEnvDirectory(buildEnv){
    // 测试、预发布环境共用Dockerfile
    return buildEnv == "stage" ? "test" : buildEnv
}


/**
 * 构建Java镜像
 * @param nameSpace 名称空间
 * @param serverName 服务名称
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryRegion 镜像仓库地区
 * @param repositoryUri 镜像仓库地址
 * @param buildEnv 构建环境
 */
def buildDockerImage(nameSpace, serverName, pomVersion, buildId, repositoryRegion, repositoryUri, buildEnv) {
    // 去除服务名称地区前缀
    def moduleName = getModuleName(serverName)
    // 获取环境目录
    def env = getEnvDirectory(buildEnv)

    // 構建docker鏡像
    sh "mv ./${moduleName}-server/target/${serverName}-server.jar ./${moduleName}-server/docker/${env}/app.jar"
    if ("dev" == buildEnv) {
        sh "docker login 10.0.15.249 -u admin -p Kpay@1026 && cd ${moduleName}-server/docker/${env} && docker build -t ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId} ."
    } else {
        sh "aws ecr get-login-password --region ${repositoryRegion} | docker login --username AWS --password-stdin ${repositoryUri}"
        sh "cd ${moduleName}-server/docker/${env} && docker build -t ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId} ."
    }
}

/**
 * 构建Java網關镜像
 * @param nameSpace 名称空间
 * @param serverName 服务名称
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryRegion 镜像仓库地区
 * @param repositoryUri 镜像仓库地址
 * @param buildEnv 构建环境
 */
def buildGatewayDockerImage(nameSpace, serverName, pomVersion, buildId, repositoryRegion, repositoryUri, buildEnv) {
    // 获取环境目录
    def env = getEnvDirectory(buildEnv)

    // 構建docker鏡像
    sh "mv ./target/${serverName}.jar ./docker/${env}/app.jar"
    if ("dev" == buildEnv) {
        sh "docker login 10.0.15.249 -u admin -p Kpay@1026 && cd docker/${env} && docker build -t ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId} ."
    } else {
        sh "aws ecr get-login-password --region ${repositoryRegion} | docker login --username AWS --password-stdin ${repositoryUri}"
        sh "cd docker/${env} && docker build -t ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId} ."
    }
}

/**
 * 构建Java調度服務镜像
 * @param nameSpace 名称空间
 * @param serverName 服务名称
 * @param modules 服务模块
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryRegion 镜像仓库地区
 * @param repositoryUri 镜像仓库地址
 * @param buildEnv 构建环境
 */
def buildScheduleDockerImage(nameSpace, serverName, modules, pomVersion, buildId, repositoryRegion, repositoryUri, buildEnv) {
    // 去除服务名称地区前缀
    def moduleName = getModuleName(serverName)
    // 获取环境目录
    def env = getEnvDirectory(buildEnv)

    sh "mv ./${moduleName}-${modules}/target/${serverName}-${modules}.jar ./${moduleName}-${modules}/docker/${env}/app.jar"
    // 構建docker鏡像
    if ("dev" == buildEnv) {
        sh "docker login 10.0.15.249 -u admin -p Kpay@1026 && cd ./${moduleName}-${modules}/docker/${env} && docker build -t ${repositoryUri}/${serverName}-${modules}:${nameSpace}-${pomVersion}-${buildId} ."
    } else {
        sh "aws ecr get-login-password --region ${repositoryRegion} | docker login --username AWS --password-stdin ${repositoryUri}"
        sh "cd ./${moduleName}-${modules}/docker/${env} && docker build -t ${repositoryUri}/${serverName}-${modules}:${nameSpace}-${pomVersion}-${buildId} ."
    }
}

/**
 * 推送Java镜像
 * @param nameSpace 名称空间
 * @param serverName 服务名称
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryUri 镜像仓库地址
 */
def pushDockerImage(nameSpace, serverName, pomVersion, buildId, repositoryUri) {
    sh "docker push ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId} && docker rmi ${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId}"
}

/**
 * 推送Schedule镜像
 * @param nameSpace 名称空间
 * @param serverName 服务名称
 * @param modules 服务模块
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryUri 镜像仓库地址
 */
def pushScheduleDockerImage(nameSpace, serverName, modules, pomVersion, buildId, repositoryUri) {
    sh "docker push ${repositoryUri}/${serverName}-${modules}:${nameSpace}-${pomVersion}-${buildId} && docker rmi ${repositoryUri}/${serverName}-${modules}:${nameSpace}-${pomVersion}-${buildId}"
}

/**
 * 更新微服务k8s部署
 * @param username 用户名
 * @param accessKey 访问密钥
 * @param clusterName 集群名称
 * @param nameSpace 命名空间
 * @param serverName 服务名称
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryUri 镜像仓库地址
 * @param apiHost api地址
 */
def updateDeployment(username, accessKey, clusterName, nameSpace, serverName, pomVersion, buildId, repositoryUri, apiHost) {
    sh """
         curl -X PUT \\
         -H "content-type: application/json" \\
         -H "Cookie: KuboardUsername=${username}; KuboardAccessKey=${accessKey}" \\
         -d '{"kind":"deployments","namespace":"${nameSpace}","name":"${serverName}","images":{"consul":"consul:1.10.10","${repositoryUri}/${serverName}":"${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId}"}}' \\
         "${apiHost}/kuboard-api/cluster/${clusterName}/kind/CICDApi/admin/resource/updateImageTag"
        """
}

/**
 * 更新應用k8s部署
 * @param username 用户名
 * @param accessKey 访问密钥
 * @param clusterName 集群名称
 * @param nameSpace 命名空间
 * @param serverName 服务名称
 * @param pomVersion 项目版本号
 * @param buildId 构建ID
 * @param repositoryUri 镜像仓库地址
 * @param apiHost api地址
 */
def updateAppDeployment(username, accessKey, clusterName, nameSpace, serverName, pomVersion, buildId, repositoryUri, apiHost) {
    sh """
         curl -X PUT \\
         -H "content-type: application/json" \\
         -H "Cookie: KuboardUsername=${username}; KuboardAccessKey=${accessKey}" \\
         -d '{"kind":"deployments","namespace":"${nameSpace}","name":"${serverName}","images":{"${repositoryUri}/${serverName}":"${repositoryUri}/${serverName}:${nameSpace}-${pomVersion}-${buildId}"}}' \\
         "${apiHost}/kuboard-api/cluster/${clusterName}/kind/CICDApi/admin/resource/updateImageTag"
        """
}
