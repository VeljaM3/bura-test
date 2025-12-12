// GitHub Commit - Single Action Push (PARAMETER-BASED)

showLogConsole()

def b4cCommonConfig = appConfig.B4C_COMMON
def GITHUB_TOKEN = b4cCommonConfig.gtVeljko
def DISCORD_WEBHOOK = b4cCommonConfig.discordWebhook
def GITHUB_OWNER = "VeljaM3"
def GITHUB_REPO = "bura-test"
def GITHUB_BRANCH = "main"

// ===================================
// KONFIGURISANJE - POSTAVI SVOJE VREDNOSTI
// ===================================
def APP_NAME = "VeljkoTest"
def ACTION_NAME = "githubCommit"  // ← PROMENI OVO ZA SVAKU AKCIJU
// ===================================

v.addHeader("h1")
v.h1.topTitle.labelCustom("GitHub Commit", [style: "h1"])
v.h1.masterTitle.labelCustom("Quick commit: ${ACTION_NAME}", [style: "h4"])
v.h1.build()

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
        branch: GITHUB_BRANCH
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

def sendToDiscord = { actionName, appName, status, message ->
    if (!DISCORD_WEBHOOK) {
        return
    }
    
    try {
        def embedColor = 3066993 // Green
        if (status == "error") {
            embedColor = 15158332 // Red
        } else if (status == "unchanged") {
            embedColor = 3447003 // Blue
        }
        
        def timestamp = new Date()
        def isoTimestamp = String.format("%tFT%<tTZ", timestamp)
        
        def discordPayload = [
            embeds: [
                [
                    title: "💾 Quick Commit",
                    description: "**${actionName}**",
                    color: embedColor,
                    fields: [
                        [
                            name: "App",
                            value: appName,
                            inline: true
                        ],
                        [
                            name: "Status",
                            value: status,
                            inline: true
                        ],
                        [
                            name: "Message",
                            value: message,
                            inline: false
                        ]
                    ],
                    footer: [
                        text: "Branch: ${GITHUB_BRANCH}"
                    ],
                    timestamp: isoTimestamp
                ]
            ]
        ]
        
        def connection = new URL(DISCORD_WEBHOOK).openConnection()
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("User-Agent", "BuraCloud-GitHub-Sync")
        
        connection.outputStream.withWriter("UTF-8") { writer ->
            writer.write(groovy.json.JsonOutput.toJson(discordPayload))
        }
        
        def responseCode = connection.responseCode
        if (responseCode in [200, 204]) {
            log("Discord notification sent")
        }
        
    } catch (Exception e) {
        log("Discord notification failed: ${e.message}")
    }
}

def commitAction = {
    v.result.clear()
    v.result.labelCustom("=== GitHub Quick Commit ===", [style: "h3", color: "blue"])
    v.result.labelCustom("", [])
    
    try {
        v.result.labelCustom("📦 App: ${APP_NAME}", [color: "blue", style: "bold"])
        v.result.labelCustom("🎬 Action: ${ACTION_NAME}", [color: "blue", style: "bold"])
        v.result.labelCustom("", [])
        
        // KONSTRUIŠI PATH
        def actionPath = "/Configuration/Apps/${APP_NAME}/ActionDefinitions/${ACTION_NAME}"
        def scriptPath = "${actionPath}/Scripts/action.groovy"
        
        log("Action path: ${actionPath}")
        log("Script path: ${scriptPath}")
        
        // DOHVATI CONTENT
        def content = bo.getBinaryAsString(scriptPath)
        
        if (!content || content.trim().isEmpty()) {
            v.result.labelCustom("❌ Script not found or empty", [color: "red"])
            v.result.labelCustom("Path: ${scriptPath}", [color: "red"])
            return
        }
        
        log("Script size: ${content.length()} chars")
        v.result.labelCustom("📄 Script size: ${content.length()} chars", [color: "gray"])
        v.result.labelCustom("", [])
        
        // GITHUB PATH
        def githubPath = "${APP_NAME}/ActionDefinitions/${ACTION_NAME}/action.groovy"
        log("GitHub path: ${githubPath}")
        
        v.result.labelCustom("📤 Uploading to GitHub...", [color: "blue"])
        
        // UPLOAD
        def result = uploadToGitHub(githubPath, content, "Update ${ACTION_NAME}")
        
        v.result.labelCustom("", [])
        
        if (result.status == "unchanged") {
            v.result.labelCustom("⏭️ No changes detected", [color: "blue", style: "bold"])
            v.result.labelCustom("Script is identical to GitHub version", [color: "blue"])
            sendToDiscord(ACTION_NAME, APP_NAME, "unchanged", "No changes detected")
        } else if (result.code in [200, 201]) {
            def statusIcon = result.code == 201 ? "✨ Created" : "✅ Updated"
            v.result.labelCustom("${statusIcon}", [color: "green", style: "bold"])
            v.result.labelCustom("GitHub: ${githubPath}", [color: "green"])
            v.result.labelCustom("Branch: ${GITHUB_BRANCH}", [color: "green"])
            
            def statusMsg = result.code == 201 ? "created" : "updated"
            sendToDiscord(ACTION_NAME, APP_NAME, statusMsg, "Successfully pushed to GitHub")
        } else {
            v.result.labelCustom("❌ Upload failed - HTTP ${result.code}", [color: "red", style: "bold"])
            sendToDiscord(ACTION_NAME, APP_NAME, "error", "Upload failed: HTTP ${result.code}")
        }
        
        def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
        v.result.labelCustom("", [])
        v.result.labelCustom("🕐 ${timestamp}", [color: "gray"])
        
    } catch (Exception e) {
        v.result.labelCustom("❌ ERROR: ${e.message}", [color: "red", style: "bold"])
        log("Exception: ${e.message}")
        e.printStackTrace()
        
        sendToDiscord(ACTION_NAME, APP_NAME, "error", e.message)
    }
}

// AUTO-RUN
commitAction()

v.addActionBar("ab1")
v.ab1.right.primarybutton("Commit", commitAction)
v.ab1.right.button("Clear", { v.result.clear() })
