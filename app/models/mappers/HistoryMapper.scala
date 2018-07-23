package models.mappers

import models.{BikeStatus, History, HistoryWithStatus}


object HistoryMapper {
  implicit class MapHistoryQuery(data: (History, BikeStatus)) {
    def toQueryClass = {
      val history = data._1
      val bike = data._2

      HistoryWithStatus(history.id, history.studentId, history.remark,
        history.borrowDate, history.returnDate,
        bike
      )
    }
  }
}