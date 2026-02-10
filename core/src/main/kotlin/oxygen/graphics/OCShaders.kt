package oxygen.graphics

object OCShaders {
    val darkness: OShader by lazy { OShader("batch/zbatchSimple", "renderer/darkness").setup() }
    fun init() {
        darkness
    }
}
