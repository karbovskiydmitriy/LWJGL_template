package template;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;

import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

import static template.Cube.*;
import static glu.GLU.*;

public class LWJGL_template {

	static final float SIZE = 100.0f;

	static long window;

	static float step = 0.18f;
	static float fovY = 60.0f;
	static float zNear = 0.1f;
	static float zFar = 1000.0f;

	static long startTime;
	static long lastTime;
	static long currentTime;
	static float angle;

	static int verticesVBO;
	static int indicesVBO;
	static int vertexShader;
	static int fragmentShader;
	static int renderProgram;

	static int timeLocation;
	static int screenSizeLocation1;
	static int depthBufferLocation1;

	static Dimension dimension;

	public static void main(String[] args) {
		init();

		while (!glfwWindowShouldClose(window)) {
			draw();

			glfwSwapBuffers(window);
			glfwPollEvents();
		}

		GL.setCapabilities(null);
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private static void init() {
		glfwInit();

		window = glfwCreateWindow(500, 500, "Hello World!", NULL, NULL);

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		glfwShowWindow(window);

		GL.createCapabilities();

		int[] width = new int[1];
		int[] height = new int[1];
		glfwGetWindowSize(window, width, height);
		dimension = new Dimension(width[0], height[0]);
		glViewport(0, 0, width[0], height[0]);
		glMatrixMode(GL_PROJECTION);
		FloatBuffer projectionMatrix = FloatBuffer.allocate(16);
		perspective(fovY, (float) dimension.height / dimension.height, zNear, zFar).store(projectionMatrix);
		glLoadMatrixf(projectionMatrix.array());
		glEnable(GL_DEPTH_TEST);
		glShadeModel(GL_SMOOTH);
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

		initShaders();

		verticesVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, verticesVBO);
		glBufferData(GL_ARRAY_BUFFER, cubeVertices, GL_STATIC_DRAW);

		indicesVBO = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, cubeIndices, GL_STATIC_DRAW);

		startTime = System.currentTimeMillis();
		lastTime = startTime;
	}

	private static void draw() {
		currentTime = System.currentTimeMillis();
		if (currentTime - lastTime >= 10) {
			lastTime = currentTime;
			angle += step;
		}

		glClearColor(1f, 1f, 1f, 1f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glMatrixMode(GL_MODELVIEW);
		FloatBuffer modelviewMatrix = FloatBuffer.allocate(16);
		lookAt(new Vector3f(0f, SIZE * 1.8f, SIZE * 3.6f), new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f)).store(modelviewMatrix);
		glLoadMatrixf(modelviewMatrix.array());
		glRotatef(angle, 0f, 1f, 0f);

		if (renderProgram != 0) {
			glUseProgram(renderProgram);
			glUniform1f(timeLocation, (float) (currentTime - startTime));
			glUniform2f(screenSizeLocation1, (float) dimension.width, (float) dimension.height);
		}

		glBindBuffer(GL_ARRAY_BUFFER, verticesVBO);
		glVertexPointer(3, GL_FLOAT, 0, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		glEnableClientState(GL_VERTEX_ARRAY);
		glDrawElements(GL_TRIANGLES, 24, GL_UNSIGNED_INT, 0);
		glDisableClientState(GL_VERTEX_ARRAY);

		glUseProgram(0);
	}

	private static void initShaders() {
		glUseProgram(0);

		if (renderProgram != 0) {
			glDetachShader(renderProgram, vertexShader);
			glDetachShader(renderProgram, fragmentShader);
			glDeleteShader(vertexShader);
			glDeleteShader(fragmentShader);
			glDeleteProgram(renderProgram);
		}

		vertexShader = loadShader(".\\shaders\\vertex.glsl", GL_VERTEX_SHADER);
		if (vertexShader == 0) {
			renderProgram = 0;

			return;
		}

		fragmentShader = loadShader(".\\shaders\\fragment.glsl", GL_FRAGMENT_SHADER);
		if (fragmentShader == 0) {
			renderProgram = 0;

			return;
		}

		renderProgram = createProgram(vertexShader, fragmentShader);
		if (renderProgram != 0) {
			glUseProgram(renderProgram);

			timeLocation = glGetUniformLocation(renderProgram, "time");
			screenSizeLocation1 = glGetUniformLocation(renderProgram, "screenSize");
			depthBufferLocation1 = glGetUniformLocation(renderProgram, "depthBuffer");
		} else {
			return;
		}
	}

	private static int createProgram(int vertexShader, int fragmentShader) {
		int program = glCreateProgram();

		if (vertexShader != 0) {
			glAttachShader(program, vertexShader);
		}
		if (fragmentShader != 0) {
			glAttachShader(program, fragmentShader);
		}
		glLinkProgram(program);

		int[] linked = new int[1];
		glGetProgramiv(program, GL_LINK_STATUS, linked);

		if (linked[0] != 0) {
			return program;
		} else {
			if (vertexShader != 0) {
				glDetachShader(program, vertexShader);
				glDeleteShader(vertexShader);
			}
			if (fragmentShader != 0) {
				glDetachShader(program, fragmentShader);
				glDeleteShader(fragmentShader);
			}
			glDeleteProgram(program);

			return 0;
		}
	}

	private static int loadShader(String fileName, int shaderType) {
		try {
			String shaderText = new String(Files.readAllBytes(new File(fileName).toPath()));
			int shader = glCreateShader(shaderType);
			glShaderSource(shader, shaderText);
			glCompileShader(shader);

			int[] compiled = new int[1];
			glGetShaderiv(shader, GL_COMPILE_STATUS, compiled);

			if (compiled[0] != 0) {
				return shader;
			} else {
				glDeleteShader(shader);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

}