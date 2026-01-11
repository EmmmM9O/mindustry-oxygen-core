package oxygen.world.blocks

import arc.math.geom.*
import mindustry.world.*

interface G3DrawBuilding {
    fun draw3DHitbox(tmp: Rect, tile: Tile) {
        val block = tile.block()
        tmp.setCentered(
            tile.worldx() + block.offset,
            tile.worldy() + block.offset,
            block.clipSize,
            block.clipSize
        )
    }

    fun drawDepth()
    fun draw3D()
}
