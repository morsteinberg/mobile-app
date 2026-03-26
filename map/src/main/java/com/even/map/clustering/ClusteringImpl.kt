package com.even.map.clustering

import android.location.Location.distanceBetween
import com.even.core.logger.Logger
import com.even.map.clustering.types.Cluster
import com.even.map.types.Element.Point
import com.even.map.types.Location
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage.CompleteLinkage

class ClusteringImpl(private val logger: Logger, points: List<Point>) : Clustering {
    private var dendrogram: HierarchicalClustering
    private var points: List<Point>

    init {
        this.points = points
        dendrogram = createDendrogram()
    }

    /**
     * A Tree where the Root is all the points combined.
     * It branches out into smaller and smaller groups, where the last groups is all the points alone.
     */
    private fun createDendrogram(): HierarchicalClustering {
        logger.d("Creating Dendrogram")
        val distanceMatrix = calcDistanceMatrix(points)

        val linkage = CompleteLinkage(distanceMatrix)
        val dendrogram = HierarchicalClustering.fit(linkage)


        return dendrogram
    }

    override fun reCalculateDendrogram(points: List<Point>) {
        logger.d("Recalculating Dendrogram")
        this.points = points
        dendrogram = createDendrogram()
    }

    /**
     * Creates cluster for specific ZoomLevel
     * This can only be called after calculating the Dendrogram.
     */
    override fun clusterForAltitude(altitude: Int): List<Cluster> {
        val threshold = getGroupingThreshold(altitude)
        val clusterIds = dendrogram.partition(threshold)

        logger.d("Getting cluster for altitude: $altitude which is threshold: $threshold")

        val maxClusterNumber = clusterIds.maxOrNull() ?: 0
        val clusters: List<MutableList<Point>> = List(maxClusterNumber) { mutableListOf() }

        clusterIds.zip(points).forEach { (clusterId, point) ->
            clusters[clusterId].add(point)
        }

        return clusters.map { cluster ->
            val ids = cluster.map { it.id }
            val centroid = calculateCentroid(cluster)
            Cluster(
                ids = ids,
                centroid = Location(centroid.first, centroid.second),
                size = cluster.size,
            )
        }
    }

    /**
     *  Centroid is the center point location of the cluster.
     *  Easily explained, it is the average center of all points.
     */
    private fun calculateCentroid(cluster: List<Point>): Pair<Double, Double> {
        val totalLat = cluster.sumOf { it.location.latitude }
        val totalLng = cluster.sumOf { it.location.longitude }
        val size = cluster.size
        return Pair(totalLat / size, totalLng / size)
    }


    /**
     * Distance Matrix is a 2D array where each element (j, k) represents the distance between two point (j, K)
     *  Example:          A    B    C
     *                A   0    2    3
     *                B   2    0    5
     *                C   3    5    0
     */
    private fun calcDistanceMatrix(dendrogramData: List<Point>): Array<DoubleArray> {
        val distanceMatrix = Array(dendrogramData.size) { DoubleArray(dendrogramData.size) }
        for (i in dendrogramData.indices) {
            for (j in i + 1 until dendrogramData.size) {
                dendrogramData[i]
                val results = floatArrayOf(0f)
                distanceBetween(
                    dendrogramData[i].location.latitude,
                    dendrogramData[i].location.longitude,
                    dendrogramData[j].location.latitude,
                    dendrogramData[j].location.longitude,
                    results,
                )

                distanceMatrix[i][j] = results[0].toDouble()
                distanceMatrix[j][i] = results[0].toDouble()
            }
        }

        return distanceMatrix
    }

    /**
     * The altitude is the current camera position altitude of the user.
     * The return value represents the radius where points in the map will be clustered.
     */
    private fun getGroupingThreshold(altitude: Int): Double {
        return when (altitude) {
            in 0..70 -> 0.0
            in 70..200 -> 1.0
            in 200..500 -> 8.0
            in 500..750 -> 15.0
            in 750..1000 -> 22.0
            in 1000..1500 -> 30.0
            in 1500..2000 -> 45.0
            in 2000..3000 -> 100.0
            in 3000..4000 -> 200.0
            in 4000..5500 -> 300.0
            else -> 345.0
        }
    }

}
