<h1 align="center">
  <br>
  <img src="https://oraxen.com/logo.svg" alt="oraxen.com logo" width="256">
  <br>
</h1>

<h4 align="center">☄️ Source code of the Oraxen spigot plugin, made with love in Java.</h4>
<p align="center">
    <a href="https://www.codefactor.io/repository/github/oraxen/oraxen" alt="CodeFactor Score">
        <img src="https://www.codefactor.io/repository/github/oraxen/oraxen/badge"/>
    </a>
    <a href="https://repo.oraxen.com/#/releases/io/th0rgal/oraxen" alt="version">
        <img src="https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/releases/io/th0rgal/oraxen/maven-metadata.xml"/>
    </a>
    <a href="https://repo.oraxen.com/#/snapshots/io/th0rgal/oraxen" alt="version">
        <img src="https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/snapshots/io/th0rgal/oraxen/maven-metadata.xml"/>
    </a>
    <a href="https://www.spigotmc.org/resources/oraxen.72448/">
        <img alt="spigot" src="https://img.shields.io/badge/spigot-oraxen-brightgreen"/>
    </a>
    <a href="https://bstats.org/plugin/bukkit/Oraxen" alt="bstats servers">
        <img src="https://img.shields.io/bstats/servers/5371?color=brightgreen"/>
    </a>
    <a href="https://bstats.org/plugin/bukkit/Oraxen" alt="bstats players">
        <img src="https://img.shields.io/bstats/players/5371?color=brightgreen"/>
    </a>
</p>

## What is it?

Oraxen is a minecraft plugin that allows you to modify the game by adding new items, weapons, blocks, and more. One of
its key features is to be able to generate the texture pack automatically from the configuration, which greatly
simplifies the work of administrators. It also includes an extensive API which can be used by developers to be able to
increase oraxen features.

## Features

- Automatically generate the resource-pack
- Automatically upload the resource-pack
- Automatically send the resource-pack to your players
- Allow to create new items in a few lines of configuration
- Support custom items, weapons, armors, blocks, and more
- Modular mechanics system to empower your items
- Automatically update configurations when you update the plugin
- Handle configuration errors

## Contributing
If you want to contribute to Oraxen, you can do so by creating a pull request.\
You should make a pull-request to the `develop` branch.\
1. Fork Oraxen on GitHub
2. Clone your forked repository (`git clone`)
3. Create your feature branch (`git checkout -b my-feature`)
4. Commit your changes (`git commit -am 'Add my feature'`)
5. Push to the branch (`git push origin my-feature`)
6. Create a new Pull Request to the `develop` branch
7. Wait for your pull request to be reviewed and merged
8. Celebrate your contribution!

## API

Oraxen's API is primarily found in these four classes:
- OraxenItems - methods related to Oraxen items
- OraxenBlocks - methods related to custom blocks in Oraxen
- OraxenFurniture - methods related to custom furniture in Oraxen
- OraxenPack - methods related to the resource-pack

### Repository
**Gradle Kts**:
```kts
maven("https://repo.oraxen.com/releases")
```
**Groovy**:
```groovy
maven {
    url "https://repo.oraxen.com/releases"
}
```
**Maven**
```html
<repository>
  <id>oraxen</id>
  <name>Oraxen Repository</name>
  <url>https://repo.oraxen.com/releases</url>
</repository>
```
### Dependency [![version](https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/releases/io/th0rgal/oraxen/maven-metadata.xml)](https://repo.oraxen.com/#/releases/io/th0rgal/oraxen) [![version](https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/snapshots/io/th0rgal/oraxen/maven-metadata.xml)](https://repo.oraxen.com/#/snapshots/io/th0rgal/oraxen)
The latest version can be found at above.\ 
```kts
compileOnly("io.th0rgal:oraxen:VERSION")
```
```groovy
compileOnly 'io.th0rgal:oraxen:VERSION'
```
```html
<dependency>
  <groupId>io.th0rgal</groupId>
  <artifactId>oraxen</artifactId>
  <version>VERSION</version>
  <scope>provided</scope>
```
Snapshot builds are also available at [https://repo.oraxen.com/snapshots](https://repo.oraxen.com/snapshots). \

## License

*Click here to read [the entire license](https://github.com/Th0rgal/Oraxen/blob/master/LICENSE.md).*

Oraxen is a paid plugin, to use it you must purchase a license on [spigotmc.org](https://spigotmc.org), nevertheless I
will not try to prevent you from downloading the source code and rebuilding it, as long as you do not distribute it (
whether it is modified or intact and compiled or whether it is the source code, partial or complete). Public forks are
allowed as long as you comply with the license (in order to propose a pull request). Buying a license will not only save
you time, I will do my best to help you if you have any concerns and it will show me that you appreciate my work.
