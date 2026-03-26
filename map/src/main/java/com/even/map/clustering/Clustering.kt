package com.even.map.clustering

import com.even.map.clustering.types.Cluster
import com.even.map.types.Element.Point

interface Clustering {
    fun reCalculateDendrogram(points: List<Point>)

    fun clusterForAltitude(altitude: Int): List<Cluster>

}
