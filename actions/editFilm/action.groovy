initData()
initLayout()

def initData(){
    m.film = bo.get(n.path)
    m.subGenre = bo.instance("VT_SUBGENRE")
    m.tag = bo.instance("VT_TAG")
}

def initLayout(){
    v.setMargin(true)
    v.caption("Izmeni film")
    v.pageLayout('page', 'task-edit')
    v.page.build()
    showHeader()
    v.page.body.addHSectionSplit("s1")
    v.s1.setSplitPosition(60)
    showForm()
    //Dodajemo sekcije noteSection, noteEditSection i noteListSectionu telu stranice
    v.s1.left.addSection("noteSection")
    v.s1.left.addSection("noteEditSection")
    v.s1.left.addSection("noteListSection")
    //Setujemo širinu novih sekcija
    v.noteSection.setWidth("400px")
    v.noteEditSection.setWidth("400px")
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
    
    if (m.film.genre){
        m.tag = bo.instance("VT_TAG")
        m.tag.selectedGenre = m.film.genre
        tagFormSection()
        tagTableSection()

        m.subGenre = bo.instance("VT_SUBGENRE")
        m.subGenre.selectedGenre = m.film.genre
        subGenreFormSection()
        subGenreTableSection()
    }
    showFooter()
}

def showHeader(){
    v.page.header.masterTitle.labelCustom("Dodaj film", [style: 'h1', color: "red"])
    v.page.header.build()
}

def showForm(){
    v.s1.left.showNode(m.film, [id: 'mainForm'])
    v.mainForm.setPropertyList("title, genre, datePublished, dateWatched, actors, rating")
    v.mainForm.setEditMode(2)
    v.mainForm.build()
    
    v.mainForm.genre.onChange = {
        m.film.tags.subNodes.each{
            m.film.tags.removeSubnode(it)
        }
        m.tag = bo.instance("VT_TAG")
        m.tag.selectedGenre = m.film.genre
        tagFormSection()
        tagTableSection()
        m.film.subGenres.subNodes.each{
            m.film.subGenres.removeSubnode(it)
        }
        m.subGenre = bo.instance("VT_SUBGENRE")
        m.subGenre.selectedGenre = m.film.genre
        subGenreFormSection()
        subGenreTableSection()
    }
}

//Definišemo sekciju i konfigurišemo njen sadržaj
def initNoteSection(){
    v.noteSection.clear()
    v.noteEditSection.clear()
    def note = bo.instance("VT_NOTE")
    v.noteSection.showNode(note, [id: 'noteForm'])
    v.noteForm.setPropertyList("date, text")
    v.noteForm.setEditMode(1)
    v.noteForm.footerBar.right.primarybutton("Dodaj", {
        m.film.notes.addSubNode(note)
        initNoteSection()
        initNoteListSection()
    })
    v.noteForm.build()
}

//Definišemo sekciju i konfigurišemo njen sadržaj
def initNoteListSection(){

    v.noteListSection.clear()
    def noteList = v.noteListSection.showList(m.film.notes.subNodes)
    noteList.setPropertyList("date, text")
    noteList.addActionBarButton(Icons.EDIT, onNoteEdit)
    noteList.addActionBarButton(Icons.TRASH_O, onNoteDelete)
    
    noteList.build()
}

def initImageSection(){
    v.imageSection.setMargin(true)
    v.imageSection.labelCustom("Poster", [style: 'h2'])
    v.imageSection.addSection("imageUploadSection")
    v.imageSection.addSection("imageTableSection")
    
    imageUploadSection()
}


def imageUploadSection(){
    v.imageUploadSection.clear()
    if (m.film.image.subNodes.size() == 0){
        v.imageUploadSection.upload2(onUpload).build()
    }

}

def imageTableSection(){
    
    v.imageTableSection.clear()
    if (m.film.image.subNodes.size() != 0){
        def imageTable = v.imageTableSection.showList(m.film.image.subNodes)
        imageTable.setPropertyList("fileName")
        imageTable.addActionBarButton(Icons.TRASH_O, onImageDelete)
    
        imageTable.build()
    }
}

//SubGenre sekcija

def initSubGenreSection(){
    v.subGenreSection.setMargin(true)
    v.subGenreSection.labelCustom("SubGenre", [style: 'h2'])
    v.subGenreSection.addSection("subGenreFormSection")
    v.subGenreSection.addSection("subGenreTableSection")
    subGenreFormSection()
    subGenreTableSection()
}

def subGenreFormSection(){
    v.subGenreFormSection.clear()
    v.subGenreFormSection.showNode(m.subGenre, [id: 'subGenreForm'])
    v.subGenreForm.setPropertyList("subGenre")
    v.subGenreForm.setEditMode(1)
    v.subGenreForm.footerBar.right.primarybutton("Dodaj", {
        m.subGenre.subGenreDescription = m.subGenre.subGenre.boDescription
        m.film.subGenres.addSubNode(m.subGenre)
        m.subGenre = bo.instance("VT_SUBGENRE")
        m.subGenre.selectedGenre = m.film.genre
        
        subGenreFormSection()
        subGenreTableSection()
    })
    v.subGenreForm.build()
}

def subGenreTableSection(){
    v.subGenreTableSection.clear()
    if (m.film.subGenres.subNodes.size() > 0){
        def subGenreTable = v.subGenreTableSection.showList(m.film.subGenres.subNodes)
        subGenreTable.setPropertyList("subGenreDescription")
        subGenreTable.addActionBarButton(Icons.TRASH_O, onSubGenreDelete)
    
        subGenreTable.build()
    }
}

//Tag sekcija
def initTagSection(){
    v.tagSection.setMargin(true)
    v.tagSection.labelCustom("Tag", [style: 'h2'])
    v.tagSection.addSection("tagFormSection")
    v.tagSection.addSection("tagTableSection")
    tagFormSection()
    tagTableSection()
}

def tagFormSection(){
    v.tagFormSection.clear()
    v.tagFormSection.showNode(m.tag, [id: 'tagForm'])
    v.tagForm.setPropertyList("tag")
    v.tagForm.setEditMode(1)
    v.tagForm.footerBar.right.primarybutton("Dodaj", {
        m.tag.tagDescription = m.tag.tag.boDescription
        m.film.tags.addSubNode(m.tag)
        m.tag = bo.instance("VT_TAG")
        m.tag.selectedGenre = m.film.genre
        
        tagFormSection()
        tagTableSection()
    })
    v.tagForm.build()
}

def tagTableSection(){
    v.tagTableSection.clear()

    if (m.film.tags.subNodes.size() > 0){
        def tagTable = v.tagTableSection.showList(m.film.tags.subNodes)
        tagTable.setPropertyList("tagDescription")
        tagTable.addActionBarButton(Icons.TRASH_O, onTagDelete)
    
        tagTable.build()
    }
    
}

@Field
def	onUpload = { fileInfo ->		
    def	file =	r.instance("FILE")	
        file.name = fileInfo.fileName	
        file.binaryReference = fileInfo.binaryReference	
        file.fileName = fileInfo.fileName	
        file.size =	fileInfo.fileSize	
        file.mimeType = fileInfo.fileType
        m.film.image.addSubNode(file)
        imageTableSection()
        imageUploadSection()
}	

def validateAndSave(){
    def errors = []
    
    if(!m.film.title) errors << "Title greska"
    if(!m.film.genre) errors << "Genre greska"
    if(!m.film.datePublished) errors << "datePublished greska"
    if(!m.film.actors) errors << "Actors greska"
    
    if(errors){
        warningPopup(errors.join("\n"))
    }
    else save()
}

def save(){
    def result = bo.update(m.film)
    if(result != null && !result.isHasError()){
        infoPopup("Film je dodat uspesno!")
        v.close()
    }
    else{
        errorPopup("Greska")
    }
}

def showFooter(){
    v.page.footerActions.right.button("Sacuvaj", {validateAndSave() }, [iconId: Icons.BOOKMARK_O])
    v.page.footerActions.left.link("Odustani", { v.close() }, [style: 'red'])
}

@Field
def onNoteEdit = {note ->
    v.noteSection.clear()
    v.noteEditSection.clear()
    
    v.noteEditSection.showNode(note, [id: 'noteEditForm'])
    v.noteEditForm.setPropertyList("date, text")
    v.noteEditForm.setEditMode(1)
    v.noteEditForm.footerBar.right.button("Promeni", {
        //m.film.notes.addSubNode(note)
        initNoteSection()
        initNoteListSection()
    })
    v.noteEditForm.build()
}

@Field
def onNoteDelete = {note ->
    m.film.notes.removeSubnode(note)
    initNoteListSection()
}

@Field
def onImageDelete = {image ->
    m.film.image.removeSubnode(image)
    imageTableSection()
    imageUploadSection()
}

@Field
def onTagDelete = {tag ->
    m.film.tags.removeSubnode(tag)
    tagTableSection()
    tagFormSection()
}

@Field
def onSubGenreDelete = {subGenre ->
    m.film.subGenres.removeSubnode(subGenre)
    subGenreTableSection()
    subGenreFormSection()
}