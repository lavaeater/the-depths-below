package depth.marching

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

    fun coordForIndex(index: Int):PointCoord {
        return this.add(oldVertexIndexToPointCoordinate[index]!!)
    }

    companion object {
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

        val oldVertexIndexToPointCoordinate = mapOf(
            0 to PointCoord(0,0,0),
            1 to PointCoord(0,0,1),
            2 to PointCoord(1,0,1),
            3 to PointCoord(1,0,0),
            4 to PointCoord(0,1,0),
            5 to PointCoord(0,1,1),
            6 to PointCoord(1,1,1),
            7 to PointCoord(1,1,0)
        )

        fun getVertIndexProper(x: Int, y: Int, z: Int): Int {
            var index = 0
            if (x == 0 && y == 0 && z == 0) {
                index = 0
            } else if (x == 1 && y == 0 && z == 0) {
                index = 3
            } else if (x == 1 && y == 1 && z == 0) {
                index = 7
            } else if (x == 1 && y == 1 && z == 1) {
                index = 6
            } else if (x == 1 && y == 0 && z == 1) {
                index = 2
            } else if (x == 0 && y == 1 && z == 1) {
                index = 5
            } else if (x == 0 && y == 0 && z == 1) {
                index = 1
            } else if (x == 0 && y == 1 && z == 0) {
                index = 4
            }
            return index
        }

    }
}
