package models

import java.sql.Timestamp

case class BorrowStatisticTable(borrowTime: Int, studentCount: Int)

case class BorrowStatisticTableQuery(
                                      startDate: Option[Timestamp],
                                      endDate: Option[Timestamp],
                                      pageSize: PageSize
                                    )

case class BorrowStatisticTableForm(
                                      startDate: Option[String],
                                      endDate: Option[String],
                                      page: Int,
                                      size: Int
                                    )

case class PopularityForm(
                           startDate: Option[String],
                           endDate: Option[String]
                         )