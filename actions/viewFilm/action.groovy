import com.vaadin.server.FileResource
import com.vaadin.ui.Image

initData()
initLayout()

v.label(appConfig.VeljkoTest.message)

def initData(){
    m.film = bo.get(n.path)
    m.subGenre = bo.instance("VT_SUBGENRE")
    m.tag = bo.instance("VT_TAG")
}

def initLayout(){
    v.setMargin(true)
    v.caption("Pregled filma")
    v.pageLayout('page', 'task-edit')
    v.page.build()
    showHeader()
    v.page.body.addHSectionSplit("s1")
    v.s1.setSplitPosition(60)
    showForm()
    //Dodajemo sekcije noteSection, noteEditSection i noteListSectionu telu stranice
    v.s1.left.addSection("noteSection")
    v.s1.left.addSection("noteListSection")
    //Setujemo širinu novih sekcija
    v.noteSection.setWidth("400px")
    v.noteListSection.setWidth("400px")
    //Inicijalizuj sekcije
    initNoteSection()
    initNoteListSection()
    //Dodajemo novu sekciju imageSection
    v.s1.right.addSection("imageSection")
    initImageSection()
    //SubGenre sekcija
    v.s1.right.addSection("subGenreSection")
    initSubGenreSection()
    //Tag sekcija
    v.s1.right.addSection("tagSection")
    initTagSection()
    
    showFooter()
}

def showHeader(){
    v.page.header.masterTitle.labelCustom("Pregled filma", [style: 'h1', color: "red"])
    v.page.header.build()
}

def showForm(){
    v.s1.left.showNode(m.film, [id: 'mainForm'])
    v.mainForm.setPropertyList("title, genre, datePublished, dateWatched, actors, rating")
    v.mainForm.setViewMode(2)
    v.mainForm.build()
}

//Notes sekcija

def initNoteSection(){
    def note = bo.instance("VT_NOTE")
    v.noteForm.build()
}

def initNoteListSection(){
    v.noteListSection.setMargin(true)
    v.noteListSection.labelCustom("Zapažanja", [style: 'h2'])
    def noteList = v.noteListSection.showList(m.film.notes.subNodes)
    noteList.setPropertyList("date, text")
    
    noteList.build()
}

//Image sekcija

def initImageSection(){
    v.imageSection.setMargin(true)
    v.imageSection.labelCustom("Poster", [style: 'h2'])
    
    if(m.film.image.subNodes.size() == 0){return}
    def image = m.film.image.subNodes[0]
    
    FileResource resource = new FileResource(repo.getFile(image.path))
    
    Image imageComponent = new Image(null, resource)
    imageComponent.setWidth("300px")
    v.imageSection.addComponent(imageComponent)
}


//SubGenre sekcija

def initSubGenreSection(){
    v.subGenreSection.setMargin(true)
    v.subGenreSection.labelCustom("SubGenre", [style: 'h2'])
    v.subGenreSection.addSection("subGenreTableSection")
    subGenreTableSection()
}


def subGenreTableSection(){
    if (m.film.subGenres.subNodes.size() > 0){
        def subGenreTable = v.subGenreTableSection.showList(m.film.subGenres.subNodes)
        subGenreTable.setPropertyList("subGenreDescription")
    
        subGenreTable.build()
    }
}

//Tag sekcija
def initTagSection(){
    v.tagSection.setMargin(true)
    v.tagSection.labelCustom("Tag", [style: 'h2'])
    v.tagSection.addSection("tagTableSection")
    tagTableSection()
}


def tagTableSection(){
    if (m.film.tags.subNodes.size() > 0){
        def tagTable = v.tagTableSection.showList(m.film.tags.subNodes)
        tagTable.setPropertyList("tagDescription")
    
        tagTable.build()
    }
}

def showFooter(){
    v.page.footerActions.right.button("Sacuvaj", {validateAndSave() }, [iconId: Icons.BOOKMARK_O])
    v.page.footerActions.left.link("Odustani", { v.close() }, [style: 'red'])
}