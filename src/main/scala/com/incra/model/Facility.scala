package com.incra.model

import com.incra.model.FacilityType.FacilityType
import scala.slick.driver.MySQLDriver.simple._

/**
  * Definition of the Facility entity
  *
  * @author Jeff Risberg
  * @since 08/12/2015
  */
case class Facility(id: Option[Long], name: String, lat: Double, lng: Double, capacity: Double, facilityType: FacilityType) extends Entity[Long]

class FacilityTable(tag: Tag) extends Table[Facility](tag, "FACILITY") {

  implicit val facilityTypeMapper = MappedColumnType.base[FacilityType, Long](_.value, FacilityType.withKey(_))

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def name = column[String]("NAME")

  def lat = column[Double]("LAT")

  def lng = column[Double]("LNG")

  def capacity = column[Double]("CAPACITY")

  def facilityType = column[FacilityType]("FACILITY_TYPE")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id.?, name, lat, lng, capacity, facilityType) <> (Facility.tupled, Facility.unapply _)
}
