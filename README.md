# <img src="/preview/logo.png" title="logo" height="80" width="80" /> GreenRobot EventBus Plugin
IntelliJ iDEA plugin to work with projects using greenrobot's <a href="http://greenrobot.org/eventbus/">EventBus</a> library

<img src="/preview/screenshare.gif" alt="preview" title="preview"/>

Install
-----
You can install the plugin from `Preferences` -> `Plugins` and search for the plugin. You can also download the plugin from the <a href="https://plugins.jetbrains.com/plugin/12856-greenrobot-eventbus">intelliJ iDEA Marketplace</a>.

Features
-----
- Supports `post`, `postSticky` and `@Subscribe`
- Jump from post to subscribe and vice versa
- Shows marker only for `@Subscribe` methods that have correct signatures
- Optionally show usages in 'Find' tool window
- Add breakpoints to all usages in a single click
- Fully supported for project using both Java and Kotlin

Testing
-----
There are no unit tests yet (I am writing them, but hit a roadblock. We will soon find a way). Any changes made to the plugin should be tested against <a href="https://github.com/thsaravana/eventbus-playground">this project</a>. This project contains all possible use cases of EventBus in both Java and Kotlin.
