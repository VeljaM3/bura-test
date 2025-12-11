m.film = bo.get(n.path)

v.addSectionS("watchFilmSection")


v.watchFilmSection.showNode(m.film, [id: 'watchFilmForm'])
v.watchFilmForm.setPropertyList("dateWatched, rating")
v.watchFilmForm.setEditMode(1)
v.watchFilmForm.footerBar.right.primarybutton("Sačuvaj", {
    validateAndSave()
})
v.watchFilmForm.footerBar.left.link("Odustani", {
    v.close()
}, [style: "red"])
v.watchFilmForm.build()

def validateAndSave(){
    def errors = []
    
    if(!m.film.dateWatched) errors << "dateWatched greska"
    if(!m.film.rating) errors << "Rating greska"
    
    if(errors){
        warningPopup(errors.join("\n"))
    }
    else save()
}

def save(){
    bo.update(m.film)
    v.close()
}