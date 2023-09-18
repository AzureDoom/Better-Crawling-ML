# Better-Crawling-ML
A stripped-down Nyfs Spiders for other mobs!

```gradle
repositories {
    // The Maven with the mods source
    maven {url 'https://libs.azuredoom.com:4443/mods'}
}

dependencies {
    //Fabric or Quilt
    modImplementation "mod.azuredoom.bettercrawling:1.0.1:better-crawling-fabric-1.20.1"
		
    //NeoForge or Forge
    implementation fg.deobf("mod.azuredoom.bettercrawling:1.0.1:better-crawling-neoforge-1.20.1")
}
```
