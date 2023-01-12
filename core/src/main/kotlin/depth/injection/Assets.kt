package depth.injection

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import depth.voxel.DeepGameSettings
import eater.injection.InjectionContext.Companion.inject
import ktx.assets.DisposableContainer
import ktx.assets.DisposableRegistry
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import net.mgsx.gltf.loaders.gltf.GLTFLoader

fun assets(): Assets {
    return inject()
}

class Assets(private val gameSettings: DeepGameSettings) : DisposableRegistry by DisposableContainer() {
    //    val buddy: Map<AnimDef, Animation<TextureRegion>> by lazy {
//        val texture = Texture("player/buddy.png".toInternalFile()).alsoRegister()
//        AnimDef.animDefs.associateWith { ad ->
//            Animation(0.1f, *Array(ad.numberOfFrames) { x ->
//                TextureRegion(texture, x * 32, ad.rowIndex * 32, 32, 32)
//            })
//        }
//    }
    init {

//        Scene2DSkin.defaultSkin = Skin("ui/uiskin.json".toInternalFile())
    }

    val coralTexture = Texture("coral.png".toInternalFile())
    val submarine by lazy { GLTFLoader().load(Gdx.files.internal("models/submarine.gltf")) }

    val diffuseTexture = Texture("textures/red_bricks_04_diff_1k.jpg".toInternalFile(), true)
    val normalTexture = Texture("textures/red_bricks_04_nor_gl_1k.jpg".toInternalFile(), true)
    val mrTexture = Texture("textures/red_bricks_04_rough_1k.jpg".toInternalFile(), true)

    override fun dispose() {
        registeredDisposables.disposeSafely()
    }
}
