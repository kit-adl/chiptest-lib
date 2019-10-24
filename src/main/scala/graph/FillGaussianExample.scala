package graph

import kit.edu.ipe.adl.chiptest.measurement.data.XYGraph

object FillGaussianExample extends App {

  //-- Create XY Graph
  var graph = new XYGraph()

  //-- Fill with gaussian
  graph.fillRandomGaussian(count = 1000,max = 20.0)

  graph.toJFreeChart
}