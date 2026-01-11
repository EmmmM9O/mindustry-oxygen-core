package oxygen.math

import arc.math.*
import arc.math.geom.*

/**
 * Creates a 3D matrix from a 2D matrix. do not effect Z-axis.
 */
fun Mat.to3D(mat4: Mat3D): Mat3D {
    val m = mat4.`val`
    val m3 = this.`val`

    m[Mat3D.M00] = m3[Mat.M00]
    m[Mat3D.M01] = m3[Mat.M01]
    m[Mat3D.M03] = m3[Mat.M02]

    m[Mat3D.M10] = m3[Mat.M10]
    m[Mat3D.M11] = m3[Mat.M11]
    m[Mat3D.M13] = m3[Mat.M12]

    m[Mat3D.M22] = 1f
    m[Mat3D.M33] = 1f
    return mat4
}

fun Mat.to3D(): Mat3D = to3D(Mat3D())
