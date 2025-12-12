// GitHub Sync Manager - ENHANCED VERSION

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

// Options sekcija (Poboljšanje #10)
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

def diffDetectionEnabled = true
v.options.labelCustom("Options:", [style: "bold"])
def diffCheckbox = new com.vaadin.ui.CheckBox("Enable diff detection (skip unchanged)")
diffCheckbox.setValue(true)
diffCheckbox.addValueChangeListener({ event ->
    diffDetectionEnabled = event.value
})
v.options.addComponent(diffCheckbox)

// Result sekcija
v.addSection("result")
v.result.setMargin(true)

def syncAllActions = {
    v.result.clear()
    v.result.labelCustom("=== Sinhronizacija ===", [style: "h3"])
    v.result.labelCustom("Branch: ${selectedBranch}", [style: "bold"])
    v.result.labelCustom("Diff Detection: ${diffDetectionEnabled ? 'ON' : 'OFF'}", [style: "bold"])
    v.result.labelCustom("", [])
    
    try {
        def allActions = repo.find()
            .ofType("ACTION_DEFINITION")
            .limit(9999)
            .execute()
        
        def myActions = allActions.findAll { it.path?.contains("/VeljkoTest/") }
        
        v.result.labelCustom("Nadjeno akcija: ${myActions.size()}", [style: "bold"])
        v.result.labelCustom("", [])
        
        def successCount = 0
        def skipCount = 0
        def unchangedCount = 0
        def failCount = 0
        
        myActions.each { action ->
            try {
                def actionName = action.name
                
                log("Processing: ${actionName}")
                
                def scriptPath = "${action.path}/Scripts/action.groovy"
                def content = bo.getBinaryAsString(scriptPath)
                
                if (!content || content.trim().isEmpty()) {
                    v.result.labelCustom("⚠️ SKIP: ${actionName} - prazan sadrzaj", [color: "gray"])
                    skipCount++
                    return
                }
                
                def githubPath = "actions/${actionName}/action.groovy"
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
                        existingContent = response.content // Base64
                    }
                } catch (Exception e) {
                    log("${actionName} - New file")
                }
                
                // Poboljšanje #2: Diff Detection
                if (diffDetectionEnabled && existingContent) {
                    def localBase64 = content.bytes.encodeBase64().toString().replaceAll("\\s", "")
                    def remoteBase64 = existingContent.replaceAll("\\s", "")
                    
                    if (localBase64 == remoteBase64) {
                        v.result.labelCustom("⏭️ ${actionName} - No changes", [color: "blue"])
                        unchangedCount++
                        return
                    }
                }
                
                // Upload
                def payload = [
                    message: "Auto-sync: ${actionName}",
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
                
                if (connection.responseCode in [200, 201]) {
                    def statusIcon = connection.responseCode == 201 ? "✨" : "✅"
                    v.result.labelCustom("${statusIcon} ${actionName} (${content.length()} chars)", [color: "green"])
                    successCount++
                } else {
                    def errorText = connection.errorStream?.text ?: "No error details"
                    v.result.labelCustom("❌ ${actionName} - HTTP ${connection.responseCode}", [color: "red"])
                    failCount++
                }
                
            } catch (Exception e) {
                v.result.labelCustom("❌ ${action.name} - ${e.message}", [color: "red"])
                failCount++
            }
        }
        
        // Poboljšanje #7: Enhanced Report
        v.result.labelCustom("", [])
        v.result.labelCustom("=== REZULTAT ===", [style: "h3", color: "blue"])
        v.result.labelCustom("✅ Uploadovano: ${successCount}", [color: "green", style: "bold"])
        v.result.labelCustom("⏭️ Bez izmena: ${unchangedCount}", [color: "blue", style: "bold"])
        v.result.labelCustom("❌ Greske: ${failCount}", [color: "red", style: "bold"])
        
        // Poboljšanje #6: Timestamp
        if (successCount > 0) {
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
v.ab1.right.primarybutton("Sync to GitHub", syncAllActions)
v.ab1.right.button("Clear Log", { v.result.clear() })
