package zio.prometheus

sealed trait Labels extends Product { self =>
  private[prometheus] def asSeq: Seq[String] =
    self.productIterator.toSeq.asInstanceOf[Seq[String]]
}

object Labels {
  def apply(value: String) = Label1(value)
  def apply(value: String, value1: String) = Label2(value, value1)

  case object Empty extends Labels
  case class Label1(value: String) extends Labels
  case class Label2(value: String, value1: String) extends Labels
}
