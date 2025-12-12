//return bo.find{
//    if(lookup) like('boDescription', lookup)
//}.ofType('CODE_BOOK_ITEM').property("businessType","vt.businesstypes.rating").limit(9999).execute()

def cacheService = services.VeljkoTest.cache.getServiceObject(this)

def ratings = cacheService.ratings()

if(lookup){
    ratings = ratings.findAll{
        it.boDescription.toLowerCase().contains(lookup.toLowerCase())
    }
}

return ratings