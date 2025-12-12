//return bo.find{
//    if(lookup) like('boDescription', lookup)
//}.ofType('CODE_BOOK_ITEM').property("businessType","vt.businesstypes.genre").limit(9999).execute()

def cacheService = services.VeljkoTest.cache.getServiceObject(this)

if (node.selectedGenre == null){return null} 
def tags = cacheService.tags()

tags = tags.findAll{
    it.genre == node.selectedGenre.path
}

if(lookup){
    tags = tags.findAll{
        it.boDescription.toLowerCase().contains(lookup.toLowerCase())
    }
}

return tags