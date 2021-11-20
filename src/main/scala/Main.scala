import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.{GLFW_DECORATED, GLFW_FALSE, GLFW_KEY_DOWN, GLFW_KEY_ESCAPE, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_UP, GLFW_RELEASE, GLFW_RESIZABLE, GLFW_TRUE, GLFW_VISIBLE, glfwCreateWindow, glfwDefaultWindowHints, glfwDestroyWindow, glfwGetPrimaryMonitor, glfwGetVideoMode, glfwGetWindowSize, glfwInit, glfwMakeContextCurrent, glfwPollEvents, glfwSetErrorCallback, glfwSetKeyCallback, glfwSetScrollCallback, glfwSetWindowPos, glfwSetWindowShouldClose, glfwShowWindow, glfwSwapBuffers, glfwSwapInterval, glfwTerminate, glfwWindowHint, glfwWindowShouldClose}
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.{GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT, GL_MODELVIEW, GL_PROJECTION, glClear, glClearColor, glLoadIdentity, glMatrixMode, glOrtho, glScaled, glTranslated, glViewport}
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL

import scala.util.Using

object Main {
  private var window: Option[Long] = Option.empty
  var height = 1080/4
  var width = 1920/4
  var s = 200
  var t = 200
  var c1 = .75
  var c2 = 1.0
  var c3 = 1.25
  var looping = true
  var maxSteps = 2000
  val res = (1920, 1080)

  private val translation = 20

  def loop(): Unit = {
    GL.createCapabilities()
    glClearColor(1,1,1,0)
    glViewport(0,0,1920,1080)
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    glOrtho(0,1920,1080,0,1,-1)
    glMatrixMode(GL_MODELVIEW)
    while ({
      //body
      val alloy = new Alloy(height,width, BigDecimal(1) / BigDecimal(3), c1, BigDecimal(1) / BigDecimal(3), c2, BigDecimal(1) / BigDecimal(3), c3)
      val jacobi = new Jacobi(alloy.arr, t, s, alloy, maxSteps,math.floor(res._1.toFloat/width.toFloat).toInt)
      jacobi.compute(window)

      //condition
      looping
    }) ()
  }

  def main(args: Array[String]): Unit = {
    for (i <- args.indices) {
      args(i) match {
        case "-s" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -s")
          else {
            try {
              s = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -s is not a number")
            }
          }
        case "-t" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -t")
          else {
            try {
              t = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -t is not a number")
            }
          }
        case "-h" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -h")
          else {
            try {
              height = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -h is not a number")
            }
          }
        case "-w" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -w")
          else {
            try {
              width = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -w is not a number")
            }
          }
        case "-c1" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -c1")
          else {
            try {
              c1 = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -c1 is not a number")
            }
          }
        case "-c2" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -c2")
          else {
            try {
              c2 = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -c2 is not a number")
            }
          }
        case "-c3" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -c3")
          else {
            try {
              c3 = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -c3 is not a number")
            }
          }
        case "-maxSteps" =>
          if (args.length <= i + 1)
            throw new IllegalArgumentException("Missing value for -maxSteps")
          else {
            try {
              maxSteps = args(i + 1).toInt
            } catch {
              case e: NumberFormatException => throw IllegalArgumentException("Value for -maxSteps is not a number")
            }
          }
        case "-loop" => looping = true
        case _ =>
      }
    }

    run()
  }

  def run(): Unit = {
    println("Hello LWJGL " + Version.getVersion + "!")

    init()
    loop()

    glfwFreeCallbacks(window.get)
    glfwDestroyWindow(window.get)

    glfwTerminate()
    glfwSetErrorCallback(null).free()
  }

  def init(): Unit = {
    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW")

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    //glfwWindowHint(GFLW_DECORATED, GLFW_FALSE)

    window = Option(glfwCreateWindow(1920, 1080, "Heatmap Propagation", glfwGetPrimaryMonitor(), NULL)) //use the java NULL
    if (window.isEmpty)
      throw new RuntimeException("Failed to create the GLFW window")

    glfwSetKeyCallback(window.get, (window, key, scancode, action, mods) => {
      (key, action) match {

        case (GLFW_KEY_ESCAPE, GLFW_RELEASE) =>
          glfwSetWindowShouldClose(window, true)
          looping = false
        case (GLFW_KEY_RIGHT, GLFW_RELEASE) => glTranslated(translation, 0, 0)
        case (GLFW_KEY_LEFT, GLFW_RELEASE) => glTranslated(-translation, 0, 0)
        case (GLFW_KEY_UP, GLFW_RELEASE) => glTranslated(0, -translation, 0)
        case (GLFW_KEY_DOWN, GLFW_RELEASE) => glTranslated(0, translation, 0)
        case _ =>
      }

    })

    glfwSetScrollCallback(window.get, (win, dx, dy) => {
      val s = 1 + (.02 * dy)
      glScaled(s, s, 0)
    })

    Using(stackPush()) { stack =>
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)

      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(window.get, pWidth, pHeight)
      // Get the resolution of the primary monitor
      val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor)
      // Center the window
      glfwSetWindowPos(window.get, (vidmode.width - pWidth.get(0)) / 2, (vidmode.height - pHeight.get(0)) / 2)
    }

    glfwMakeContextCurrent(window.get)
    glfwSwapInterval(1)
    glfwShowWindow(window.get)

  }
}
