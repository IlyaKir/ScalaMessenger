package errors

case object WrongHeaderException extends Exception("there's no user from header in db")
case object WrongMessageIdException extends Exception("wrong message id")
case object NoHeaderException extends Exception("can't execute secure method without header")