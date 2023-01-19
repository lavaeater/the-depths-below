package depth.ecs.systems

import java.awt.Point

data class PointCoord(val x: Int, val y: Int, val z: Int) {
    fun getCoordForVertex(vertexIndex: Int): PointCoord {
        val offset = vertexIndexToPointCoordinate[vertexIndex]!!
        return add(offset)
    }

    fun add(coord: PointCoord): PointCoord {
        return PointCoord(
            this.x + coord.x,
            this.y + coord.y,
            this.z + coord.z
        )
    }

    fun add(x: Int, y: Int, z: Int): PointCoord {
        return PointCoord(
            this.x + x,
            this.y + y,
            this.z + z
        )
    }

    companion object {
        //TODO: Redo this according to paper
        //MAYBE My tables are wrong?
        val vertexIndexToPointCoordinate = mapOf(
            0 to PointCoord(0, 0, 0),
            1 to PointCoord(1, 0, 0),
            2 to PointCoord(1, 1,0),
            3 to PointCoord(0, 1,0),
            4 to PointCoord(0, 0,1),
            5 to PointCoord(1, 0,1),
            6 to PointCoord(1, 1, 1),
            7 to PointCoord(0, 1, 1)
        )

    }
}
