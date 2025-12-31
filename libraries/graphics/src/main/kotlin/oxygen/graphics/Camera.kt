package oxygen.graphics

import arc.*
import arc.math.geom.*
import oxygen.math.geom.*
import kotlin.math.*

/** Base class for {@link OrthographicCamera} and {@link PerspectiveCamera}.
 * @author mzechner */
abstract class OCamera {
    /** the position of the camera **/
    val position = Vec3()

    /** the unit length direction vector of the camera **/
    val direction = Vec3(0f, 0f, -1f)

    /** the unit length up vector of the camera **/
    val up = Vec3(0f, 1f, 0f)

    /** the projection matrix **/
    val projection = Mat3D()

    /** the view matrix **/
    val view = Mat3D()

    /** the combined projection and view matrix **/
    val combined = Mat3D()

    /** the inverse combined projection and view matrix **/
    val invProjectionView = Mat3D()

    /** the near clipping plane distance, has to be positive **/
    var near = 1f

    /** the far clipping plane distance, has to be positive **/
    val far = 100f

    /** the viewport width **/
    var width = 0f

    /** the viewport height **/
    var height = 0f

    /** the frustum, for clipping operations **/
    val frustum = Frustum()

    val tmpVec = Vec3()
    private val ray = Ray(Vec3(), Vec3())

    abstract fun update()
    abstract fun update(updateFrustum: Boolean)

    fun resize(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    fun lookAt(x: Float, y: Float, z: Float) {
        tmpVec.set(x, y, z).sub(position).nor() // up and direction must ALWAYS be orthonormal vectors
        if (!tmpVec.isZero) {
            val dot = tmpVec.dot(up)
            if (abs(dot - 1) < 0.000000001f) {
                // Collinear
                up.set(direction).scl(-1f)
            } else if (abs(dot + 1) < 0.000000001f) {
                // Collinear opposite
                up.set(direction)
            }
            direction.set(tmpVec)
            normalizeUp()
        }
    }

    /**
     * Recalculates the direction of the camera to look at the point (x, y, z).
     * @param target the point to look at
     */
    fun lookAt(target: Vec3) {
        lookAt(target.x, target.y, target.z)
    }

    /**
     * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
     * recalculating the up vector via a cross product between right and direction.
     */
    fun normalizeUp() {
        tmpVec.set(direction).crs(up)
        up.set(tmpVec).crs(direction).nor()
    }

    /** Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
     * will not be orthogonalized.
     *
     * @param angle the angle
     * @param axisX the x-component of the axis
     * @param axisY the y-component of the axis
     * @param axisZ the z-component of the axis */
    fun rotate(angle: Float, axisX: Float, axisY: Float, axisZ: Float) {
        direction.rotate(angle, axisX, axisY, axisZ)
        up.rotate(angle, axisX, axisY, axisZ)
    }

    /** Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
     * will not be orthogonalized.
     *
     * @param axis the axis to rotate around
     * @param angle the angle, in degrees */
    fun rotate(axis: Vec3, angle: Float) {
        direction.rotate(axis, angle)
        up.rotate(axis, angle)
    }

    /** Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be
     * orthogonalized.
     *
     * @param transform The rotation matrix */
    fun rotate(transform: Mat3D) {
        direction.rot(transform)
        up.rot(transform)
    }

    /** Rotates the direction and up vector of this camera by the given {@link Quaternion}. The direction and up vector will not be
     * orthogonalized.
     *
     * @param quat The quaternion */
    fun rotate(quat: Quat) {
        quat.transform(direction)
        quat.transform(up)
    }

    /** Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to
     * given point. The direction and up vector will not be orthogonalized.
     *
     * @param point the point to attach the axis to
     * @param axis the axis to rotate around
     * @param angle the angle, in degrees */
    fun rotateAround(point: Vec3, axis: Vec3, angle: Float) {
        tmpVec.set(point)
        tmpVec.sub(position)
        translate(tmpVec)
        rotate(axis, angle)
        tmpVec.rotate(axis, angle)
        translate(-tmpVec.x, -tmpVec.y, -tmpVec.z)
    }

    /** Transform the position, direction and up vector by the given matrix
     *
     * @param transform The transform matrix */
    fun transform(transform: Mat3D) {
        position.mul(transform)
        rotate(transform)
    }

    /** Moves the camera by the given amount on each axis.
     * @param x the displacement on the x-axis
     * @param y the displacement on the y-axis
     * @param z the displacement on the z-axis */
    fun translate(x: Float, y: Float, z: Float) {
        position.add(x, y, z)
    }

    /** Moves the camera by the given vector.
     * @param vec the displacement vector */
    fun translate(vec: Vec3) {
        position.add(vec)
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject, but does not
     * rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left corner, y
     * pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0 will return a
     * point on the near plane, a z-coordinate of 1 will return a point on the far plane. This method allows you to specify the
     * viewport position and dimensions in the coordinate system expected by glViewport(int, int, int, int), with the
     * origin in the bottom left corner of the screen.
     * @param screenCoords the point in screen coordinates (origin top left)
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    fun unproject(
        screenCoords: Vec3,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Vec3 {
        var x = screenCoords.x
        var y = screenCoords.y
        x -= viewportX
        y -= viewportY
        screenCoords.x = (2f * x) / viewportWidth - 1f
        screenCoords.y = (2f * y) / viewportHeight - 1f
        screenCoords.z = 2f * screenCoords.z - 1f
        Mat3D.prj(screenCoords, invProjectionView)
        return screenCoords
    }

    /**
     * Function to translate a point given in screen coordinates to world space. It's the same as GLU gluUnProject but does not
     * rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from {@link Graphics#getWidth()} and
     * {@link Graphics#getHeight()}. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the top left
     * corner, y pointing down, x pointing to the right) as reported by the touch methods in {@link Input}. A z-coordinate of 0
     * will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
     * @param screenCoords the point in screen coordinates
     * @return the mutated and unprojected screenCoords {@link Vec3}
     */
    fun unproject(screenCoords: Vec3): Vec3 {
        unproject(screenCoords, 0f, 0f, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
        return screenCoords
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes.
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    fun project(worldCoords: Vec3): Vec3 {
        project(worldCoords, 0f, 0f, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
        return worldCoords
    }

    /**
     * Projects the {@link Vec3} given in world space to screen coordinates. It's the same as GLU gluProject with one small
     * deviation: The viewport is assumed to span the whole screen. The screen coordinate system has its origin in the
     * <b>bottom</b> left, with the y-axis pointing <b>upwards</b> and the x-axis pointing to the right. This makes it easily
     * useable in conjunction with Batch and similar classes. This method allows you to specify the viewport position and
     * dimensions in the coordinate system expected by glViewport(int, int, int, int), with the origin in the bottom
     * left corner of the screen.
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the mutated and projected worldCoords {@link Vec3}
     */
    fun project(
        worldCoords: Vec3,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Vec3 {
        Mat3D.prj(worldCoords, combined)
        worldCoords.x = viewportWidth * (worldCoords.x + 1f) / 2f + viewportX
        worldCoords.y = viewportHeight * (worldCoords.y + 1f) / 2f + viewportY
        worldCoords.z = (worldCoords.z + 1f) / 2f
        return worldCoords
    }

    fun getMouseRay() = getPickRay(Core.input.mouseX().toFloat(), Core.input.mouseY().toFloat())

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
     * @param viewportWidth the width of the viewport in pixels
     * @param viewportHeight the height of the viewport in pixels
     * @return the picking Ray.
     */
    fun getPickRay(
        screenX: Float,
        screenY: Float,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Ray {
        unproject(ray.origin.set(screenX, screenY, 0f), viewportX, viewportY, viewportWidth, viewportHeight)
        unproject(ray.direction.set(screenX, screenY, 1f), viewportX, viewportY, viewportWidth, viewportHeight)
        ray.direction.sub(ray.origin).nor()
        return ray
    }

    /**
     * Creates a picking {@link Ray} from the coordinates given in screen coordinates. It is assumed that the viewport spans the
     * whole screen. The screen coordinates origin is assumed to be in the top left corner, its y-axis pointing down, the x-axis
     * pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
     * @return the picking Ray.
     */
    fun getPickRay(screenX: Float, screenY: Float) =
        getPickRay(screenX, screenY, 0f, 0f, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())

}

class OrthographicCamera : OCamera {
    val zoom = 1f

    constructor() {
        near = 0f
    }

    constructor(width: Float, height: Float) {
        this.width = width
        this.height = height
        this.near = 0f
        update()
    }


    override fun update() {
        update(true)
    }

    override fun update(updateFrustum: Boolean) {
        projection.setToOrtho(
            zoom * -width / 2f,
            zoom * (width / 2f),
            zoom * -(height / 2f),
            zoom * height / 2f,
            near,
            far
        )
        view.setToLookAt(direction, up)
        view.translate(-position.x, -position.y, -position.z)
        combined.set(projection).mul(view)
        if (updateFrustum) {
            invProjectionView.set(combined).inv()
            frustum.update(invProjectionView)
        }
    }

    /** Sets this camera to an orthographic projection using a viewport fitting the screen resolution, centered at
     * (Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2), with the y-axis pointing up or down.
     * @param yDown whether y should be pointing down */
    fun setToOrtho(yDown: Boolean) {
        setToOrtho(yDown, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
    }

    /** Sets this camera to an orthographic projection, centered at (viewportWidth/2, viewportHeight/2), with the y-axis pointing
     * up or down.
     * @param yDown whether y should be pointing down.
     * @param viewportWidth
     * @param viewportHeight */
    fun setToOrtho(yDown: Boolean, viewportWidth: Float, viewportHeight: Float) {
        if (yDown) {
            up.set(0f, -1f, 0f)
            direction.set(0f, 0f, 1f)
        } else {
            up.set(0f, 1f, 0f)
            direction.set(0f, 0f, -1f)
        }
        position.set(zoom * viewportWidth / 2.0f, zoom * viewportHeight / 2.0f, 0f)
        this.width = viewportWidth
        this.height = viewportHeight
        update()
    }

    /** Rotates the camera by the given angle around the direction vector. The direction and up vector will not be orthogonalized.
     * @param angle */
    fun rotate(angle: Float) {
        rotate(direction, angle)
    }

    /** Moves the camera by the given amount on each axis.
     * @param x the displacement on the x-axis
     * @param y the displacement on the y-axis */
    fun translate(x: Float, y: Float) {
        translate(x, y, 0f)
    }

    /** Moves the camera by the given vector.
     * @param vec the displacement vector */
    fun translate(vec: Vec2) {
        translate(vec.x, vec.y, 0f)
    }
}

class PerspectiveCamera : OCamera {
    var fov = 67f

    constructor()

    constructor(fov: Float, width: Float, height: Float) {
        this.fov = fov
        this.width = width
        this.height = height
        update()
    }

    override fun update() {
        update(true)
    }

    override fun update(updateFrustum: Boolean) {
        val aspect = width / height
        projection.setToProjection(abs(near), abs(far), fov, aspect)
        view.setToLookAt(position, tmpVec.set(position).add(direction), up)
        combined.set(projection).mul(view)
        if (updateFrustum) {
            invProjectionView.set(combined).inv()
            frustum.update(invProjectionView)
        }
    }
}
