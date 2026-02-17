package oxygen.world

import mindustry.*
import arc.math.geom.*
import arc.util.noise.*
import arc.math.*
import arc.util.*
import mindustry.world.*
import kotlin.math.*

import mindustry.Vars.*

fun Tiles.solid(x:Int, y:Int) = get(x, y)?.solid() ?: true

fun Tiles.wallSolid(x:Int, y:Int)= get(x, y)?.block()?.solid ?: true

fun Tiles.isAccessible(x:Int, y:Int) = !wallSolid(x, y - 1) || !wallSolid(x, y + 1) || !wallSolid(x - 1, y) || !wallSolid(x + 1, y);

fun Tiles.unitWidth():Int = width * Vars.tilesize
fun Tiles.unitHeight():Int = height * Vars.tilesize

fun Tiles.getDarkness(x: Int,y: Int):Float{
    var dark = 0f

    if(Vars.state.rules.borderDarkness){
        var edgeBlend = 2
        var edgeDst = 0

        if(!state.rules.limitMapArea){
            edgeDst = min(x, min(y, min(-(x - (width - 1)), -(y - (height - 1)))))
        }else{
            edgeDst =
                min(x - state.rules.limitX,
                min(y - state.rules.limitY,
                min(-(x - (state.rules.limitX + state.rules.limitWidth - 1)), -(y - (state.rules.limitY + state.rules.limitHeight - 1)))))
        }

        if(edgeDst <= edgeBlend){
            dark = max((edgeBlend - edgeDst) * (4f / edgeBlend), dark)
        }
    }

    if(state.hasSector() && state.getSector().preset == null){
        var circleBlend = 5
        //quantized angle
        val offset = state.getSector().rect.rotation + 90f
        val angle = Angles.angle(x.toFloat(), y.toFloat(), width.toFloat()/2f, height.toFloat()/2f) + offset
        //polygon sides, depends on sector
        val sides = state.getSector().tile.corners.size
        val step = 360f / sides.toFloat()
        //prev and next angles of poly
        val prev = Mathf.round(angle,step) 
        val next = prev + step
        //raw line length to be translated
        val length = state.getSector().getSize()/2f
        var rawDst = Intersector.distanceLinePoint(Tmp.v1.trns(prev, length), Tmp.v2.trns(next, length), Tmp.v3.set(x - width/2f, y - height/2f).rotate(offset)) / Mathf.sqrt3 - 1f


        //noise
        rawDst += Noise.noise(x.toFloat(), y.toFloat(), 11f, 7f) + Noise.noise(x.toFloat(), y.toFloat(), 22f, 15f)

        val circleDst = (rawDst - (length - circleBlend))
        if(circleDst > 0){
            dark = max(circleDst, dark)
        }
    }

    val tile = get(x, y)
    if(tile != null && tile.isDarkened()){
        dark = max(dark, tile.data.toFloat())
    }

    return dark
}

fun Tiles.getWallDarkness(tile: Tile):Byte{
    if(tile.isDarkened()){
        var minDst = darkRadius + 1
        for(cx in tile.x - darkRadius..tile.x + darkRadius){
            for(cy in tile.y - darkRadius..tile.y + darkRadius){
                if(`in`(cx, cy) && !getn(cx, cy).isDarkened()){
                    minDst = Math.min(minDst, abs(cx - tile.x) + abs(cy - tile.y))
                }
            }
        }

        return max((minDst - 1), 0).toByte()
    }
    return 0
}
