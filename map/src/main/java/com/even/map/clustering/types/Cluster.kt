package com.even.map.clustering.types

import com.even.map.types.Location


data class Cluster(
    val ids: List<String>,
    val centroid: Location,
    val size: Int,
)
