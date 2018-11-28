package models

case class Student(id: String, firstName: String, lastName: String,
                   phone: String, major: Option[String], `type`: String, status: String,
                   address: Option[String], department: Option[String],
                   profilePicture: Option[String]) {
  def toStudentWithFaculty(faculty: Faculty) = StudentWithFaculty(
    this.id, this.firstName, this.lastName, this.phone, Some(faculty),
    this.`type`, this.status, this.address, this.department, this.profilePicture
  )
}

case class StudentWithFaculty(id: String, firstName: String, lastName: String,
                   phone: String, major: Option[Faculty], `type`: String, status: String,
                   address: Option[String], department: Option[String],
                   profilePicture: Option[String])

case class StudentResponse(student: Student)

case class StudentFields(
                          id: String = "id", firstName: String = "firstName", lastName: String = "lastName",
                          phone: String = "phone", major: String = "major", `type`: String = "type", status: String = "status",
                          address: String = "address", department: String = "department",
                          profilePicture: String = "profilePicture"
                        )

case class StudentQuery(userId: Option[String],
                        `type`: Option[String],
                        name: Option[String],
                        page: Int,
                        pageSize: Int = 10)