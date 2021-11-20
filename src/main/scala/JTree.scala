import org.lwjgl.glfw.GLFW.{glfwPollEvents, glfwSwapBuffers, glfwWindowShouldClose}
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.{GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT, GL_FLOAT, GL_MODELVIEW, GL_PROJECTION, GL_QUADS, GL_RGB, glBegin, glClear, glClearColor, glColor3f, glDrawPixels, glEnd, glLoadIdentity, glMatrixMode, glOrtho, glRasterPos2f, glVertex3f, glVertex3i, glViewport}

import java.nio.ByteBuffer
import java.util.concurrent.{RecursiveAction, RecursiveTask}

abstract class JTree extends RecursiveTask[BigDecimal] {
  def setArr(a:Array[Array[Cell]]):Unit
  def computeT(): BigDecimal = {
    compute()
  }
}

class Interior(q1:JTree,q2:JTree,q3:JTree,q4:JTree) extends JTree {
  val quads = Seq(q1,q2,q3,q4)
  def compute(): BigDecimal = {
    q1.fork()
    q2.fork()
    q3.fork()

    val a = q4.computeT()
    val b = q1.join() + q2.join() + q3.join()
    q1.reinitialize()
    q2.reinitialize()
    q3.reinitialize()
    a+b
  }

  override def setArr(a:Array[Array[Cell]]): Unit = {
    q1.setArr(a)
    q2.setArr(a)
    q3.setArr(a)
    q4.setArr(a)
  }
}

class Leaf(var arr:Array[Array[Cell]],var out:Array[Array[Cell]],val tl:Coord,val br:Coord) extends JTree {
  override def compute(): BigDecimal = {
    var diff:BigDecimal = 0

    for(i <- tl.x until br.x) {
      for(j <- tl.y until br.y) {
        val oldCell = arr(i)(j)
        val neighbors = getNeighbors(i,j,arr)

        val thermConsts = (neighbors(0).cm1,neighbors(0).cm2,neighbors(0).cm3)
        val partTemps = neighbors.map(p => p.tempProps()).reduce((a,b) => ((a._1 + b._1),(a._2 + b._2),(a._3+b._3)))
        val adjusted:(BigDecimal,BigDecimal,BigDecimal) = ((partTemps._1 * oldCell.cm1), ((partTemps._2 * oldCell.cm2)), ((partTemps._3 * oldCell.cm3)))
        val newTemp:BigDecimal = (adjusted._1 + adjusted._2 + adjusted._3)/neighbors.length

        diff += (newTemp - oldCell.temp).abs

        out(i)(j) = oldCell.copy(temp = newTemp)
      }
    }
    diff
  }

  override def setArr(a: Array[Array[Cell]]): Unit = (arr = a)

  private def tuple3toSeq[T](t: (T,T,T)): Seq[T] = Seq(t._1,t._2,t._3)

  def getNeighbors(x:Int,y:Int,arr:Array[Array[Cell]]): IndexedSeq[Cell] = {
    var neighbors = IndexedSeq[Cell]()
    if(x > 0)
      neighbors :+= arr(x-1)(y)

    if(x < arr.length-1)
      neighbors :+= arr(x+1)(y)

    if(y > 0)
      neighbors :+= arr(x)(y-1)

    if(y < arr(x).length-1)
      neighbors :+= arr(x)(y+1)

    neighbors
  }
}

class Jacobi(var old:Array[Array[Cell]],val t:Double,val s:Double,alloy:Alloy, val maxSteps:Int,private val cellSize:Int) {
  var heat1 = t
  var heat2 = s
  val maxDiff = 40
  val minSize = 35
  val roomTemp = Alloy.roomTemp
  var lastDiff = 0
  private val graphicMaxHeat = 200
  //var old = Array.fill[Cell](arr.length,arr(0).length)(alloy.randomCell(20)) //room temp 20
  var out:Array[Array[Cell]] = Array.ofDim[Cell](old.length,old.head.length)
  val root:JTree = build(old,Coord(0,0),Coord(old.length,old(0).length))



  private def build(arr:Array[Array[Cell]],tl:Coord,br:Coord): JTree = {
    if((br.x - tl.x) * (br.y - tl.y) < minSize)
      new Leaf(arr,out,tl,br)
    else {
      val midX = ((br.x + tl.x)/2).toInt
      val midY = ((br.y + tl.y)/2).toInt
      new Interior(
        build(arr,Coord(tl.x,tl.y),Coord(midX,midY)),
        build(arr,Coord(midX,tl.y),Coord(br.x,midY)),
        build(arr,Coord(tl.x,midY),Coord(midX,br.y)),
        build(arr,Coord(midX,midY),Coord(br.x,br.y))
      )
    }
  }

  def compute(window:Option[Long]): Array[Array[Cell]] = {


    var steps = 0
    var difference:BigDecimal = 100
    var color:(Float,Float,Float) = null

    while(!glfwWindowShouldClose(window.get) && difference > maxDiff && steps < maxSteps) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      applyHeat()

      difference = root.computeT()
      old = out.map(p => p.clone())

      println(s"TEMP: ${old(5)(5).temp}")
      root.setArr(old)
      //updateHeatingWaveSynced(20)  //will be tested when graphics
      steps += 1
      println(s"Step: $steps")

      glBegin(GL_QUADS)
      glColor3f(0,1,0)
      //rect(-100000,-100000,200000,200000) //large rectangle to show edge borders
      rect(0,0,100,100)
      
      for(i <- old.indices) {
        for(j <- old(i).indices) {
          color = interpolateHeatColor(old(i)(j).temp)
          glColor3f(color._1,color._2,color._3)
          rect(i*cellSize,j*cellSize, cellSize,cellSize)
        }
      }
      glEnd()


      glfwSwapBuffers(window.get)
      glfwPollEvents()
      println(s"Difference: $difference    Steps: $steps")
      //Thread.sleep(25)
    }

    out
  }

  private def rect(x:Int,y:Int,width:Int,height:Int): Unit = {
    glVertex3i(x,y,0)
    glVertex3i(x+width,y,0)
    glVertex3i(x+width,y+height,0)
    glVertex3i(x,y+height,0)
  }

  private def applyHeat(): Unit = {
    old(0)(0).temp = heat1
    old.last.last.temp = heat2
  }


  private def updateHeating(t:Double,s:Double): Unit = {
    heat1 = t
    heat2 = s
  }
  private def updateHeatingWaveSynced(amplitude:Double): Unit = { //might look cool, we'll see
    val heat = math.sin(2 * math.Pi * 3 * (System.currentTimeMillis() / 1000.0))
  }

  def interpolateHeatColor(heat:BigDecimal): (Float,Float,Float) = {
    val t = ((heat - roomTemp) / (graphicMaxHeat - roomTemp)).toFloat
    val r = t
    val g = (1 - (2*(.5 - t).abs)).toFloat
    val b = (1 - t).toFloat
    (r,g,b)
  }
}


case class Coord(x:Int,y:Int)