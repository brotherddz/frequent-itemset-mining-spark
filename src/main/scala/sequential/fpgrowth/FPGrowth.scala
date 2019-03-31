package sequential.fpgrowth

import sequential.Apriori.Itemset
import sequential.Util.printItemsets
import sequential.{FIM, Util}

import scala.collection.mutable

object FPGrowth {

  def main(args: Array[String]): Unit = {
    val itemsets =
      """|1,3
         |1,2,3
         |1
         |1,3,4
         |3
         |1,2
         |1,2,3,4
      """.stripMargin
    val frequentItemsets = new FPGrowth().executeByText(itemsets, 3)
    printItemsets(frequentItemsets)
  }

}

/**
  * 1. Find singletons, order by most frequent
  * 2. Build FP-Tree by creating all possible paths (TODO: Links between items)
  * 3. For each singleton, find a list of conditional pattern base
  * 4. Build the conditional FP-Tree
  */
// TODO: Possible problem with ("mushroom.txt", 0.35) and "" as separator
class FPGrowth extends FIM {

  def findFrequentItemsets(transactions: List[Itemset], minSupport: Double): List[Itemset] = {
    val support = Util.absoluteSupport(minSupport, transactions.size)
    val singletons = mutable.LinkedHashMap(findSingletons(transactions, support).map(i => i -> Option.empty[FPNode]): _*)
    val fpTree = new FPTree(transactions, support, singletons)

    singletons.keys.toList.reverse
      .flatMap(s => findFrequentItemsets(fpTree, List(s), support))
  }

  def findFrequentItemsets(fpTree: FPTree, prefix: List[String], minSupport: Int): List[Itemset] = {
    val isFrequent = fpTree.isPrefixFrequent(prefix.head, minSupport)
    if (isFrequent) {
      val condFPTree = fpTree.conditionalTreeForPrefix(prefix.head, minSupport)
      val prefixes = generatePrefixes(prefix, fpTree.singletons.keySet)
      return prefix.sorted +: prefixes.flatMap(p => findFrequentItemsets(condFPTree, p, minSupport))
    }
    List.empty
  }

  /**
    * Generates all possible prefixes for a given prefix.
    * Header: {a, b, c, d}
    * Prefix: {d} => Out: {da, db, dc}
    * Prefix: {dc} => Out: {dca, dcb}
    */
  private def generatePrefixes(prefix: List[String], header: scala.collection.Set[String]): List[List[String]] = {
    header.filter(i => !prefix.contains(i))
      .map(i => i +: prefix).toList
  }

  private def findSingletons(transactions: List[Itemset], minSupport: Int) = {
    transactions.flatten
      .groupBy(identity)
      .map(t => (t._1, t._2.size))
      .filter(_._2 >= minSupport)
      .toSeq.sortBy(_._2)
      .reverse.map(t => t._1)
  }

  override def findFrequentItemsets(fileName: String, separator: String, transactions: List[Itemset], minSupport: Double): List[Itemset] = {
    if (fileName.isEmpty) {
      findFrequentItemsets(transactions, minSupport)
    }
    else {
      findFrequentItemsets(Util.parseTransactions(fileName, separator), minSupport)
    }
  }

}
