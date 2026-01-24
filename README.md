<h1 align="center">
  <br>
  <img src="docs/icon-512.png" alt="Oraxen Logo" width="128">
  <br>
  Oraxen
  <br>
</h1>

<h4 align="center">Source code of the Oraxen Spigot plugin, made with love in Java.</h4>

<p align="center">
  <a href="https://www.codefactor.io/repository/github/oraxen/oraxen">
    <img src="https://www.codefactor.io/repository/github/oraxen/oraxen/badge" alt="CodeFactor Score"/>
  </a>
  <a href="https://repo.oraxen.com/#/releases/io/th0rgal/oraxen">
    <img src="https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/releases/io/th0rgal/oraxen/maven-metadata.xml" alt="Version"/>
  </a>
  <a href="https://www.spigotmc.org/resources/oraxen.72448/">
    <img src="https://img.shields.io/badge/spigot-oraxen-brightgreen" alt="Spigot"/>
  </a>
  <a href="https://bstats.org/plugin/bukkit/Oraxen">
    <img src="https://img.shields.io/bstats/servers/5371?color=brightgreen" alt="bStats Servers"/>
  </a>
  <a href="https://bstats.org/plugin/bukkit/Oraxen">
    <img src="https://img.shields.io/bstats/players/5371?color=brightgreen" alt="bStats Players"/>
  </a>
</p>

<p align="center">
  <a href="#what-is-it">What is it?</a> •
  <a href="#features">Features</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#api">API</a> •
  <a href="#license">License</a>
</p>

---

## What is it?

Oraxen is a Minecraft plugin that allows you to modify the game by adding new items, weapons, blocks, and more. One of its key features is the ability to generate the texture pack automatically from the configuration, which greatly simplifies the work of administrators. It also includes an extensive API which can be used by developers to expand Oraxen's capabilities.

## Features

- Automatically generate the resource-pack
- Automatically upload the resource-pack
- Automatically send the resource-pack to your players
- Create new items in a few lines of configuration
- Support for custom items, weapons, armors, blocks, and more
- Modular mechanics system to empower your items
- Automatic configuration updates when you update the plugin
- Handle configuration errors gracefully

## Contributing

If you want to contribute to Oraxen, you can do so by creating a pull request. You should make a pull-request to the `develop` branch.

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

| Class | Description |
|-------|-------------|
| `OraxenItems` | Methods related to Oraxen items |
| `OraxenBlocks` | Methods related to custom blocks in Oraxen |
| `OraxenFurniture` | Methods related to custom furniture in Oraxen |
| `OraxenPack` | Methods related to the resource-pack |

### Repository

<details>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
maven("https://repo.oraxen.com/releases")
```
</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
maven {
    url "https://repo.oraxen.com/releases"
}
```
</details>

<details>
<summary><b>Maven</b></summary>

```xml
<repository>
  <id>oraxen</id>
  <name>Oraxen Repository</name>
  <url>https://repo.oraxen.com/releases</url>
</repository>
```
</details>

### Dependency

[![version](https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.oraxen.com/releases/io/th0rgal/oraxen/maven-metadata.xml)](https://repo.oraxen.com/#/releases/io/th0rgal/oraxen)

The latest version can be found at the badge above.

<details>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
compileOnly("io.th0rgal:oraxen:VERSION")
```
</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
compileOnly 'io.th0rgal:oraxen:VERSION'
```
</details>

<details>
<summary><b>Maven (with exclusions)</b></summary>

```xml
<dependency>
    <groupId>io.th0rgal</groupId>
    <artifactId>oraxen</artifactId>
    <version>1.167.0</version>
    <exclusions>
        <exclusion>
            <groupId>me.gabytm.util</groupId>
            <artifactId>actions-spigot</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.jetbrains</groupId>
            <artifactId>annotations</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.ticxo</groupId>
            <artifactId>PlayerAnimator</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.github.stefvanschie.inventoryframework</groupId>
            <artifactId>IF</artifactId>
        </exclusion>
        <exclusion>
            <groupId>io.th0rgal</groupId>
            <artifactId>protectionlib</artifactId>
        </exclusion>
        <exclusion>
            <groupId>dev.triumphteam</groupId>
            <artifactId>triumph-gui</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.bstats</groupId>
            <artifactId>bstats-bukkit</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.jeff-media</groupId>
            <artifactId>custom-block-data</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.jeff-media</groupId>
            <artifactId>persistent-data-serializer</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.jeff_media</groupId>
            <artifactId>MorePersistentDataTypes</artifactId>
        </exclusion>
        <exclusion>
            <groupId>gs.mclo</groupId>
            <artifactId>java</artifactId>
        </exclusion>
    </exclusions>
    <scope>provided</scope>
</dependency>
```
</details>

Snapshot builds are also available at [https://repo.oraxen.com/snapshots](https://repo.oraxen.com/snapshots).

## License

*Click here to read [the entire license](https://github.com/Th0rgal/Oraxen/blob/master/LICENSE.md).*

Oraxen is a paid plugin. To use it you must purchase a license on [spigotmc.org](https://spigotmc.org), [polymart.org](https://polymart.org/product/629/), or [builtbybit.com](https://builtbybit.com/resources/oraxen-custom-items-blocks-more.16594/). Nevertheless we will not try to prevent you from downloading the source code and rebuilding it, as long as you do not distribute it (whether it is modified or intact and compiled or whether it is the source code, partial or complete). Public forks are allowed as long as you comply with the license (in order to propose a pull request). Buying a license will not only save you time, I will do my best to help you if you have any concerns and it will show me that you appreciate my work.
