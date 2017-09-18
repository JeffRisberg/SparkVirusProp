package com.incra.services

import java.sql.Date

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.incra.model.{Facility, FacilityTable, FacilityType}

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * @author Jeff Risberg
 * @since 10/08/2014
 */
class FacilityService(implicit val bindingModule: BindingModule) extends Injectable {
  private def mainDatabase = inject[Database]

  println("InitFacilityService")
  mainDatabase withSession {
    implicit session =>
      val facilities = TableQuery[FacilityTable]

      // Create the tables, including primary and foreign keys
      if (MTable.getTables("facility").list().isEmpty) {
        (facilities.ddl).create

        facilities += Facility(None, "Primary Treatment Center", 6.891, -10.32, 2000.0, FacilityType.Treatment)
        facilities += Facility(None, "Patient Intake Center", 6.79, -10.29, 1000.0, FacilityType.Diagnosis)
        facilities += Facility(None, "Emergency Treatment Center", 6.51, -10.67, 1200.0, FacilityType.Treatment)
        facilities += Facility(None, "Holding Area 1", 6.370, -10.790, 3400.0, FacilityType.Quarantine)
        facilities += Facility(None, "Holding Area 2", 6.27, -10.785, 2000.0, FacilityType.Quarantine)
      }
  }
  println("EndInitFacilityService")

  /**
   *
   */
  def getEntityList(): List[Facility] = {
    mainDatabase withSession {
      implicit session =>

        TableQuery[FacilityTable].list
    }
  }

  /**
   *
   */
  def findById(id: Long): Option[Facility] = {
    mainDatabase withSession {
      implicit session =>

        TableQuery[FacilityTable].where(_.id === id).firstOption
    }
  }
}
