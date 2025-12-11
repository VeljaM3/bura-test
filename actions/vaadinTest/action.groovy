//Provera verzije Vaadin
//def customLogic() {
//    v.label("Click")
//}
//
//try {
//    def pkg = com.vaadin.server.VaadinServlet.class.getPackage()
//    log("Package Version: " + pkg.getImplementationVersion())
//} catch (Throwable e) {
//    log("Ne mogu da očitam verziju iz paketa.")
//}
//showLogConsole()
//***************************************************

showLogConsole()
log("Pokrećem prikaz mape...")

// Importujemo ContentMode (ako je potrebno - platforme često imaju ovo globalno dostupno)
import com.vaadin.shared.ui.ContentMode

// 1. Kreiranje sekcije
v.addSectionA("mapSection", [type:"dash2", style:"blue"])

// 2. Inicijalizacija Headera
v.mapSection.header() 
v.mapSection.header.left.icon(Icons.GLOBE, [color: "blue", style:"h2"])
v.mapSection.header.topBar.labelCustom("Interaktivna mapa sveta ❌", [style:"h3,bold", color:"blue"])
v.mapSection.header.build()

// 3. HTML za mapu (OpenStreetMap embed)
String mapHtml = """
<iframe 
  width="100%" 
  height="800" 
  frameborder="0" 
  scrolling="no" 
  marginheight="0" 
  marginwidth="0" 
  src="https://www.openstreetmap.org/export/embed.html?bbox=19.0,42.0,23.0,46.0&amp;layer=mapnik" 
  style="border: 1px solid #ccc">
</iframe>
<br/>
<small><a href="https://www.openstreetmap.org/#map=7/44.000/21.000" target="_blank">Prikaži veću mapu</a></small>
"""

// 4. Kreiranje Vaadin Label komponente sa HTML ContentMode
// NAPOMENA: Bura Cloud možda nema direktan DSL za ovo, pa koristimo čist Vaadin 8 kod
import com.vaadin.ui.Label

def htmlLabel = new Label(mapHtml, ContentMode.HTML)
htmlLabel.setWidth("100%")

// 5. Dodavanje labela u sekciju kroz "getSectionLayout()" wrapper
// (ovo je metoda koja omogućava pristup internom Vaadin layoutu)
v.mapSection.addComponent(htmlLabel)

// Alternativa: Dugme koje otvara mapu u novom tabu
def openMapAction = {
    // JavaScript za otvaranje URL-a u novom tabu
    // U Vaadin 8 koristimo BrowserWindowOpener ili JavaScript
    com.vaadin.server.Page.getCurrent().open("https://www.openstreetmap.org/#map=7/44.000/21.000", "_blank")
}

v.mapSection.button("Otvori mapu u celom ekranu", openMapAction, [iconId: Icons.EXTERNAL_LINK])

log("Interfejs sa mapom je iscrtan.")



