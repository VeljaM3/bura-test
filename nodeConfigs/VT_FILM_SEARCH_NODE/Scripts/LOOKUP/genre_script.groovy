//return bo.find{
//    if(lookup) like('boDescription', lookup)
//}.ofType('CODE_BOOK_ITEM').property("businessType","vt.businesstypes.genre").limit(9999).execute()

def cacheService = services.VeljkoTest.cache.getServiceObject(this)

def genres = cacheService.genres()

if(lookup){
    genres = genres.findAll{
        it.boDescription.toLowerCase().contains(lookup.toLowerCase())
    }
}

return genres