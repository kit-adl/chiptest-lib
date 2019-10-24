package graph

import java.io.File
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object GraphImportXLSX extends App {
  
  var xlsxFile = new File("src/examples/resources/graph/points.xlsx")
 // var xygraph = XYGraph(getClass.getClassLoader.getResource("graph/xygraph-waveform.xml"))
  //-- Open

  var book = new  XSSFWorkbook(new FileInputStream(xlsxFile))
  
  //-- List
  var firstSheet = book.getSheetAt(0)
  var firstRow = firstSheet.getRow(0)
  
  println("First Cell: "+firstRow.getCell(0).getStringCellValue)
  println("Second Cell: "+firstRow.getCell(1).getStringCellValue)
  

  
 // println("Range from "+firstSheet.getRepeatingRows.getFirstRow+" to "+firstSheet.getRepeatingRows.getLastRow)
  
  
  
}