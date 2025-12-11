m.film = bo.get(n.path)
m.note = bo.instance("VT_NOTE")

v.addSectionS("addFilmNoteSection")


v.addFilmNoteSection.showNode(m.note, [id: 'addFilmNoteForm'])
v.addFilmNoteForm.setPropertyList("text, date")
v.addFilmNoteForm.setEditMode(1)
v.addFilmNoteForm.footerBar.right.primarybutton("Sačuvaj", {
    validateAndSave()
})
v.addFilmNoteForm.footerBar.left.link("Odustani", {
    v.close()
}, [style: "red"])
v.addFilmNoteForm.build()

def validateAndSave(){
    def errors = []
    
    if(!m.note.date) errors << "date greska"
    if(!m.note.text) errors << "text greska"
    
    if(errors){
        warningPopup(errors.join("\n"))
    }
    else save()
}

def save(){
    m.film.notes.addSubNode(m.note)
    bo.update(m.film)
    v.close()
}