// Logovanje za debug
showLogConsole()

// Postavke za GitHub
def GITHUB_TOKEN = appConfig.B4C_COMMON.gtVeljko
def GITHUB_OWNER = "VeljaM3"
def GITHUB_REPO = "bura-test"
def GITHUB_BRANCH = "main"

// Kreiranje glavnog layout-a
v.setMargin(false)

// Kreiranje header komponente
v.addHeader("h1")
v.h1.addStyle("gray")
v.h1.topTitle.labelCustom("GitHub Push Automation", [style: "h1"])
v.h1.masterTitle.labelCustom("Upload fajla na GitHub", [style: "h4", color: "gray"])
v.h1.build()

// Kreiranje sekcije za sadržaj
v.addSection("s1")
v.s1.setMargin(true)
v.s1.addStyle("gray")

// Info labela
v.s1.labelCustom("Klikni na dugme da pushujes izmene na GitHub", [style: "bold"])

// TextArea za unos sadržaja fajla
v.s1.labelCustom("Sadrzaj fajla:", [style: "h4"])
def textArea = new com.vaadin.ui.TextArea()
textArea.setRows(10)
textArea.setValue("""// Primer sadrzaja
def message = "Hello from Bura Cloud!"
log(message)""")
textArea.setWidth("100%")
v.s1.addComponent(textArea)

// Input polje za naziv fajla
v.s1.labelCustom("Naziv fajla:", [style: "h4"])
def fileNameInput = new com.vaadin.ui.TextField()
fileNameInput.setValue("test-script.groovy")
fileNameInput.setWidth("100%")
v.s1.addComponent(fileNameInput)

// Sekcija za rezultat
v.addSection("resultSection")
v.resultSection.setMargin(true)

// Closure za GitHub commit/push
def pushToGitHub = {
    try {
        // Uzimanje vrednosti iz input polja
        def fileName = fileNameInput.value
        def fileContent = textArea.value
        
        if (!fileName || fileName.trim().isEmpty()) {
            v.resultSection.clear()
            v.resultSection.labelCustom("Greska: Naziv fajla ne moze biti prazan!", [color: "red", style: "bold"])
            return
        }
        
        if (!fileContent || fileContent.trim().isEmpty()) {
            v.resultSection.clear()
            v.resultSection.labelCustom("Greska: Sadrzaj fajla ne moze biti prazan!", [color: "red", style: "bold"])
            return
        }
        
        log("Pocetak GitHub push operacije...")
        v.resultSection.clear()
        v.resultSection.labelCustom("Slanje na GitHub...", [color: "blue", style: "bold"])
        
        // GitHub API URL
        def apiUrl = "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/contents/${fileName}"
        
        // Provera da li fajl već postoji (da bi dobili SHA)
        def existingSha = null
        try {
            def getUrl = new URL(apiUrl)
            def getConnection = getUrl.openConnection()
            getConnection.setRequestMethod("GET")
            getConnection.setRequestProperty("Authorization", "token ${GITHUB_TOKEN}")
            getConnection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (getConnection.responseCode == 200) {
                def response = new groovy.json.JsonSlurper().parseText(getConnection.inputStream.text)
                existingSha = response.sha
                log("Fajl vec postoji, SHA: ${existingSha}")
            }
        } catch (Exception e) {
            log("Fajl ne postoji, krearace se novi")
        }
        
        // Enkodovanje sadržaja u Base64
        def encodedContent = fileContent.bytes.encodeBase64().toString()
        
        // Kreiranje JSON payload-a
        def payload = [
            message: "Auto-commit from Bura Cloud: ${fileName}",
            content: encodedContent,
            branch: GITHUB_BRANCH
        ]
        
        // Dodaj SHA ako fajl već postoji (za update)
        if (existingSha) {
            payload.sha = existingSha
        }
        
        def jsonPayload = groovy.json.JsonOutput.toJson(payload)
        
        // Slanje PUT zahteva
        def url = new URL(apiUrl)
        def connection = url.openConnection()
        connection.setRequestMethod("PUT")
        connection.setDoOutput(true)
        connection.setRequestProperty("Authorization", "token ${GITHUB_TOKEN}")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        
        // Slanje podataka
        connection.outputStream.withWriter { writer ->
            writer.write(jsonPayload)
        }
        
        // Provera odgovora
        def responseCode = connection.responseCode
        log("Response code: ${responseCode}")
        
        if (responseCode == 200 || responseCode == 201) {
            def response = new groovy.json.JsonSlurper().parseText(connection.inputStream.text)
            log("Uspesan push! Commit SHA: ${response.commit.sha}")
            
            v.resultSection.clear()
            v.resultSection.addStyle("green")
            v.resultSection.labelCustom("USPEH! Pushovan na GitHub!", [color: "green", style: "h3,bold"])
            v.resultSection.labelCustom("Fajl: ${fileName}", [style: "bold"])
            v.resultSection.labelCustom("Commit SHA: ${response.commit.sha}")
            v.resultSection.labelCustom("Poruka: ${response.commit.message}")
            v.resultSection.labelCustom("URL: ${response.content.html_url}", [color: "blue"])
            
        } else {
            def errorText = connection.errorStream?.text ?: "Nepoznata greska"
            log("Greska: ${errorText}")
            
            v.resultSection.clear()
            v.resultSection.addStyle("red")
            v.resultSection.labelCustom("GRESKA prilikom slanja na GitHub!", [color: "red", style: "h3,bold"])
            v.resultSection.labelCustom("Status: ${responseCode}", [color: "red"])
            v.resultSection.labelCustom("Detalji: ${errorText}", [color: "red"])
        }
        
    } catch (Exception e) {
        log("Izuzetak: ${e.message}")
        e.printStackTrace()
        
        v.resultSection.clear()
        v.resultSection.addStyle("red")
        v.resultSection.labelCustom("GRESKA: ${e.message}", [color: "red", style: "bold"])
    }
}

// Action Bar sa dugmadima
v.addActionBar("ab1")
v.ab1.right.primarybutton("Push na GitHub", pushToGitHub)
v.ab1.right.button("Ocisti", {
    textArea.setValue("")
    fileNameInput.setValue("")
    v.resultSection.clear()
})
