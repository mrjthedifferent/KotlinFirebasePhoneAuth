package mrj.info.bd.kotlinfirebasephoneauth.repositories

import mrj.info.bd.kotlinfirebasephoneauth.retrofit.RetrofitService

class MainRepository constructor(private val retrofitService: RetrofitService) {

    fun getAllMovies() = retrofitService.getAllMovies()
}