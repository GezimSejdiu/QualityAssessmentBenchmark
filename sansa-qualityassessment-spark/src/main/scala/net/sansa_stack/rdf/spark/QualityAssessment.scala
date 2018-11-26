package net.sansa_stack.rdf.spark

import java.io.File

import scala.collection.mutable

import net.sansa_stack.rdf.spark.io._
import net.sansa_stack.rdf.spark.qualityassessment._
import org.apache.jena.riot.Lang
import org.apache.spark.sql.SparkSession

object QualityAssessment {

  def main(args: Array[String]) {
    parser.parse(args, Config()) match {
      case Some(config) =>
        run(config.in, config.out)
      case None =>
        println(parser.usage)
    }
  }

  def run(input: String, output: String): Unit = {

    val rdf_quality_file = new File(input).getName

    val spark = SparkSession.builder
      .appName(s"RDF Quality Assessment Example $rdf_quality_file")
      // .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()

    println("======================================")
    println("| RDF Quality Assessment Example     |")
    println("======================================")

    val lang = Lang.NTRIPLES
    val triples = spark.rdf(lang)(input)

    triples.persist()
    // compute  quality assessment

    println("TripleCount: " + triples.count())

    var startTime = System.currentTimeMillis()

    val HumanReadableLicense = triples.assessHumanReadableLicense()
    println(HumanReadableLicense + " :(L2) [Detection of a Human Readable License]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val MachineReadableLicense = triples.assessMachineReadableLicense()
    println(MachineReadableLicense + " :(L1) [Detection of a Machine Readable License]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val InterlinkingCompleteness = triples.assessInterlinkingCompleteness()
    println(InterlinkingCompleteness + " :(I2) [Linkage Degree of Linked External Data Providers]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val LabeledResources = triples.assessLabeledResources()
    println(LabeledResources + " :(U1) [Detection of a Human Readable Labels]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val NoHashUris = triples.assessNoHashUris()
    println(NoHashUris + " :(RC1) [Short URIs]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val XSDDatatypeCompatibleLiterals = triples.assessXSDDatatypeCompatibleLiterals()
    println(XSDDatatypeCompatibleLiterals + " :(SV3) [Identification of Literals with Malformed Datatypes]: " + (System.currentTimeMillis() - startTime) + "ms.")

    startTime = System.currentTimeMillis()
    val ExtensionalConciseness = triples.assessExtensionalConciseness()
    println(ExtensionalConciseness + " :(CN2) [Extensional Conciseness]: " + (System.currentTimeMillis() - startTime) + "ms.")

   
    spark.close()
  }

  case class Config(
    in: String = "",
    out: String = "")

  val parser = new scopt.OptionParser[Config]("RDF Quality Assessment Example") {

    head("RDF Quality Assessment Example")

    opt[String]('i', "input").required().valueName("<path>").
      action((x, c) => c.copy(in = x)).
      text("path to file that contains the data (in N-Triples format)")

    opt[String]('o', "out").required().valueName("<directory>").
      action((x, c) => c.copy(out = x)).
      text("the output directory")

    help("help").text("prints this usage text")
  }
}
