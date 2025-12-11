initData()
initLayout()

def initData(){
    m.searchNode = bo.instance("VT_FILM_SEARCH_NODE")
    m.out = [:]
}

def initLayout(){
    v.caption("Lista filmova")
    v.setMargin(false)
    
    v.searchPage("page")
    v.page.mainTitleText = "Lista filmova"
    v.page.searchNode = m.searchNode
    v.page.quickSearchPropertyList = "title"
    v.page.searchTablePropList = "id, title, datePublished, dateWatched, rating, genre"
    v.page.quickSearchClosure = simpleSearch
    v.page.advancedSearchClosure = advancedSearch
    v.page.advancedSearchPropertyList = "id, title, datePublishedFrom, datePublishedTo, dateWatchedFrom, dateWatchedTo, genre, rating, state"
    v.page.rowsPerPage = appConfig.VeljkoTest.rowCount
    v.page.searchFormCollapsed = true
    v.page.hideDownloadExcelButton = false
    v.page.masterBar.left.link(null, refresh, [style:"h1", color:"blue-3", iconId: Icons.REFRESH])
    v.page.emptyListMessage = "Lista je prazna"
    
    v.page.addGeneratedColumn("genre", genreCustomColumn)
    v.page.addGeneratedColumn("rating", ratingCustomColumn)
    
    
    v.page.onClick = { node -> 
        action(node, "veljkotest.view.film.action").setExecuteOnClose(refresh).build()
    }
    //v.page.firstColumnClickable	= false
    
    v.page.excelFilePropList = "id, title, datePublished, dateWatched, rating, genre"
    v.page.excelFileName = "Film.xlsx"
    
    v.page.onActionBarClick = {node, popup ->
        popup.setSpacing(true)
        popup.link("Izmeni", {
            action(node,"veljkotest.edit.film.action").setExecuteOnClose(refresh).build()
            popup.close()
        })
        if (node.state == true) {
            popup.link("Izbriši", {
            onDelete(node)
            popup.close()
        })
        }
        if (node.state == false) {
            popup.link("Aktiviraj", {
            onActivate(node)
            popup.close()
        })
        }
        if (node.dateWatched == null) {
            popup.link("Pogledaj", {
            action(node,"veljkotest.watch.film.action").setExecuteOnClose(refresh).build()
            popup.close()
        })
        }
        if (node.dateWatched) {
            popup.link("Poništi gledanje", {
            removeWatched(node)
            popup.close()
        })
        }
        popup.link("Dodaj belešku", {
            action(node,"veljkotest.add.film.note.action").showInPopup().setExecuteOnClose(refresh).build()
            popup.close()
        })
    }
    
    v.page.header.bottomBar.addSection("listFilm")
    v.listFilm.right.primarybutton('Dodaj novi film', { action(null, 'veljkotest.add.film.action').setExecuteOnClose(refresh).build() }, [iconId: Icons.PLUS])
    v.listFilm.setMargin(true, false, true, false)
    
    v.page.build()
    v.page.header.addStyle("blue")
    v.page.executeQuickSearch()
}

@Field def genreCustomColumn = { node, cv ->
    cv.labelCustom(node.genre.boDescription)
}

@Field def ratingCustomColumn = { node, cv ->
    if (node.rating == null){
        cv.labelCustom("/")
    }else{
        cv.labelCustom(node.rating.boDescription)
    }
}

@Field def refresh = {
    v.page.executeQuickSearch()
}

@Field def simpleSearch = { searchNode, pageNumber, rowsPerPage ->
    def filmList = getFilms(searchNode, pageNumber, rowsPerPage)
    filmList = services.B4C_COMMON.B4C_service.stringToRef(filmList, "genre, rating")
    return filmList
}

@Field def advancedSearch = { searchNode, pageNumber, rowsPerPage ->
    def filmList = getFilms(searchNode, pageNumber, rowsPerPage)
    filmList = services.B4C_COMMON.B4C_service.stringToRef(filmList, "genre, rating")
    return filmList
}

def getFilms(searchNode, pageNumber, rowsPerPage){
    def query = bo.find{
        if(searchNode.id) eq("id", searchNode.id)
        if(searchNode.title) like("title", searchNode.title)
        if(searchNode.genre) eq("genre", searchNode.genre.path)
        if(searchNode.rating) eq("rating", searchNode.rating.path)
        if(searchNode.datePublishedFrom) gte("datePublished", searchNode.datePublishedFrom)
        if(searchNode.datePublishedTo) lte("datePublishedTo", searchNode.datePublishedTo)
        if(searchNode.dateWatchedFrom) lte("dateWatchedFrom", searchNode.dateWatchedFrom)
        if(searchNode.dateWatchedTo) lte("dateWatchedTo", searchNode.dateWatchedTo)
        if(searchNode.state)
            {eq("state", false)}
        else {
            eq("state", true)
        }
    }
    
    query.ofType("VT_FILM")
    query.limit(rowsPerPage)
    query.offset(pageNumber - 1)
    return query.execute()
}

@Field def onDelete = { node ->
    action(null, 'veljkotest.generic.popup.message.action').showInPopup().addOutParam(m.out, 'agrees').addParam("displayMessage", "Da li ste sigurni da želite da obrišete ovaj film").setExecuteOnClose({
        if(m.out.agrees){
            node.state = false
            r.update(node)
            refresh()
        }
    }).build()
}

@Field def onActivate = { node ->
    action(null, 'veljkotest.generic.popup.message.action').showInPopup().addOutParam(m.out, 'agrees').addParam("displayMessage", "Da li ste sigurni da želite da aktivirate ovaj film").setExecuteOnClose({
        if(m.out.agrees){
            node.state = true
            r.update(node)
            refresh()
        }
    }).build()
}

@Field def removeWatched = { node ->
    action(null, 'veljkotest.generic.popup.message.action').showInPopup().addOutParam(m.out, 'agrees').addParam("displayMessage", "Da li ste sigurni da želite da obrišete status gledanja").setExecuteOnClose({
        if(m.out.agrees){
            node.dateWatched = null
            node.rating = null
            r.update(node)
            refresh()
        }
    }).build()
}