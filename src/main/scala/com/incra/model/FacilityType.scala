package com.incra.model

import scala.slick.lifted.MappedTo

/**
 * FacilityType for a Facility.
 *
 * @author Jeff Risberg
 * @since 10/04/14
 */
object FacilityType extends scala.Enumeration {

  val Diagnosis = FacilityType(1L, "Diagnosis")

  val Treatment = FacilityType(2L, "Treatment")

  val Passthrough = FacilityType(3L, "Passthrough")

  val Quarantine = FacilityType(4L, "Quarantine")

  case class FacilityType(value: Long, name: String) extends Val(nextId, name)

  final def withKey(k: Long): FacilityType = {
    values.iterator.map(_.asInstanceOf[FacilityType]).find(_.value == k).get
  }

  final def list: List[FacilityType] = values.toList.map(_.asInstanceOf[FacilityType])
}


