// GitHub Sync Manager - FINAL VERSION + Error Details

showLogConsole()

def GITHUB_TOKEN = appConfig.B4C_COMMON.gtVeljko
def GITHUB_OWNER = "VeljaM3"
def GITHUB_REPO = "bura-test"
def GITHUB_BRANCH = "main"

v.addHeader("h1")
v.h1.topTitle.labelCustom("GitHub Sync Manager", [style: "h1"])
v.h1.masterTitle.labelCustom("Sinhronizacija VeljkoTest akcija", [style: "h4"])
v.h1.build()

v.addSection("result")
v.result.setMargin(true)

// Sync svih akcija na GitHub
def syncAllActions = {
    v.result.clear()
    v.result.labelCustom("=== Sinhronizacija svih akcija ===", [style: "h3"])
    
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
        def failCount = 0
        
        myActions.each { action ->
            try {
                def actionName = action.name
                log("Processing: ${actionName}")
                
                // Dohvati skriptu
                def scriptPath = "${action.path}/Scripts/action.groovy"
                def content = bo.getBinaryAsString(scriptPath)
                
                if (!content || content.trim().isEmpty()) {
                    v.result.labelCustom("⚠️ SKIP: ${actionName} - prazan sadrzaj", [color: "gray"])
                    skipCount++
                    return
                }
                
                log("Content length: ${content.length()}")
                
                // GitHub path
                def githubPath = "actions/${actionName}/action.groovy"
                def apiUrl = "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/contents/${githubPath}"
                
                // Provera SHA
                def existingSha = null
                try {
                    def getConnection = new URL(apiUrl).openConnection()
                    getConnection.setRequestMethod("GET")
                    getConnection.setRequestProperty("Authorization", "token ${GITHUB_TOKEN}")
                    getConnection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    
                    if (getConnection.responseCode == 200) {
                        def response = new groovy.json.JsonSlurper().parseText(getConnection.inputStream.text)
                        existingSha = response.sha
                        log("${actionName} - Existing SHA: ${existingSha}")
                    } else {
                        log("${actionName} - File does not exist (${getConnection.responseCode})")
                    }
                } catch (Exception e) {
                    log("${actionName} - File lookup error: ${e.message}")
                }
                
                // Upload
                def payload = [
                    message: "Auto-sync: ${actionName}",
                    content: content.bytes.encodeBase64().toString(),
                    branch: GITHUB_BRANCH
                ]
                
                if (existingSha) {
                    payload.sha = existingSha
                    log("${actionName} - Updating with SHA: ${existingSha}")
                } else {
                    log("${actionName} - Creating new file")
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
                
                def responseCode = connection.responseCode
                
                if (responseCode in [200, 201]) {
                    v.result.labelCustom("✅ ${actionName}", [color: "green"])
                    successCount++
                } else {
                    // Detaljni error log
                    def errorText = ""
                    try {
                        errorText = connection.errorStream?.text ?: "No error details"
                    } catch (Exception e) {
                        errorText = "Could not read error: ${e.message}"
                    }
                    
                    v.result.labelCustom("❌ ${actionName} - HTTP ${responseCode}", [color: "red"])
                    v.result.labelCustom("   Error: ${errorText.take(200)}", [color: "red"])
                    
                    log("${actionName} - ERROR ${responseCode}")
                    log("${actionName} - Error details: ${errorText}")
                    log("${actionName} - Payload SHA: ${payload.sha}")
                    log("${actionName} - Content length: ${content.length()}")
                    
                    failCount++
                }
                
            } catch (Exception e) {
                v.result.labelCustom("❌ ${action.name} - ${e.message}", [color: "red"])
                log("Exception: ${e.message}")
                e.printStackTrace()
                failCount++
            }
        }
        
        v.result.labelCustom("", [])
        v.result.labelCustom("=== REZULTAT ===", [style: "h3", color: "blue"])
        v.result.labelCustom("Uspesno: ${successCount}", [color: "green", style: "bold"])
        v.result.labelCustom("Preskoceno: ${skipCount}", [color: "gray"])
        v.result.labelCustom("Neuspesno: ${failCount}", [color: "red", style: "bold"])
        
    } catch (Exception e) {
        v.result.labelCustom("ERROR: ${e.message}", [color: "red", style: "bold"])
        e.printStackTrace()
    }
}

v.addActionBar("ab1")
v.ab1.right.primarybutton("Sync All Actions to GitHub", syncAllActions)
