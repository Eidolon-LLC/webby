package webby.mvc.script.compiler
import java.io.File
import java.nio.file.Path
import java.util

import io.bit3.jsass.{CompilationException, Compiler, Options, OutputStyle}

import scala.collection.JavaConverters._

/**
  * Compiler for SASS files. Uses libsass library via jsass.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "io.bit3" % "jsass" % "5.5.3"
  * }}}
  */
case class LibSassCompiler(includePaths: Seq[String] = Nil,
                           outputStyle: OutputStyle = OutputStyle.NESTED) extends ScriptCompiler {
  def sourceFileExt: String = "sass"
  def targetFileExt: String = "css"
  def targetContentType: String = "text/css"

  def isIndentedSyntaxSrc = true

  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    val compiler = new Compiler
    val options = new Options
    val sourcePaths = new util.ArrayList[File]()
    sourcePaths.addAll(includePaths.map(new File(_)).asJavaCollection)
    options.setIncludePaths(sourcePaths)

    options.setIsIndentedSyntaxSrc(isIndentedSyntaxSrc)

    options.setOutputStyle(outputStyle)

    try {
      var css = compiler.compileString(source, sourcePath.toUri, null, options).getCss
      if (css == null) Right("")
      else {
        // Remove BOM (byte-order-mark) from beginning of resulting file
        if (css.length > 1 && css.charAt(0) == 0xfeff) css = css.substring(1)
        Right(css)
      }
    } catch {
      case e: CompilationException =>
        Left(e.getErrorMessage)
    }
  }
}
