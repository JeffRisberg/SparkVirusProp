package com.incra.services

import com.incra.model.{Facility, GridCell, Particle}
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.MersenneTwister
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

/**
  * @author Jeff Risberg
  * @since 9/8/15
  */
class ProcessService extends Serializable {

  /**
    * Spin up a Spark instance, specify the timesteps and trials for the prediction.
    *
    * Return a series of GridCells.
    *
    * @param gridCells
    * @param numTimesteps
    * @param numTrials
    * @param parallelism
    * @return
    */
  def run(sc: SparkContext,
          gcBroadcast: Broadcast[scala.collection.immutable.IndexedSeq[GridCell]],
          gridCells: Seq[GridCell],
          facilities: Seq[Facility], baseLat: Double, baseLng: Double, windSpd: Double, windHdg: Double, facilitiesActive: Boolean,
          numTimesteps: Int,
          numTrials: Int,
          parallelism: Int): Array[(Option[GridCell], Int)] = {

    val baseSeed = 1001L

    println("start the trials!")
    val endpoints = computeTrialReturns(sc, facilities, baseLat, baseLng, windSpd, windHdg, facilitiesActive, numTimesteps, baseSeed, numTrials, parallelism)
    endpoints.cache()

    println("print the results")
    endpoints.foreach { endpoint => println(endpoint) }

    // Update the grid cells
    // This is done in parallel
    val endCells = endpoints.map(endpoint => {
      val gridCellOpt = gcBroadcast.value.find {
        cell => cell.containsPoint(endpoint.x, endpoint.y)
      }

      (gridCellOpt, if (endpoint.active) 1 else 0)
    })

    // This is done in parallel
    val counts = endCells.reduceByKey { case (x, y) => x + y }

    // We call collect to convert RDD to cell counts in master process
    println("computation begins here")

    counts.collect
  }

  def computeTrialReturns(sc: SparkContext, facilities: Seq[Facility], baseLat: Double, baseLng: Double, windSpd: Double, windHdg: Double, facilitiesActive: Boolean,
                          numTimesteps: Int,
                          baseSeed: Long,
                          numTrials: Int,
                          parallelism: Int): RDD[Particle] = {

    // Generate different seeds so that our simulations don't all end up with the same results
    val seeds = (baseSeed until baseSeed + parallelism)
    val seedRdd = sc.parallelize(seeds, parallelism)

    // Main computation: run simulations and compute aggregate return for each
    seedRdd.flatMap(
      trialResults(facilities, baseLat, baseLng, windSpd, windHdg, facilitiesActive, _, numTimesteps, numTrials / parallelism))
  }

  def trialResults(facilities: Seq[Facility], baseLat: Double, baseLng: Double, windSpd: Double, windHdg: Double, facilitiesActive: Boolean, seed: Long, numTimesteps: Int, numTrials: Int): Seq[Particle] = {

    val rand = new MersenneTwister(seed)
    val trialReturns = new Array[Particle](numTrials)
    val threshold = 0.075
    val degRad = 57.2958

    val latitudeDistribution = new NormalDistribution(rand, baseLat, 0.05, 0.0)
    val longitudeDistribution = new NormalDistribution(rand, baseLng, 0.05, 0.0)

    val angle = ((windHdg + 360) % 360) / degRad
    val baseDLat = -0.0013 + Math.cos(angle) * windSpd / 1200.0
    val baseDLng = -0.0035 + Math.sin(angle) * windSpd / 1200.0

    val dLatitudeDistribution = new NormalDistribution(rand, baseDLat, 0.0009, 0.0)
    val dLongitudeDistribution = new NormalDistribution(rand, baseDLng, 0.0020, 0.0)

    for (i <- 0 until numTrials) {
      val x = latitudeDistribution.sample()
      val y = longitudeDistribution.sample()
      val dX = dLatitudeDistribution.sample()
      val dY = dLongitudeDistribution.sample()

      val target = Particle(x, y, dX, dY, true)
      for (t <- 1 until numTimesteps) {
        target.step(1.0)

        for (facility <- facilities) {
          val dLat = target.x - facility.lat
          val dLng = target.y - facility.lng
          val dist = Math.sqrt(dLat * dLat + dLng * dLng)

          if (facilitiesActive && dist < threshold) {
            target.active = false
          }
        }
      }

      trialReturns(i) = target
    }
    trialReturns
  }
}