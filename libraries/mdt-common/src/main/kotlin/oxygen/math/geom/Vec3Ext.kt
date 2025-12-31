package oxygen.math.geom

import arc.math.geom.*

object Vec3ExtObj {
    val tmpMat = Mat3D()
}

/** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
 * @param matrix The matrix
 * @return This vector for chaining */
fun Vec3.mul(matrix: Mat3D): Vec3 {
    val lMat = matrix.`val`
    return this.set(
        x * lMat[Mat3D.M00] + y * lMat[Mat3D.M01] + z * lMat[Mat3D.M02] + lMat[Mat3D.M03],
        x * lMat[Mat3D.M10] + y * lMat[Mat3D.M11] + z * lMat[Mat3D.M12] + lMat[Mat3D.M13],
        x * lMat[Mat3D.M20] + y * lMat[Mat3D.M21] + z * lMat[Mat3D.M22] + lMat[Mat3D.M23]
    )
}

/** Multiplies this vector by the first three columns of the matrix, essentially only applying rotation and scaling.
 *
 * @param matrix The matrix
 * @return This vector for chaining */
fun Vec3.rot(matrix: Mat3D): Vec3 {
    val lMat = matrix.`val`
    return this.set(
        x * lMat[Mat3D.M00] + y * lMat[Mat3D.M01] + z * lMat[Mat3D.M02],
        x * lMat[Mat3D.M10] + y * lMat[Mat3D.M11] + z * lMat[Mat3D.M12],
        x * lMat[Mat3D.M20] + y * lMat[Mat3D.M21] + z * lMat[Mat3D.M22]
    )
}

/** Rotates this vector by the given angle in degrees around the given axis.
 *
 * @param degrees the angle in degrees
 * @param axisX the x-component of the axis
 * @param axisY the y-component of the axis
 * @param axisZ the z-component of the axis
 * @return This vector for chaining */
fun Vec3.rotate(degrees: Float, axisX: Float, axisY: Float, axisZ: Float) =
    this.mul(Vec3ExtObj.tmpMat.setToRotation(axisX, axisY, axisZ, degrees))

/** Rotates this vector by the given angle in radians around the given axis.
 *
 * @param radians the angle in radians
 * @param axisX the x-component of the axis
 * @param axisY the y-component of the axis
 * @param axisZ the z-component of the axis
 * @return This vector for chaining */
fun Vec3.rotateRad(radians: Float, axisX: Float, axisY: Float, axisZ: Float) =
    this.mul(Vec3ExtObj.tmpMat.setToRotationRad(axisX, axisY, axisZ, radians))
