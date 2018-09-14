package models

case class TimeUsageReport(startDate: Option[java.util.Date], endDate: Option[java.util.Date])

case class TimeUsageReportField(
                               startDate: String = "startDate",
                               endDate: String = "endDate"
                               )