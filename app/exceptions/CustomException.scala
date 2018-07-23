package exceptions

abstract class CustomException() extends Exception

case object DBException extends CustomException
