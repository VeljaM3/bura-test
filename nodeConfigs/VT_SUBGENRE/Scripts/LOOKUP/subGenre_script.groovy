def cacheService = services.VeljkoTest.cache.getServiceObject(this)

if (node.selectedGenre == null){return null} 
def subGenres = cacheService.subGenres()

subGenres = subGenres.findAll{
    it.genre == node.selectedGenre.path
}

if(lookup){
    subGenres = subGenres.findAll{
        it.boDescription.toLowerCase().contains(lookup.toLowerCase())
    }
}

return subGenres