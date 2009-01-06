package shared
class Sequence {
  val window = Math.MAX_SHORT
  var seq: Short = 0
  var initialised = false

  def next = {
    if (seq == Math.MAX_SHORT) {
      seq = Math.MIN_SHORT
    }
    else {
      seq = (seq+1).toShort
    }
    seq
  }

  def inOrder(newSeq: Short) = {
    if ((newSeq > seq && (newSeq - seq) < window ) ||
         newSeq < 0 && seq > 0 ||
         !initialised) {
      initialised = true
      seq = newSeq
      true
    }
    else {
      println("Threw out old packet.")
      false
    }
  }
}
