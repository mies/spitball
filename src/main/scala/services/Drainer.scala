package services

import org.slf4j.LoggerFactory

object Drainer {

  def logger = LoggerFactory.getLogger(Drainer.getClass)

  def parse(in: String): Iterator[String] = {
    def loop(unparsed: Iterator[Char], parsed: Iterator[String]): Iterator[String] = {
      if (unparsed.isEmpty) parsed
      else {
        val (head, tail) = unparsed.span(_ != ' ')
        val length = head.mkString.toInt
        val chunk = tail.slice(1, 1 + length).mkString
        loop(tail, parsed ++ Iterator(chunk))
      }
    }
    loop(in.toIterator, Iterator.empty)
  }

}
