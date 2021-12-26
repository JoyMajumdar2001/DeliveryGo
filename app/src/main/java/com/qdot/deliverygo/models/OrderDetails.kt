package com.qdot.deliverygo.models

data class OrderDetails(
    val Trackid : String,
    val Appname : String,
    val Ploc : String,
    val Recname : String,
    val Recmob : String,
    val Recmail : String,
    val Destloc : String,
    val Partner : String,
    val Partnerid : String,
    val Shipdate : Long,
    val Shiptime : String,
    val Pweight : String,
    val Pprice : String,
    val Delivered : Boolean
)
