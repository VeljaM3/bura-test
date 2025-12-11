import com.vaadin.server.FileResource
import com.vaadin.ui.Image
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.data.ValueProvider;
import com.vaadin.ui.*

m.searchNode = bo.instance("VT_FILM_SEARCH_NODE")

initLayout()

def initLayout(){
    v.caption("Image Film List")

    v.searchPage("page")
    v.page.mainTitleText = "Lista filmova sa slikom"
    v.page.searchNode = m.searchNode
    v.page.quickSearchPropertyList = "title"
    v.page.searchTablePropList = "image, title, genre, rating, datePublished"
    v.page.quickSearchClosure = simpleSearch
    v.page.rowsPerPage = 5
    v.page.searchFormCollapsed = true
    v.page.hideDownloadExcelButton = false
    
    v.page.masterBar.left.link(null, refresh, [style: "h1", color: "blue-3", iconId: Icons.REFRESH])
    v.page.emptyListMessage = "Lista je prazna"
    
    v.page.addGeneratedColumn("genre", genreCustomColumn)
    v.page.addGeneratedColumn("rating", ratingCustomColumn)
    v.page.addGeneratedColumn("image", imageCustomColumn)
    v.page.addGeneratedColumn("title", viewFilmByTitle)
    
    v.page.firstColumnClickable	= true
    
    v.page.build()
    v.page.executeQuickSearch()
}

@Field def genreCustomColumn = {node, cv ->
    cv.labelCustom(node.genre.boDescription)
}

@Field def ratingCustomColumn = { node, cv ->
    if (node.rating == null){
        cv.labelCustom("/")
    }else{
        cv.labelCustom(node.rating.boDescription)
    }
}

@Field def imageCustomColumn = {node, cv ->
    // Pristup Grid komponenti i podešavanje dimenzija
    def verticalLayoutIterator = v.page.getComponent().iterator().next().iterator()
    verticalLayoutIterator.next()
    def verticalLayout = verticalLayoutIterator.next()
    def grid = verticalLayout.iterator().next().iterator().next().iterator().next()
    
    grid.setBodyRowHeight(100)
    
    def columnObj = grid.getColumn("image")
    columnObj.setWidth(200)
    
    if (node.image.subNodes.size()>0){
        def img = node.image.subNodes[0]
        def image = getImage(img.path)
        cv.getComponent().addComponent(image)
    } else {
        def label = new com.vaadin.ui.Label()
        label.setValue("Nema")
        cv.getComponent().addComponent(label)
    }
}

@Field def viewFilmByTitle = { node, cv ->
    cv.link(node.title) { 
        action(node, "veljkotest.view.film.action").setExecuteOnClose(refresh).build()
    }
}

v.buttonBar.right.button("Zatvori", { v.close() })

@Field def refresh = {
    v.page.executeQuickSearch()
}

@Field def simpleSearch = { searchNode, pageNumber, rowsPerPage ->
    def filmList = getFilms(searchNode, pageNumber, rowsPerPage)
    filmList = services.B4C_COMMON.B4C_service.stringToRef(filmList, "genre, rating")
    return filmList
}

def getFilms(searchNode, pageNumber, rowsPerPage){
    def query = bo.find{
        if(searchNode.id) eq("id", searchNode.title)
        if(searchNode.title) like("title", searchNode.title)
        if(searchNode.genre) eq("genre", searchNode.genre.path)
        if(searchNode.rating) eq("rating", searchNode.rating.path)
        if(searchNode.datePublishedFrom) gte("datePublished", searchNode.datePublishedFrom)
        if(searchNode.datePublishedTo) lte("datePublishedTo", searchNode.datePublishedTo)
        if(searchNode.dateWatchedFrom) lte("dateWatchedFrom", searchNode.dateWatchedFrom)
        if(searchNode.dateWatchedTo) lte("dateWatchedTo", searchNode.dateWatchedTo)
    }
    query.ofType("VT_FILM")
    query.limit(rowsPerPage)
    query.offset(pageNumber -1)
    return query.execute()
}

def getImage(def imageNode){
    FileResource resource = new FileResource(repo.getFile(imageNode))
    
    Image image = new Image(null, resource)
    image.setHeight("100px")
    image.setWidth("50px")
    return image
}