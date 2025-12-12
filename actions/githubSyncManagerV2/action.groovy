// GitHub Sync Manager - Always-On Diff Detection

showLogConsole()

def b4cCommonConfig = appConfig.B4C_COMMON
def GITHUB_TOKEN = b4cCommonConfig.gtVeljko
def GITHUB_OWNER = "VeljaM3"
def GITHUB_REPO = "bura-test"
def GITHUB_BRANCH = "main"

// Diff detection je UVEK uključen
def diffDetectionEnabled = true

v.addHeader("h1")
v.h1.topTitle.labelCustom("GitHub Sync Manager", [style: "h1"])
v.h1.masterTitle.labelCustom("Smart sync sa automatskim diff detection", [style: "h4"])
v.h1.build()

v.addSection("options")
v.options.setMargin(true)
v.options.addStyle("gray")

def selectedBranch = "main"
v.options.labelCustom("Target Branch:", [style: "bold"])
def branchCombo = new com.vaadin.ui.ComboBox()
branchCombo.setItems(["main", "develop", "staging"])
branchCombo.setValue("main")
branchCombo.addValueChangeListener({ event ->
    selectedBranch = event.value
})
v.options.addComponent(branchCombo)

v.options.labelCustom("", [])
v.options.labelCustom("Sync Options:", [style: "bold"])

def syncActionsEnabled = true
def actionsCheckbox = new com.vaadin.ui.CheckBox("Sync Action Definitions")
actionsCheckbox.setValue(true)
actionsCheckbox.addValueChangeListener({ event ->
    syncActionsEnabled = event.value
})
v.options.addComponent(actionsCheckbox)

def syncNodeConfigsEnabled = true
def nodeConfigsCheckbox = new com.vaadin.ui.CheckBox("Sync Node Configs")
nodeConfigsCheckbox.setValue(true)
nodeConfigsCheckbox.addValueChangeListener({ event ->
    syncNodeConfigsEnabled = event.value
})
v.options.addComponent(nodeConfigsCheckbox)

v.addSection("result")
v.result.setMargin(true)

def uploadToGitHub = { githubPath, content, commitMessage ->
    def apiUrl = "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/contents/${githubPath}"
    
    def existingSha = null
    def existingContent = null
    
    try {
        def getConnection = new URL(apiUrl).openConnection()
        getConnection.setRequestMethod("GET")
        getConnection.setRequestProperty("Authorization", "token ${GITHUB_TOKEN}")
        getConnection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        
        if (getConnection.responseCode == 200) {
            def response = new groovy.json.JsonSlurper().parseText(getConnection.inputStream.text)
            existingSha = response.sha
            existingContent = response.content
        }
    } catch (Exception e) {
        log("${githubPath} - New file")
    }
    
    // Diff detection UVEK AKTIVAN
    if (existingContent) {
        def localBase64 = content.bytes.encodeBase64().toString().replaceAll("\\s", "")
        def remoteBase64 = existingContent.replaceAll("\\s", "")
        
        if (localBase64 == remoteBase64) {
            return [status: "unchanged", code: 304]
        }
    }
    
    def payload = [
        message: commitMessage,
        content: content.bytes.encodeBase64().toString(),
        branch: selectedBranch
    ]
    
    if (existingSha) {
        payload.sha = existingSha
    }
    
    def connection = new URL(apiUrl).openConnection()
    connection.setRequestMethod("PUT")
    connection.setDoOutput(true)
    connection.setRequestProperty("Authorization", "token ${GITHUB_TOKEN}")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
    
    connection.outputStream.withWriter { writer ->
        writer.write(groovy.json.JsonOutput.toJson(payload))
    }
    
    return [status: "uploaded", code: connection.responseCode]
}

def syncActions = { stats ->
    v.result.labelCustom("## Syncing Actions...", [style: "h3", color: "blue"])
    
    def allActions = repo.find()
        .ofType("ACTION_DEFINITION")
        .limit(9999)
        .execute()
    
    def myActions = allActions.findAll { it.path?.contains("/VeljkoTest/") }
    
    v.result.labelCustom("Found ${myActions.size()} actions", [style: "bold"])
    
    myActions.each { action ->
        try {
            def actionName = action.name
            
            def scriptPath = "${action.path}/Scripts/action.groovy"
            def content = bo.getBinaryAsString(scriptPath)
            
            if (!content || content.trim().isEmpty()) {
                v.result.labelCustom("⚠️ SKIP: ${actionName} - prazan sadrzaj", [color: "gray"])
                stats.skipCount++
                return
            }
            
            def githubPath = "actions/${actionName}/action.groovy"
            def result = uploadToGitHub(githubPath, content, "Auto-sync: ${actionName}")
            
            if (result.status == "unchanged") {
                v.result.labelCustom("⏭️ ${actionName} - No changes", [color: "blue"])
                stats.unchangedCount++
            } else if (result.code in [200, 201]) {
                def statusIcon = result.code == 201 ? "✨" : "✅"
                v.result.labelCustom("${statusIcon} ${actionName} (${content.length()} chars)", [color: "green"])
                stats.successCount++
            } else {
                v.result.labelCustom("❌ ${actionName} - HTTP ${result.code}", [color: "red"])
                stats.failCount++
            }
            
        } catch (Exception e) {
            v.result.labelCustom("❌ ${action.name} - ${e.message}", [color: "red"])
            stats.failCount++
        }
    }
}

def parsePropertyWithScripts = { propertyPath, nodeName, allScripts ->
    def propertyData = [
        name: propertyPath.split("/").last(),
        path: propertyPath,
        scripts: [:]
    ]
    
    try {
        def propDetails = repo.get(propertyPath)
        propertyData.definitionName = propDetails.definitionName
        
        if (propDetails.properties?.data) {
            propDetails.properties.data.each { p ->
                try {
                    def val = propDetails[p.name]
                    if (val != null && p.name != "scripts") {
                        propertyData[p.name] = val.toString()
                    }
                } catch (Exception e) {
                    // Skip
                }
            }
        }
    } catch (Exception e) {
        log("  Could not get property details for ${propertyPath}")
    }
    
    def scriptTypes = ["LOOKUP", "ONCHANGE", "ONCREATE", "VALIDATOR"]
    
    scriptTypes.each { scriptType ->
        try {
            def scriptFolderPath = "${propertyPath}/Scripts/${scriptType}"
            def scriptsInFolder = repo.find()
                .onPath(scriptFolderPath)
                .ofType("FILE")
                .limit(999)
                .execute()
            
            if (scriptsInFolder && !scriptsInFolder.isEmpty()) {
                log("    Found ${scriptsInFolder.size()} ${scriptType} scripts for ${propertyData.name}")
                
                scriptsInFolder.each { scriptFile ->
                    try {
                        def scriptContent = bo.getBinaryAsString(scriptFile.path)
                        if (scriptContent) {
                            def scriptName = scriptFile.name
                            
                            allScripts << [
                                nodeName: nodeName,
                                propertyName: propertyData.name,
                                scriptType: scriptType,
                                scriptName: scriptName,
                                content: scriptContent
                            ]
                            
                            if (!propertyData.scripts[scriptType]) {
                                propertyData.scripts[scriptType] = []
                            }
                            propertyData.scripts[scriptType] << scriptName
                        }
                    } catch (Exception e) {
                        log("    Could not read script ${scriptFile.name}: ${e.message}")
                    }
                }
            }
        } catch (Exception e) {
            log("    No ${scriptType} scripts for ${propertyData.name}")
        }
    }
    
    return propertyData
}

def parseNodeStructure = { nodePath, allScripts ->
    def nodeData = [
        name: nodePath.split("/").last(),
        path: nodePath,
        properties: [:],
        dataTemplates: [],
        subnodes: [:]
    ]
    
    try {
        def nodeObj = repo.get(nodePath)
        if (nodeObj) {
            nodeData.definitionName = nodeObj.definitionName
        }
    } catch (Exception e) {
        log("Could not get node object for ${nodePath}")
    }
    
    try {
        def props = repo.find()
            .onPath("${nodePath}/Properties")
            .limit(999)
            .execute()
        
        log("  Found ${props.size()} properties")
        
        def directProps = props.findAll { prop ->
            def relativePath = prop.path.replace("${nodePath}/Properties/", "")
            !relativePath.contains("/")
        }
        
        log("  Filtered to ${directProps.size()} direct properties")
        
        directProps.each { prop ->
            try {
                def propData = parsePropertyWithScripts(prop.path, nodeData.name, allScripts)
                nodeData.properties[prop.name] = propData
            } catch (Exception e) {
                log("  Could not process property ${prop.name}: ${e.message}")
            }
        }
    } catch (Exception e) {
        log("  No properties for ${nodePath}")
    }
    
    try {
        def templates = repo.find()
            .onPath("${nodePath}/DataTemplates")
            .limit(999)
            .execute()
        
        log("  Found ${templates.size()} data templates")
        
        templates.each { template ->
            nodeData.dataTemplates << template.name
        }
    } catch (Exception e) {
        log("  No data templates for ${nodePath}")
    }
    
    return nodeData
}

def syncNodeConfigs = { stats ->
    v.result.labelCustom("", [])
    v.result.labelCustom("## Syncing Node Configs...", [style: "h3", color: "blue"])
    
    try {
        def allNodesUnderConfig = repo.find()
            .onPath("/Configuration/Apps/VeljkoTest/NodeConfig")
            .limit(9999)
            .execute()
        
        log("Found ${allNodesUnderConfig.size()} total items under NodeConfig")
        
        def nodeDefinitions = allNodesUnderConfig.findAll { node ->
            def pathParts = node.path.split("/")
            def nodeConfigIndex = pathParts.findIndexOf { it == "NodeConfig" }
            def isDirectChild = (nodeConfigIndex >= 0 && pathParts.size() == nodeConfigIndex + 2)
            
            if (isDirectChild) {
                log("Direct child found: ${node.name} at ${node.path}")
            }
            
            return isDirectChild
        }
        
        log("Filtered to ${nodeDefinitions.size()} direct node definitions")
        v.result.labelCustom("Found ${nodeDefinitions.size()} node definitions", [style: "bold"])
        
        if (nodeDefinitions.isEmpty()) {
            v.result.labelCustom("⚠️ No node definitions found", [color: "gray"])
            return
        }
        
        nodeDefinitions.each { nodeDef ->
            try {
                def nodeName = nodeDef.name
                log("Processing node config: ${nodeName}")
                
                def allScripts = []
                
                def configData = parseNodeStructure(nodeDef.path, allScripts)
                
                try {
                    def subnodesPath = "${nodeDef.path}/Subnodes"
                    def subnodesList = repo.find()
                        .onPath(subnodesPath)
                        .limit(999)
                        .execute()
                    
                    log("  Found ${subnodesList.size()} items under Subnodes")
                    
                    def directSubnodes = subnodesList.findAll { subnode ->
                        def relativePath = subnode.path.replace("${subnodesPath}/", "")
                        !relativePath.contains("/")
                    }
                    
                    log("  Filtered to ${directSubnodes.size()} direct subnodes")
                    
                    directSubnodes.each { subnode ->
                        try {
                            def subnodeName = subnode.name
                            log("    Processing subnode: ${subnodeName}")
                            
                            def subnodeData = parseNodeStructure(subnode.path, allScripts)
                            configData.subnodes[subnodeName] = subnodeData
                            
                        } catch (Exception e) {
                            log("    Error processing subnode ${subnode.name}: ${e.message}")
                        }
                    }
                    
                } catch (Exception e) {
                    log("  No subnodes for ${nodeName}: ${e.message}")
                }
                
                def jsonContent = groovy.json.JsonOutput.prettyPrint(
                    groovy.json.JsonOutput.toJson(configData)
                )
                
                def jsonGithubPath = "nodeConfigs/${nodeName}/${nodeName}.json"
                def jsonResult = uploadToGitHub(jsonGithubPath, jsonContent, "Auto-sync: NodeConfig ${nodeName} metadata")
                
                def nodeSuccessCount = 0
                def nodeUnchangedCount = 0
                
                if (jsonResult.status == "unchanged") {
                    nodeUnchangedCount++
                } else if (jsonResult.code in [200, 201]) {
                    nodeSuccessCount++
                }
                
                log("  Uploading ${allScripts.size()} scripts for ${nodeName}")
                
                allScripts.each { script ->
                    try {
                        def scriptGithubPath = "nodeConfigs/${script.nodeName}/Scripts/${script.scriptType}/${script.propertyName}_${script.scriptName}"
                        def scriptResult = uploadToGitHub(scriptGithubPath, script.content, "Auto-sync: ${script.nodeName} - ${script.propertyName} ${script.scriptType}")
                        
                        if (scriptResult.status == "unchanged") {
                            nodeUnchangedCount++
                        } else if (scriptResult.code in [200, 201]) {
                            nodeSuccessCount++
                        }
                    } catch (Exception e) {
                        log("  Error uploading script: ${e.message}")
                    }
                }
                
                if (nodeSuccessCount > 0) {
                    v.result.labelCustom("✅ ${nodeName} - ${nodeSuccessCount} files", [color: "green"])
                    stats.successCount += nodeSuccessCount
                }
                if (nodeUnchangedCount > 0) {
                    v.result.labelCustom("⏭️ ${nodeName} - ${nodeUnchangedCount} unchanged", [color: "blue"])
                    stats.unchangedCount += nodeUnchangedCount
                }
                
            } catch (Exception e) {
                v.result.labelCustom("❌ ${nodeDef.name} - ${e.message}", [color: "red"])
                log("Error processing ${nodeDef.name}: ${e.message}")
                e.printStackTrace()
                stats.failCount++
            }
        }
        
    } catch (Exception e) {
        v.result.labelCustom("❌ NodeConfig sync error: ${e.message}", [color: "red"])
        log("NodeConfig sync error: ${e.message}")
        e.printStackTrace()
        stats.failCount++
    }
}

def syncToGitHub = {
    v.result.clear()
    v.result.labelCustom("=== Sinhronizacija ===", [style: "h3"])
    v.result.labelCustom("Branch: ${selectedBranch}", [style: "bold"])
    v.result.labelCustom("Diff Detection: ON (auto)", [style: "bold", color: "green"])
    v.result.labelCustom("", [])
    
    try {
        def stats = [
            successCount: 0,
            skipCount: 0,
            unchangedCount: 0,
            failCount: 0
        ]
        
        if (syncActionsEnabled) {
            syncActions(stats)
        } else {
            v.result.labelCustom("⏭️ Actions sync disabled", [color: "gray"])
        }
        
        if (syncNodeConfigsEnabled) {
            syncNodeConfigs(stats)
        } else {
            v.result.labelCustom("⏭️ NodeConfigs sync disabled", [color: "gray"])
        }
        
        v.result.labelCustom("", [])
        v.result.labelCustom("=== REZULTAT ===", [style: "h3", color: "blue"])
        v.result.labelCustom("✅ Uploadovano: ${stats.successCount}", [color: "green", style: "bold"])
        v.result.labelCustom("⏭️ Bez izmena: ${stats.unchangedCount}", [color: "blue", style: "bold"])
        v.result.labelCustom("⚠️ Preskoceno: ${stats.skipCount}", [color: "gray"])
        v.result.labelCustom("❌ Greske: ${stats.failCount}", [color: "red", style: "bold"])
        
        if (stats.successCount > 0) {
            def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
            v.result.labelCustom("", [])
            v.result.labelCustom("Last sync: ${timestamp}", [color: "gray"])
        }
        
    } catch (Exception e) {
        v.result.labelCustom("ERROR: ${e.message}", [color: "red", style: "bold"])
        e.printStackTrace()
    }
}

v.addActionBar("ab1")
v.ab1.right.primarybutton("Sync to GitHub", syncToGitHub)
v.ab1.right.button("Clear Log", { v.result.clear() })
