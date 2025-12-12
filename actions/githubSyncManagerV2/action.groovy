// GitHub Sync Manager - ENHANCED VERSION + NodeConfig Sync

showLogConsole()

// Config
def b4cCommonConfig = appConfig.B4C_COMMON
def GITHUB_TOKEN = b4cCommonConfig.gtVeljko
def GITHUB_OWNER = "VeljaM3"
def GITHUB_REPO = "bura-test"
def GITHUB_BRANCH = "main"

v.addHeader("h1")
v.h1.topTitle.labelCustom("GitHub Sync Manager - Enhanced", [style: "h1"])
v.h1.masterTitle.labelCustom("Smart sync sa diff detection", [style: "h4"])
v.h1.build()

// Options sekcija
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

def diffDetectionEnabled = true
def diffCheckbox = new com.vaadin.ui.CheckBox("Enable diff detection (skip unchanged)")
diffCheckbox.setValue(true)
diffCheckbox.addValueChangeListener({ event ->
    diffDetectionEnabled = event.value
})
v.options.addComponent(diffCheckbox)

// Novi checkbox za Actions
def syncActionsEnabled = true
def actionsCheckbox = new com.vaadin.ui.CheckBox("Sync Action Definitions")
actionsCheckbox.setValue(true)
actionsCheckbox.addValueChangeListener({ event ->
    syncActionsEnabled = event.value
})
v.options.addComponent(actionsCheckbox)

// Novi checkbox za NodeConfigs
def syncNodeConfigsEnabled = true
def nodeConfigsCheckbox = new com.vaadin.ui.CheckBox("Sync Node Configs")
nodeConfigsCheckbox.setValue(true)
nodeConfigsCheckbox.addValueChangeListener({ event ->
    syncNodeConfigsEnabled = event.value
})
v.options.addComponent(nodeConfigsCheckbox)

// Result sekcija
v.addSection("result")
v.result.setMargin(true)

// Helper funkcija za upload na GitHub
def uploadToGitHub = { githubPath, content, commitMessage ->
    def apiUrl = "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/contents/${githubPath}"
    
    // Provera SHA
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
    
    // Diff Detection
    if (diffDetectionEnabled && existingContent) {
        def localBase64 = content.bytes.encodeBase64().toString().replaceAll("\\s", "")
        def remoteBase64 = existingContent.replaceAll("\\s", "")
        
        if (localBase64 == remoteBase64) {
            return [status: "unchanged", code: 304]
        }
    }
    
    // Upload
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

// Sync Actions
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
            log("Processing action: ${actionName}")
            
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

// Sync NodeConfigs
def syncNodeConfigs = { stats ->
    v.result.labelCustom("", [])
    v.result.labelCustom("## Syncing Node Configs...", [style: "h3", color: "blue"])
    
    def allNodeConfigs = repo.find()
        .ofType("NODE_CONFIG")
        .limit(9999)
        .execute()
    
    def myNodeConfigs = allNodeConfigs.findAll { it.path?.contains("/VeljkoTest/") }
    
    v.result.labelCustom("Found ${myNodeConfigs.size()} node configs", [style: "bold"])
    
    myNodeConfigs.each { nodeConfig ->
        try {
            def configName = nodeConfig.name
            log("Processing node config: ${configName}")
            
            // Ekstraktuj sve properties iz NodeConfig-a
            def configData = [
                name: nodeConfig.name,
                path: nodeConfig.path,
                definitionName: nodeConfig.definitionName,
                properties: [:]
            ]
            
            // Uzmi sve properties
            try {
                def properties = nodeConfig.properties
                if (properties && properties.data) {
                    properties.data.each { prop ->
                        // Uzmi ime i vrednost property-ja
                        def propName = prop.name
                        def propValue = null
                        
                        try {
                            propValue = nodeConfig[propName]
                        } catch (Exception e) {
                            propValue = "N/A"
                        }
                        
                        configData.properties[propName] = propValue
                    }
                }
            } catch (Exception e) {
                log("Could not extract properties for ${configName}: ${e.message}")
            }
            
            // Konvertuj u JSON
            def jsonContent = groovy.json.JsonOutput.prettyPrint(
                groovy.json.JsonOutput.toJson(configData)
            )
            
            def githubPath = "nodeConfigs/${configName}.json"
            def result = uploadToGitHub(githubPath, jsonContent, "Auto-sync: NodeConfig ${configName}")
            
            if (result.status == "unchanged") {
                v.result.labelCustom("⏭️ ${configName} - No changes", [color: "blue"])
                stats.unchangedCount++
            } else if (result.code in [200, 201]) {
                def statusIcon = result.code == 201 ? "✨" : "✅"
                v.result.labelCustom("${statusIcon} ${configName} (${jsonContent.length()} chars)", [color: "green"])
                stats.successCount++
            } else {
                v.result.labelCustom("❌ ${configName} - HTTP ${result.code}", [color: "red"])
                stats.failCount++
            }
            
        } catch (Exception e) {
            v.result.labelCustom("❌ ${nodeConfig.name} - ${e.message}", [color: "red"])
            stats.failCount++
        }
    }
}

// Main sync funkcija
def syncToGitHub = {
    v.result.clear()
    v.result.labelCustom("=== Sinhronizacija ===", [style: "h3"])
    v.result.labelCustom("Branch: ${selectedBranch}", [style: "bold"])
    v.result.labelCustom("Diff Detection: ${diffDetectionEnabled ? 'ON' : 'OFF'}", [style: "bold"])
    v.result.labelCustom("", [])
    
    try {
        def stats = [
            successCount: 0,
            skipCount: 0,
            unchangedCount: 0,
            failCount: 0
        ]
        
        // Sync Actions
        if (syncActionsEnabled) {
            syncActions(stats)
        } else {
            v.result.labelCustom("⏭️ Actions sync disabled", [color: "gray"])
        }
        
        // Sync NodeConfigs
        if (syncNodeConfigsEnabled) {
            syncNodeConfigs(stats)
        } else {
            v.result.labelCustom("⏭️ NodeConfigs sync disabled", [color: "gray"])
        }
        
        // Enhanced Report
        v.result.labelCustom("", [])
        v.result.labelCustom("=== REZULTAT ===", [style: "h3", color: "blue"])
        v.result.labelCustom("✅ Uploadovano: ${stats.successCount}", [color: "green", style: "bold"])
        v.result.labelCustom("⏭️ Bez izmena: ${stats.unchangedCount}", [color: "blue", style: "bold"])
        v.result.labelCustom("⚠️ Preskoceno: ${stats.skipCount}", [color: "gray"])
        v.result.labelCustom("❌ Greske: ${stats.failCount}", [color: "red", style: "bold"])
        
        // Timestamp
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
