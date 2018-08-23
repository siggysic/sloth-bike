package models

case class Student(id: String, firstName: String, lastName: String,
                   major: String, profilePicture: String)

case class StudentResponse(student: Student)