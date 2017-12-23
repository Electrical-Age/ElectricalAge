package mods.eln.misc

data class Vec3f(var x: Float, var y: Float, var z: Float) {
    companion object {
        val ZERO = Vec3f(0.0f, 0.0f, 0.0f)
        val ONE = Vec3f(1.0f, 1.0f, 1.0f)
        val AXIS_X = Vec3f(1.0f, 0.0f, 0.0f)
        val AXIS_Y = Vec3f(0.0f, 1.0f, 0.0f)
        val AXIS_Z = Vec3f(0.0f, 0.0f, 1.0f)
        val AXES = arrayOf(AXIS_X, AXIS_Y, AXIS_Z)

        fun fromVolumeCenter(c: Coordonate) = Vec3f(c.x + 0.5f, c.y + 0.5f, c.z + 0.5f)
    }

    constructor(c: Coordonate) : this(c.x.toFloat(), c.y.toFloat(), c.z.toFloat())
    fun toCoordonate(dim: Int = 0) = Coordonate(x.toInt(), y.toInt(), z.toInt(), dim)

    fun dot(other: Vec3f) = x * other.x + y * other.y + z * other.z

    fun negate(): Vec3f = Vec3f(-x, -y, -z)
    operator fun unaryMinus() = negate()
    fun reciprocal(): Vec3f = Vec3f(1/x, 1/y, 1/z)
    fun add(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)
    fun add(other: Float) = Vec3f(x + other, y + other, z + other)
    operator fun plus(other: Vec3f) = add(other)
    operator fun plus(other: Float) = add(other)
    fun sub(other: Vec3f) = add(other.negate())
    fun sub(other: Float) = add(-other)
    operator fun minus(other: Vec3f) = sub(other)
    operator fun minus(other: Float) = sub(other)
    fun mul(other: Vec3f) = Vec3f(x * other.x, y * other.y, z * other.z)
    fun mul(other: Float) = Vec3f(x * other, y * other, z * other)
    operator fun times(other: Vec3f) = mul(other)
    operator fun times(other: Float) = mul(other)
    operator fun div(other: Vec3f) = mul(other.reciprocal())
    operator fun div(other: Float) = mul(1/other)

    fun magSquared() = dot(this)
    fun mag() = Math.sqrt(magSquared().toDouble()).toFloat()
    fun isZero() = magSquared() == 0.0f

    fun distanceTo(other: Vec3f) = sub(other).mag()
    fun normalized() = if(isZero()) Vec3f(0.0f, 0.0f, 0.0f) else div(mag())
}

data class Line3f(var origin: Vec3f, var vector: Vec3f) {
    companion object {
        fun fromEndpoints(a: Vec3f, b: Vec3f) = Line3f(a, b.sub(a))
    }

    fun pointAt(u: Float) = origin.add(vector.mul(u))
    fun midpoint() = pointAt(0.5f)
    fun opposite() = pointAt(1.0f)
    fun length() = vector.mag()

    fun allIntersectionsAlongU(): List<Float> {
        var isctus = ArrayList<Float>()

        for(axis in Vec3f.AXES) {
            val kor = Math.floor(origin.dot(axis).toDouble()).toLong()
            val kop = Math.floor(opposite().dot(axis).toDouble()).toLong()

            for(k in (Math.min(kor, kop) - 1) .. (Math.max(kor, kop) + 1)) {
                val u = Plane3f(axis, axis.mul(k.toFloat())).isctLineU(this)
                if(u === null || u < 0.0 || u >= 1.0) continue
                isctus.add(u)
            }
        }

        isctus.sort()
        return isctus
    }
    fun allIntersectionsAlong() = allIntersectionsAlongU().map {u -> pointAt(u)}

    fun allSegmentsAlong(): List<Line3f> {
        var segs = ArrayList<Line3f>()

        val initSeq = ArrayList<Vec3f>()
        initSeq.add(origin)
        val iscts = allIntersectionsAlong()

        for((a, b) in (initSeq.asSequence() + iscts.asSequence()).zip(iscts.asSequence() + opposite())) {
            segs.add(Line3f(a, b.sub(a)))
        }

        return segs
    }
    fun allMidpointsAlong() = allSegmentsAlong().map {seg -> seg.midpoint()}
    fun allCoordonatesAlong(dim: Int = 0) = allMidpointsAlong().map {pt -> pt.toCoordonate(dim)}
}

data class Plane3f(var normal: Vec3f, var offset: Float) {
    init {
        normal = normal.normalized()
    }

    companion object {
        val PLANE_X = Plane3f(Vec3f.AXIS_X, 0.0f)
        val PLANE_Y = Plane3f(Vec3f.AXIS_Y, 0.0f)
        val PLANE_Z = Plane3f(Vec3f.AXIS_Z, 0.0f)
        val AXIS_PLANES = arrayOf(PLANE_X, PLANE_Y, PLANE_Z)
    }

    constructor(normal: Vec3f, point: Vec3f) : this(normal, -normal.dot(point))

    fun pointOn() = normal.mul(-offset)

    fun isctLineU(ln: Line3f): Float? {
        val d = ln.vector.dot(normal)
        if(d == 0.0f) return null

        val p0 = pointOn()
        return p0.sub(ln.origin).dot(normal) / d
    }
    fun isctLine(ln: Line3f): Vec3f? {
        val u = isctLineU(ln)
        if(u === null) return null
        return ln.pointAt(u)
    }
    fun isctSegment(seg: Line3f): Vec3f? {
        val u = isctLineU(seg)
        if(u === null || u < 0.0 || u > 1.0) return null
        return seg.pointAt(u)
    }
}
